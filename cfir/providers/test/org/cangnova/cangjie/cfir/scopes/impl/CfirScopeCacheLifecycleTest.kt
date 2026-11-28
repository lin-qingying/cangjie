@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.expandedConeTypeWithEnsuredPhase
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirMemberScopeOwner
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/** 库声明作为既有完成输入；通过真实 scope 查询观察共享、泛型替换与新代有效成员。 */
class CfirScopeCacheLifecycleTest {
    @Test
    fun diamondSharesCompletedAncestorAndRepeatedRawQuery() {
        val fixture = Fixture()
        val root = fixture.interfaceDeclaration("Root", members = listOf(fixture.function("Root", "value", ConePrimitiveType.INT64)))
        val left = fixture.interfaceDeclaration("Left", parents = listOf(fixture.type(root)))
        val right = fixture.interfaceDeclaration("Right", parents = listOf(fixture.type(root)))
        val core = fixture.coreModel()
        val leaf = fixture.classDeclaration("Leaf", parents = listOf(core.objectRef(), fixture.ref(fixture.type(left)), fixture.ref(fixture.type(right))))
        fixture.install(core.any, core.objectDeclaration, root, left, right, leaf)
        val scopeSession = ScopeSession()

        val raw = fixture.scope(leaf, scopeSession)
        val parentLeft = raw.parentScopeInputs[1].second as CfirClassUseSiteMemberScope
        val parentRight = raw.parentScopeInputs[2].second as CfirClassUseSiteMemberScope
        assertSame(parentLeft.parentScopeInputs.single().second, parentRight.parentScopeInputs.single().second)
        assertEquals(setOf(Name.identifier("value")), raw.getCallableNames())
        assertSame(raw, fixture.scope(leaf, scopeSession))
        assertEquals(listOf(fixture.type(core.objectDeclaration), fixture.type(left), fixture.type(right)), raw.parentScopeInputs.map { it.first.type })
        assertSame(leaf.superTypeRefs.first(), (raw.parentScopeInputs.first().first.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)

        val beforeGeneration = raw.cacheGeneration
        assertSame(raw, fixture.scope(leaf, scopeSession, requiredPhase = CfirResolvePhase.BODY_RESOLVE))
        assertEquals(beforeGeneration, raw.cacheGeneration)
    }

    @Test
    fun genericInstancesShareDeclarationsAndKeepDifferentSubstitutionProducts() {
        val fixture = Fixture()
        val symbol = CfirInterfaceSymbol(fixture.classId("I"))
        val parameter = fixture.parameter(symbol)
        val templateType = parameter.symbol.constructType()
        val function = fixture.function("I", "choose", templateType)
        val owner = fixture.interfaceDeclaration("I", parameters = listOf(parameter), members = listOf(function), symbol = symbol)
        fixture.install(owner)
        assertSame(owner.symbol, fixture.session.cfirProvider.getContainingClass(function.symbol))
        val scopeSession = ScopeSession()
        val intType = ConeClassLikeType(symbol.toLookupTag(), listOf(ConePrimitiveType.INT64), isInterface = true)
        val boolType = ConeClassLikeType(symbol.toLookupTag(), listOf(ConePrimitiveType.BOOLEAN), isInterface = true)
        val rawInt = fixture.scope(owner, scopeSession, type = intType)
        val rawBool = fixture.scope(owner, scopeSession, type = boolType)
        assertNotSame(rawInt, rawBool)
        assertSame(rawInt.declarationMembers, rawBool.declarationMembers)
        val intScope = fixture.scopes.getClassSubstitutionScope(fixture.session, scopeSession, { fixture.scope(owner, scopeSession, type = intType) }, intType).value
        val boolScope = fixture.scopes.getClassSubstitutionScope(fixture.session, scopeSession, { fixture.scope(owner, scopeSession, type = boolType) }, boolType).value
        assertNotSame(intScope, boolScope)
        val intFunction = mutableListOf<CfirNamedFunctionSymbol>().also {
            intScope.processFunctionsByName(function.name, it::add)
        }.single()
        val boolFunction = mutableListOf<CfirNamedFunctionSymbol>().also {
            boolScope.processFunctionsByName(function.name, it::add)
        }.single()
        assertEquals(ConePrimitiveType.INT64, intFunction.cfir.returnTypeRef.coneTypeOrNull)
        assertEquals(ConePrimitiveType.BOOLEAN, boolFunction.cfir.returnTypeRef.coneTypeOrNull)
        assertSame(intScope, fixture.scopes.getClassSubstitutionScope(fixture.session, scopeSession, { fixture.scope(owner, scopeSession, type = intType) }, intType).value)
        assertSame(boolScope, fixture.scopes.getClassSubstitutionScope(fixture.session, scopeSession, { fixture.scope(owner, scopeSession, type = boolType) }, boolType).value)
    }

    @Test
    fun realGraphAndMemberInputChangesRetireAllCurrentViewsWithoutClearingPeerStorage() {
        val fixture = Fixture()
        val first = fixture.interfaceDeclaration("First", members = listOf(fixture.function("First", "first", ConePrimitiveType.INT64)))
        val second = fixture.interfaceDeclaration("Second", members = listOf(fixture.function("Second", "second", ConePrimitiveType.INT64)))
        val core = fixture.coreModel()
        val objectRef = core.objectRef()
        val firstRef = fixture.ref(fixture.type(first))
        val secondRef = fixture.ref(fixture.type(second))
        val target = fixture.classDeclaration("Target", listOf(objectRef, firstRef))
        fixture.install(core.any, core.objectDeclaration, first, second, target)
        val graph = MutableGraph()
        fixture.publish(graph, core.any, core.objectDeclaration, first, second, target)
        fixture.session.register(CfirDirectSupertypeProvider::class, graph)
        val scopeSession = ScopeSession()
        val peerFixture = Fixture()
        val peerCore = peerFixture.coreModel()
        val peerTarget = peerFixture.classDeclaration("Independent", listOf(peerCore.objectRef()))
        peerFixture.install(peerCore.any, peerCore.objectDeclaration, peerTarget)
        val old = fixture.scope(target, scopeSession, graph = graph)
        val peer = peerFixture.scope(peerTarget, scopeSession)
        val oldStatic = target.symbol.staticScopeForQualifierType(fixture.session, scopeSession, memberRequiredPhase = null)
        assertEquals(setOf(Name.identifier("first")), old.getCallableNames())
        assertEquals(setOf(Name.identifier("first")), oldStatic.getCallableNames())

        target.superTypeRefs.clear()
        target.superTypeRefs.addAll(listOf(objectRef, secondRef))
        fixture.publish(graph, target)
        val current = fixture.scope(target, scopeSession, graph = graph)
        val currentStatic = target.symbol.staticScopeForQualifierType(fixture.session, scopeSession, memberRequiredPhase = null)
        assertNotSame(old, current)
        assertNotSame(oldStatic, currentStatic)
        assertEquals(setOf(Name.identifier("second")), current.getCallableNames())
        assertEquals(setOf(Name.identifier("second")), currentStatic.getCallableNames())
        assertSame(peer, peerFixture.scope(peerTarget, scopeSession))
        assertSame(old.cacheOwner, current.cacheOwner)
        assertTrue(current.cacheGeneration > old.cacheGeneration)

        (target.declarations as MutableList<CfirDeclaration>).add(fixture.function("Target", "later", ConePrimitiveType.INT64))
        val next = fixture.scope(target, scopeSession, graph = graph)
        assertNotSame(current, next)
        assertEquals(setOf(Name.identifier("second"), Name.identifier("later")), next.getCallableNames())
        assertSame(next, fixture.scope(target, scopeSession, graph = graph))
    }

    @Test
    fun parentPreparationCannotPublishDifferentGenerationsOfTheSameProducer() {
        val fixture = Fixture()
        val first = fixture.interfaceDeclaration("First")
        val second = fixture.interfaceDeclaration("Second", members = listOf(fixture.function("Second", "before", ConePrimitiveType.INT64)))
        val core = fixture.coreModel()
        val leaf = fixture.classDeclaration("Leaf", parents = listOf(core.objectRef(), fixture.ref(fixture.type(first)), fixture.ref(fixture.type(second))))
        fixture.install(core.any, core.objectDeclaration, first, second, leaf)
        val storage = ScopeSession()
        val graph = MutableGraph()
        fixture.publish(graph, core.any, core.objectDeclaration, first, second, leaf)
        fixture.session.register(CfirDirectSupertypeProvider::class, graph)
        fixture.scope(second, storage, graph = graph)
        var replace = true
        graph.onRead = { symbol ->
            if (replace && symbol === second.symbol) {
                replace = false
                (second.declarations as MutableList<CfirDeclaration>).add(fixture.function("Second", "after", ConePrimitiveType.INT64))
            }
        }
        val producer = CfirCangJieScopeProvider()
        val raw = producer.getUseSiteMemberScope(
            leaf.symbol, fixture.session, storage, scopeKind = CfirClassMemberScopeKind.USE_SITE,
            directSupertypeProvider = graph, memberRequiredPhase = null,
        )
        val parents = raw.parentScopeInputs.filter { it.first.type.classId in setOf(first.symbol.classId, second.symbol.classId) }
            .map { it.second as CfirClassUseSiteMemberScope }
        assertFalse(replace, "必须在第二个父输入准备中实际替换声明成员")
        assertSame(parents[0].cacheOwner, parents[1].cacheOwner)
        assertEquals(parents[0].cacheGeneration, parents[1].cacheGeneration)
        assertEquals(parents[0].cacheOwner.generation, parents[0].cacheGeneration)
        assertTrue(raw.cacheDependencies.all { it.isCurrent() })
        assertTrue(raw.getCallableNames().contains(Name.identifier("after")))
        assertSame(raw, producer.getUseSiteMemberScope(
            leaf.symbol, fixture.session, storage, scopeKind = CfirClassMemberScopeKind.USE_SITE,
            directSupertypeProvider = graph, memberRequiredPhase = null,
        ))
    }

    /** general request 必须绑定原 symbol；同名 fixed 父节点不能被 ClassId 路径误剪。 */
    @Test
    fun sameNameGeneralRootsKeepFixedParentsAndAllNominalFlags() {
        val fixture = Fixture()
        val core = fixture.coreModel()
        val sharedId = fixture.classId("Shared")
        val firstSymbol = CfirClassSymbol(sharedId, useFixedSymbolLookupTag = true)
        val secondSymbol = CfirClassSymbol(sharedId, useFixedSymbolLookupTag = true)
        val firstParameter = fixture.parameter(firstSymbol)
        val secondParameter = fixture.parameter(secondSymbol)
        val bSymbol = CfirInterfaceSymbol(fixture.classId("B"), useFixedSymbolLookupTag = true)
        val bParameter = fixture.parameter(bSymbol)
        val b = fixture.interfaceDeclaration(
            "B", parameters = listOf(bParameter), symbol = bSymbol,
            members = listOf(fixture.function("B", "fromB", ConePrimitiveType.INT64)),
        )
        val secondTemplate = ConeClassLikeType(secondSymbol.toLookupTag(), listOf(firstParameter.symbol.constructType()))
        val secondRef = fixture.ref(secondTemplate)
        val bTemplate = ConeClassLikeType(bSymbol.toLookupTag(), listOf(secondParameter.symbol.constructType()), isInterface = true)
        val bRef = fixture.ref(bTemplate)
        val objectRef = core.objectRef()
        val first = fixture.classDeclaration("Shared", listOf(secondRef), firstSymbol, listOf(firstParameter))
        val second = fixture.classDeclaration("Shared", listOf(objectRef, bRef), secondSymbol, listOf(secondParameter))
        fixture.install(core.any, core.objectDeclaration, b, first, second, selectedSymbols = mapOf(sharedId to secondSymbol))
        val graph = MutableGraph()
        fixture.publish(graph, core.any, core.objectDeclaration, b, first, second)
        fixture.session.register(CfirDirectSupertypeProvider::class, graph)
        val secondInt = ConeClassLikeType(secondSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64))
        val bInt = ConeClassLikeType(bSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64), isInterface = true)
        fun declared(type: ConeCangJieType, ref: CfirResolvedTypeRef) = CfirInstantiatedSupertypeDescriptor(
            type, ref.coneType, CfirInstantiatedSupertypeOrigin.Declared(ref),
        )
        val prepared = mapOf<CfirClassLikeSymbol<*>, List<CfirInstantiatedSupertypeDescriptor>>(
            firstSymbol to listOf(declared(secondInt, secondRef)),
            secondSymbol to listOf(declared(fixture.type(core.objectDeclaration), objectRef), declared(bInt, bRef)),
            core.objectDeclaration.symbol to listOf(declared(fixture.type(core.any), core.anyRef)),
            core.any.symbol to emptyList(),
            bSymbol to emptyList(),
        )
        val observations = mutableListOf<ConeClassLikeType>()
        val typed = PreparedTypedInputs(
            CfirTypeAwareSupertypeSources(fixture.session, fixture.session.symbolProvider, fixture.session.extendProvider, graph),
            prepared, observations,
        )
        fixture.session.register(CfirTypeAwareSupertypeProvider::class, typed)
        val attributes = ConeAttributes.create(listOf(ScopeOwnerAttribute("root")))
        val general = ConeClassLikeType(
            sharedId.toLookupTag(), listOf(ConePrimitiveType.INT64), attributes,
            isInterface = false, isThisType = true, rawArrayDimensions = 1u,
        )
        val storage = ScopeSession()
        val firstScope = fixture.scope(first, storage, type = general, graph = graph)
        val secondScope = fixture.scope(second, storage, type = general, graph = graph)
        assertNotSame(firstScope, secondScope)
        val firstParent = firstScope.parentScopeInputs.single().first
        assertSame(secondRef, (firstParent.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertSame(secondSymbol, assertInstanceOf(ConeClassLikeLookupTagWithFixedSymbol::class.java,
            assertInstanceOf(ConeClassLikeType::class.java, firstParent.type).lookupTag).symbol)
        val secondParent = secondScope.parentScopeInputs.single { it.first.type == bInt }.first
        assertSame(bRef, (secondParent.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertSame(bSymbol, assertInstanceOf(ConeClassLikeLookupTagWithFixedSymbol::class.java,
            assertInstanceOf(ConeClassLikeType::class.java, secondParent.type).lookupTag).symbol)
        assertEquals(setOf(Name.identifier("fromB")), firstScope.getCallableNames())
        assertEquals(setOf(Name.identifier("fromB")), secondScope.getCallableNames())
        assertSame(firstScope, fixture.scope(first, storage, type = general, graph = graph))
        assertSame(secondScope, fixture.scope(second, storage, type = general, graph = graph))
        for (symbol in listOf(firstSymbol, secondSymbol)) {
            val observed = observations.first { it.isThisType && it.lookupTag is ConeClassLikeLookupTagWithFixedSymbol &&
                (it.lookupTag as ConeClassLikeLookupTagWithFixedSymbol).symbol === symbol }
            assertSame(symbol, assertInstanceOf(ConeClassLikeLookupTagWithFixedSymbol::class.java, observed.lookupTag).symbol)
            assertEquals(general.typeArguments, observed.typeArguments)
            assertSame(attributes, observed.attributes)
            assertEquals(general.isInterface, observed.isInterface)
            assertEquals(general.isThisType, observed.isThisType)
            assertEquals(general.rawArrayDimensions, observed.rawArrayDimensions)
        }
        val path = CfirSupertypePath.root(firstSymbol)
        assertTrue(path.contains(firstSymbol))
        assertFalse(path.contains(secondSymbol))
        val childPath = path.child(secondSymbol)
        assertTrue(childPath.contains(firstSymbol))
        assertTrue(childPath.contains(secondSymbol))
    }

    /** 仅提供已实例化的正式 typed 输入；root 绑定和 scope/path/cache 算法仍由生产完成。 */
    private class PreparedTypedInputs(
        private val sources: CfirTypeAwareSupertypeSources,
        private val products: Map<CfirClassLikeSymbol<*>, List<CfirInstantiatedSupertypeDescriptor>>,
        private val observations: MutableList<ConeClassLikeType>,
    ) : CfirTypeAwareSupertypeProvider {
        override val inputStamp: CfirTypeAwareSupertypeInputStamp
            get() = CfirTypeAwareSupertypeInputStamp(
                sources, CfirTypeAwareSupertypeInputRevision(sources.extends?.modificationCount, sources.directSupertypes?.modificationCount, 0L), emptySet(),
            )
        override fun forSources(sources: CfirTypeAwareSupertypeSources): CfirTypeAwareSupertypeProvider {
            require(sources == this.sources) { "Prepared typed inputs belong to their actual source providers" }
            return this
        }
        override fun getDirectSupertypeDescriptors(type: ConeCangJieType): List<CfirInstantiatedSupertypeDescriptor> {
            val nominal = assertInstanceOf(ConeClassLikeType::class.java, type)
            val tag = assertInstanceOf(ConeClassLikeLookupTagWithFixedSymbol::class.java, nominal.lookupTag)
            observations += nominal
            return products.getValue(tag.symbol)
        }
    }

    /** 属性仅作输入身份标记，沿用仓库 ConeAttribute 合同测试的装配方式。 */
    private data class ScopeOwnerAttribute(val label: String) : ConeAttribute<ScopeOwnerAttribute>() {
        override fun union(other: ScopeOwnerAttribute?): ScopeOwnerAttribute? = other?.takeIf { it == this }
        override fun intersect(other: ScopeOwnerAttribute?): ScopeOwnerAttribute? = other?.takeIf { it == this }
        override fun add(other: ScopeOwnerAttribute?): ScopeOwnerAttribute = other ?: this
        override fun isSubtypeOf(other: ScopeOwnerAttribute?): Boolean = other == null || other == this
        override val key: KClass<out ScopeOwnerAttribute> get() = ScopeOwnerAttribute::class
        override val keepInInferredDeclarationType: Boolean get() = true
        override val implementsEquality: Boolean get() = true
    }

    /** 未发布不能留下成功空 scope；正式空根发布后可在同一存储重新准备。 */
    @Test
    fun unpublishedProductCannotProduceScopeButPublishedEmptyRootCan() {
        val fixture = Fixture()
        val root = fixture.interfaceDeclaration("Root")
        val other = fixture.interfaceDeclaration("Other")
        fixture.install(root, other)
        val graph = MutableGraph()
        fixture.publish(graph, other)
        val storage = ScopeSession()
        assertNull(graph.getCompletedSupertypeProduct(root.symbol))
        assertThrows(IllegalStateException::class.java) {
            fixture.scope(root, storage, graph = graph)
        }
        fixture.publish(graph, root)
        val product = checkNotNull(graph.getCompletedSupertypeProduct(root.symbol))
        assertTrue(product.superTypeRefs.isEmpty())
        val scope = fixture.scope(root, storage, graph = graph)
        assertTrue(scope.parentScopeInputs.isEmpty())
        assertSame(scope, fixture.scope(root, storage, graph = graph))
    }

    /** 循环状态和版本 getter 只观察已发布产品，不能触发父输入读取回调。 */
    @Test
    fun publishedCycleStateObservationDoesNotReadOrPrepareParents() {
        val fixture = Fixture()
        val owner = fixture.interfaceDeclaration("Owner")
        val unknown = fixture.interfaceDeclaration("Unknown")
        fixture.install(owner, unknown)
        val graph = MutableGraph()
        fixture.publish(graph, owner)
        val revision = graph.modificationCount
        var reads = 0
        graph.onRead = { reads++ }
        assertEquals(false, graph.getPublishedReferenceCycleState(owner.symbol))
        assertNull(graph.getPublishedReferenceCycleState(unknown.symbol))
        owner.isInReferenceCycle = true
        assertEquals(false, graph.getPublishedReferenceCycleState(owner.symbol))
        assertEquals(revision, graph.modificationCount)
        assertEquals(0, reads)
        fixture.publish(graph, owner)
        assertEquals(true, graph.getPublishedReferenceCycleState(owner.symbol))
        assertTrue(graph.modificationCount > revision)
        assertEquals(0, reads)
        assertTrue(checkNotNull(graph.getCompletedSupertypeProduct(owner.symbol)).isInReferenceCycle)
        assertEquals(1, reads)
    }

    /** null 只免除 callable 阶段要求，仍须先准备真实 SUPER_TYPES 产品。 */
    @Test
    fun nullMemberPhasePreparesSupertypesBeforeReadingAndCachingParents() {
        val fixture = Fixture()
        val parent = fixture.interfaceDeclaration("Parent", members = listOf(fixture.function("Parent", "value", ConePrimitiveType.INT64)))
        val parentRef = fixture.ref(fixture.type(parent))
        val owner = fixture.interfaceDeclaration("Owner", initialPhase = CfirResolvePhase.IMPORTS)
        fixture.install(parent, owner)
        val graph = MutableGraph()
        fixture.publish(graph, parent)
        fixture.session.register(CfirDirectSupertypeProvider::class, graph)
        var preparationCalls = 0
        val resolver = SupertypePreparationResolver { element ->
            if (element === owner) {
                preparationCalls++
                owner.superTypeRefs.clear()
                owner.superTypeRefs.add(parentRef)
                owner.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
                fixture.publish(graph, owner)
            }
        }
        fixture.session.register(CfirLazyDeclarationResolver::class, resolver)

        val revision = graph.modificationCount
        assertEquals(revision, graph.modificationCount)
        assertEquals(0, preparationCalls)
        assertEquals(CfirResolvePhase.IMPORTS, owner.resolvePhase)
        assertNull(graph.getCompletedSupertypeProduct(owner.symbol))
        val storage = ScopeSession()
        val scope = fixture.scope(owner, storage, graph = graph, requiredPhase = null)
        assertEquals(1, preparationCalls)
        assertEquals(CfirResolvePhase.SUPER_TYPES, owner.resolvePhase)
        assertTrue(owner.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertSame(parentRef, checkNotNull(graph.getCompletedSupertypeProduct(owner.symbol)).superTypeRefs.single())
        assertSame(parentRef, (scope.parentScopeInputs.single().first.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(setOf(Name.identifier("value")), scope.getCallableNames())
        assertTrue(scope.cacheDependencies.all { it.isCurrent() })
        assertSame(scope, fixture.scope(owner, storage, graph = graph, requiredPhase = null))
        assertEquals(1, preparationCalls)
        assertEquals(0, resolver.callablePreparationCalls)
    }

    /** 完整错误输入被普通成员投影过滤后，不得查询时重新生成 Object。 */
    @Test
    fun completedErrorProjectionDoesNotInventObjectParent() {
        val fixture = Fixture()
        val core = fixture.coreModel()
        val errorRef = ConeErrorType(org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("invalid superclass"))
            .toCfirResolvedTypeRef(null)
        val owner = fixture.classDeclaration("Invalid", listOf(errorRef))
        fixture.install(core.any, core.objectDeclaration, owner)
        val graph = MutableGraph()
        fixture.publish(graph, core.any, core.objectDeclaration, owner)
        val product = checkNotNull(graph.getCompletedSupertypeProduct(owner.symbol))
        assertSame(errorRef, product.superTypeRefs.single())
        assertTrue(product.declaredSuperTypes.isEmpty())
        val scope = fixture.scope(owner, ScopeSession(), graph = graph)
        assertTrue(scope.parentScopeInputs.isEmpty())
    }

    /** 空成功映射与失败不同；每个 owner 的参数和成员映射均按真实 symbol 保存。 */
    @Test
    fun ordinaryExtendInputsFreezeAllCandidatesAndPlainMappings() {
        val fixture = Fixture()
        val core = fixture.coreModel()
        val hostSymbol = CfirClassSymbol(fixture.classId("Host"))
        val hostParameter = fixture.parameter(hostSymbol, CfirDeclarationOrigin.Source, TestSource("Host.T"))
        val objectRef = core.objectRef()
        val host = fixture.classDeclaration("Host", listOf(objectRef), hostSymbol, listOf(hostParameter),
            CfirDeclarationOrigin.Source, TestSource("Host"))
        fixture.install(core.any, core.objectDeclaration, host)
        val graph = MutableGraph()
        fixture.publish(graph, core.any, core.objectDeclaration, host)
        fixture.session.register(CfirDirectSupertypeProvider::class, graph)
        assertPublishedSourceRoot(host, objectRef, core, graph)
        val intHost = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64))
        val boolHost = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(ConePrimitiveType.BOOLEAN))
        val genericSymbol = CfirExtendSymbol()
        val genericParameter = fixture.extendParameter(genericSymbol)
        val genericReceiver = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(genericParameter.symbol.constructType()))
        val identity = fixture.extendFunction("identity", genericReceiver, genericParameter.symbol.constructType())
        val generic = fixture.extend(genericReceiver, listOf(identity), genericSymbol, listOf(genericParameter))
        val intMember = fixture.extendFunction("intMember", intHost)
        val intOnly = fixture.extend(intHost, listOf(intMember))
        val boolMember = fixture.extendFunction("boolMember", boolHost)
        val boolOnly = fixture.extend(boolHost, listOf(boolMember))
        val key = CfirExtendTargetKey.ClassLike(hostSymbol.classId)
        val extends = fixture.installExtends(listOf(generic to key, intOnly to key, boolOnly to key))
        val originals = listOf(identity, intMember, boolMember).map(::originalFunctionInput)

        val first = fixture.prepareExtendInput(key, intHost, extends)
        assertEquals(first, fixture.prepareExtendInput(key, intHost, extends))
        assertEquals(listOf(generic, intOnly, boolOnly), first.candidates.map { it.extend })
        assertEquals(setOf(generic, intOnly, boolOnly), first.owners.keys)
        assertEquals(setOf(generic, intOnly, boolOnly).map { CfirExtendMemberScope.ReceiverKey(it, intHost) }.toSet(), first.receivers.keys)
        for ((owner, input) in first.owners) {
            assertNull(input.snapshot)
            assertSame(owner.extendedTypeRef, input.targetRef)
            assertEquals(owner.declarations, input.members)
        }
        val mapped = first.receivers.getValue(CfirExtendMemberScope.ReceiverKey(generic, intHost))
        assertEquals(CfirExtendMemberScope.Applicability.EXACT, mapped.applicability)
        val expected = mapOf(genericParameter.symbol.toLookupTag() to ConePrimitiveType.INT64)
        assertEquals(expected, checkNotNull(mapped.directSubstitution).replacements)
        assertEquals(expected, checkNotNull(mapped.ownerSubstitution).replacements)
        assertEquals(expected, mapped.memberSubstitutions.getValue(identity.symbol).replacements)
        assertEquals(intHost, mapped.memberSubstitutions.getValue(identity.symbol).receiver)
        val emptySuccess = first.receivers.getValue(CfirExtendMemberScope.ReceiverKey(intOnly, intHost))
        assertEquals(CfirExtendMemberScope.Applicability.EXACT, emptySuccess.applicability)
        assertTrue(checkNotNull(emptySuccess.directSubstitution).replacements.isEmpty())
        assertTrue(checkNotNull(emptySuccess.ownerSubstitution).replacements.isEmpty())
        assertEquals(setOf(intMember.symbol), emptySuccess.memberSubstitutions.keys)
        val rejected = first.receivers.getValue(CfirExtendMemberScope.ReceiverKey(boolOnly, intHost))
        assertEquals(CfirExtendMemberScope.Applicability.INAPPLICABLE, rejected.applicability)
        assertNull(rejected.directSubstitution)
        assertNull(rejected.ownerSubstitution)
        assertTrue(rejected.memberSubstitutions.isEmpty())
        assertEquals(setOf(generic, intOnly, boolOnly), first.instantiationOwners)
        assertEquals(setOf(generic, intOnly), first.matchedPeers.getValue(intOnly))
        assertEquals(setOf(generic, boolOnly), first.matchedPeers.getValue(boolOnly))
        assertTrue(first.peerDispositions.isEmpty())

        val second = fixture.prepareExtendInput(key, boolHost, extends)
        assertEquals(mapOf(genericParameter.symbol.toLookupTag() to ConePrimitiveType.BOOLEAN),
            checkNotNull(second.receivers.getValue(CfirExtendMemberScope.ReceiverKey(generic, boolHost)).directSubstitution).replacements)
        assertNotEquals(first, second)
        assertThrows(UnsupportedOperationException::class.java) {
            (first.owners.getValue(generic).members as MutableList<CfirDeclaration>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (checkNotNull(mapped.directSubstitution).replacements as MutableMap<*, *>).clear()
        }
        val storage = ScopeSession()
        val intScope = fixture.scope(host, storage, type = intHost)
        val boolScope = fixture.scope(host, storage, type = boolHost)
        assertNotSame(intScope, boolScope)
        val names = listOf(identity.name, intMember.name, boolMember.name)
        assertFrozenQueries(intScope, listOf(identity, intMember), names)
        assertFrozenQueries(boolScope, listOf(identity, boolMember), names)
        assertSame(intScope, fixture.scope(host, storage, type = intHost))
        assertSame(boolScope, fixture.scope(host, storage, type = boolHost))
        assertSame(objectRef, (intScope.parentScopeInputs.single().first.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertSame(core.anyRef, ((intScope.parentScopeInputs.single().second as CfirClassUseSiteMemberScope)
            .parentScopeInputs.single().first.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        listOf(identity, intMember, boolMember).zip(originals).forEach { (function, input) -> assertOriginalFunctionUnchanged(function, input) }
    }

    /** 真实候选重读得到等值输入，不能因 fresh List/Map 令相同 scope 无限换代。 */
    @Test
    fun repeatedExtendPreparationCanonicalizesAndRetiresOnlyChangedMembers() {
        val fixture = Fixture()
        val owner = fixture.interfaceDeclaration("Independent")
        fixture.install(owner)
        val receiver = ConePrimitiveType.INT64
        val key = CfirExtendTargetKey.ClassLike(receiver.kind.classId)
        val member = fixture.extendFunction("before", receiver)
        val extend = fixture.extend(receiver, listOf(member))
        val extends = fixture.installExtends(listOf(extend to key), mapOf(receiver.kind to listOf(extend)))
        val original = originalFunctionInput(member)
        val storage = ScopeSession()
        fun scope() = fixture.scopes.getExtendMemberScope(fixture.session, storage, key, receiver, CfirCjmpMemberScopeView.RAW, null)
        val first = scope()
        val peerFixture = Fixture()
        val peerOwner = peerFixture.interfaceDeclaration("Peer")
        peerFixture.install(peerOwner)
        val peer = peerFixture.scope(peerOwner, storage)
        assertFrozenQueries(first.value, listOf(member), listOf(member.name))
        repeat(3) {
            assertSame(first.value, scope().value)
            assertTrue(first.isCurrent())
        }
        val later = fixture.extendFunction("later", receiver)
        (extend.declarations as MutableList<CfirDeclaration>).add(later)
        // 新成员与其 owner 索引同次发布；旧不可变 provider 不承担新成员的索引职责。
        val replacement = fixture.installExtends(listOf(extend to key), mapOf(receiver.kind to listOf(extend)))
        assertNotSame(extends, replacement)
        val next = scope()
        assertNotSame(first.value, next.value)
        assertFalse(first.isCurrent())
        assertTrue(next.isCurrent())
        assertFrozenQueries(next.value, listOf(member, later), listOf(member.name, later.name))
        assertSame(next.value, scope().value)
        assertSame(peer, peerFixture.scope(peerOwner, storage))
        assertEquals(0L, extends.modificationCount)
        assertOriginalFunctionUnchanged(member, original)
    }

    @Test
    fun rejectedAliasMatchTracksPublishedRhsAndAcceptsThenRejects() = aliasRhsLifecycle(initiallyAccepted = false)

    @Test
    fun acceptedAliasMatchTracksPublishedRhsAndRejectsThenAccepts() = aliasRhsLifecycle(initiallyAccepted = true)

    /** builtin 与 nominal 索引可指向同一 extend，但 primitive/nominal receiver 输入不能折叠。 */
    @Test
    fun primitiveAndNominalReceiverDomainsKeepSeparateInputsAndQueries() {
        val fixture = Fixture()
        fixture.install(fixture.interfaceDeclaration("Independent"))
        val intType = ConePrimitiveType.INT64
        val boolType = ConePrimitiveType.BOOLEAN
        val nominalInt = ConeClassLikeType(intType.kind.classId.toLookupTag())
        val intKey = CfirExtendTargetKey.ClassLike(intType.kind.classId)
        val boolKey = CfirExtendTargetKey.ClassLike(boolType.kind.classId)
        val intMember = fixture.extendFunction("integer", intType)
        val boolMember = fixture.extendFunction("boolean", boolType)
        val intExtend = fixture.extend(intType, listOf(intMember))
        val boolExtend = fixture.extend(boolType, listOf(boolMember))
        val originals = listOf(intMember, boolMember).map(::originalFunctionInput)
        val extends = fixture.installExtends(listOf(intExtend to intKey, boolExtend to boolKey),
            mapOf(intType.kind to listOf(intExtend), boolType.kind to listOf(boolExtend)))
        val input = fixture.prepareExtendInput(intKey, nominalInt, extends)
        assertEquals(listOf(intExtend), input.candidates.map { it.extend })
        assertEquals(intType, input.candidates.single().concreteReceiverType)
        assertEquals(setOf(CfirExtendMemberScope.ReceiverKey(intExtend, nominalInt),
            CfirExtendMemberScope.ReceiverKey(intExtend, intType)), input.receivers.keys)
        // 合成 ClassId 是索引入口，不是 primitive 语义 Ty；实际候选使用准备出的 primitive receiver。
        val nominalState = input.receivers.getValue(CfirExtendMemberScope.ReceiverKey(intExtend, nominalInt))
        assertEquals(CfirExtendMemberScope.Applicability.INAPPLICABLE, nominalState.applicability)
        assertNull(nominalState.directSubstitution)
        assertNull(nominalState.ownerSubstitution)
        assertTrue(nominalState.memberSubstitutions.isEmpty())
        val primitiveState = input.receivers.getValue(CfirExtendMemberScope.ReceiverKey(intExtend, intType))
        assertEquals(CfirExtendMemberScope.Applicability.EXACT, primitiveState.applicability)
        assertTrue(checkNotNull(primitiveState.directSubstitution).replacements.isEmpty())
        assertEquals(intType, checkNotNull(primitiveState.ownerSubstitution).receiver)
        assertEquals(intType, primitiveState.memberSubstitutions.getValue(intMember.symbol).receiver)
        val storage = ScopeSession()
        fun scope(key: CfirExtendTargetKey, receiver: ConeCangJieType) = fixture.scopes.getExtendMemberScope(
            fixture.session, storage, key, receiver, CfirCjmpMemberScopeView.RAW, null,
        ).value
        val integer = scope(intKey, intType)
        val nominal = scope(intKey, nominalInt)
        val boolean = scope(boolKey, boolType)
        assertNotSame(integer, nominal)
        assertNotSame(integer, boolean)
        val names = listOf(intMember.name, boolMember.name)
        assertFrozenQueries(integer, listOf(intMember), names)
        assertFrozenQueries(boolean, listOf(boolMember), names)
        assertFrozenQueries(nominal, listOf(intMember), names)
        assertSame(boolean, scope(boolKey, boolType))
        assertSame(nominal, scope(intKey, nominalInt))
        assertSame(integer, scope(intKey, intType))
        listOf(intMember, boolMember).zip(originals).forEach { (function, original) -> assertOriginalFunctionUnchanged(function, original) }
    }

    /** bare 是调用前的延后模板；allowBare 不能把完全实例化的失败伪装为延后。 */
    @Test
    fun bareReceiverDeferralStaysIndependentOfConcreteExactAndRejectedProducts() {
        val fixture = Fixture()
        val core = fixture.coreModel()
        val hostSymbol = CfirClassSymbol(fixture.classId("Host"))
        val hostParameter = fixture.parameter(hostSymbol)
        val host = fixture.classDeclaration("Host", listOf(core.objectRef()), hostSymbol, listOf(hostParameter))
        fixture.install(core.any, core.objectDeclaration, host)
        val symbol = CfirExtendSymbol()
        val parameter = fixture.extendParameter(symbol)
        val pattern = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(parameter.symbol.constructType()))
        val member = fixture.extendFunction("identity", pattern, parameter.symbol.constructType(), isStatic = true)
        val original = originalFunctionInput(member)
        val extend = fixture.extend(pattern, listOf(member), symbol, listOf(parameter))
        val key = CfirExtendTargetKey.ClassLike(hostSymbol.classId)
        val extends = fixture.installExtends(listOf(extend to key))
        val bare = ConeClassLikeType(hostSymbol.toLookupTag())
        val applied = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64))
        val exact = fixture.prepareExtendInput(key, applied, extends, allowBare = true)
        val deferred = fixture.prepareExtendInput(key, bare, extends, allowBare = true)
        val rejected = fixture.prepareExtendInput(key, bare, extends, allowBare = false)
        val state = deferred.receivers.getValue(CfirExtendMemberScope.ReceiverKey(extend, bare))
        assertEquals(CfirExtendMemberScope.Applicability.DEFERRED_BARE, state.applicability)
        assertNull(state.directSubstitution)
        assertNull(state.ownerSubstitution)
        assertEquals(mapOf(parameter.symbol.toLookupTag() to parameter.symbol.constructType()),
            state.memberSubstitutions.getValue(member.symbol).replacements)
        assertEquals(bare, state.memberSubstitutions.getValue(member.symbol).receiver)
        assertEquals(CfirExtendMemberScope.Applicability.EXACT,
            exact.receivers.getValue(CfirExtendMemberScope.ReceiverKey(extend, applied)).applicability)
        assertEquals(CfirExtendMemberScope.Applicability.INAPPLICABLE,
            rejected.receivers.getValue(CfirExtendMemberScope.ReceiverKey(extend, bare)).applicability)
        assertEquals(deferred, fixture.prepareExtendInput(key, bare, extends, allowBare = true))
        val storage = ScopeSession()
        fun scope(receiver: ConeCangJieType, allowBare: Boolean) = fixture.scopes.getExtendMemberScope(
            fixture.session, storage, key, receiver, CfirCjmpMemberScopeView.RAW, null, bare = allowBare,
        ).value
        val appliedScope = scope(applied, true)
        val deferredScope = scope(bare, true)
        val rejectedScope = scope(bare, false)
        assertNotSame(appliedScope, deferredScope)
        assertNotSame(deferredScope, rejectedScope)
        assertFrozenQueries(appliedScope, listOf(member), listOf(member.name))
        assertFrozenQueries(deferredScope, listOf(member), listOf(member.name))
        assertFrozenQueries(rejectedScope, emptyList(), listOf(member.name))
        assertSame(rejectedScope, scope(bare, false))
        assertSame(deferredScope, scope(bare, true))
        assertSame(appliedScope, scope(applied, true))
        assertOriginalFunctionUnchanged(member, original)

        val intOnly = fixture.extend(applied, listOf(fixture.extendFunction("onlyInt", applied, isStatic = true)))
        val intOnlyProvider = FrozenExtendProvider(listOf(intOnly to key))
        val boolHost = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(ConePrimitiveType.BOOLEAN))
        val mismatch = fixture.prepareExtendInput(key, boolHost, intOnlyProvider, allowBare = true)
        assertEquals(CfirExtendMemberScope.Applicability.INAPPLICABLE,
            mismatch.receivers.getValue(CfirExtendMemberScope.ReceiverKey(intOnly, boolHost)).applicability,
            "完全实例化的 Bool receiver 不能得到 DEFERRED_BARE")
    }

    /** 两个合法 RHS world 只更换所选 alias 的 Source 产品；原 abbreviation 和工作 RHS 不提供成功证明。 */
    private fun aliasRhsLifecycle(initiallyAccepted: Boolean) {
        val fixture = Fixture()
        val aliasFixture = Fixture()
        val core = fixture.coreModel()
        val hostSymbol = CfirClassSymbol(fixture.classId("Host"))
        val hostParameter = fixture.parameter(hostSymbol, CfirDeclarationOrigin.Source, TestSource("Host.T"))
        val objectRef = core.objectRef()
        val host = fixture.classDeclaration("Host", listOf(objectRef), hostSymbol, listOf(hostParameter),
            CfirDeclarationOrigin.Source, TestSource("Host"))
        val receiver = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64))
        val aliasId = fixture.classId("SelectedAlias")
        val firstRhs = aliasFixture.sourceRef(if (initiallyAccepted) ConePrimitiveType.INT64 else ConePrimitiveType.BOOLEAN, "SelectedAlias.RHS.initial")
        val alias = aliasFixture.sourceAlias(aliasId, firstRhs)
        val canonical = fixture.sourceAlias(aliasId, fixture.sourceRef(ConePrimitiveType.BOOLEAN, "canonicalAlias.RHS"))
        aliasFixture.install(alias)
        fixture.install(core.any, core.objectDeclaration, host, canonical, selectedSymbols = mapOf(aliasId to alias.symbol))
        val ownerGraph = MutableGraph()
        fixture.publish(ownerGraph, core.any, core.objectDeclaration, host)
        ownerGraph.publish(canonical, listOf(canonical.expandedTypeRef), emptyList(), emptyList())
        fixture.session.register(CfirDirectSupertypeProvider::class, ownerGraph)
        assertPublishedSourceRoot(host, objectRef, core, ownerGraph)
        val aliasGraph = MutableGraph()
        aliasGraph.publish(alias, listOf(firstRhs), emptyList(), emptyList())
        aliasFixture.session.register(CfirDirectSupertypeProvider::class, aliasGraph)
        assertSame(aliasFixture.session, alias.moduleData.session)
        assertEquals(CfirTypeRefOrigin.SOURCE, firstRhs.typeUseOrigin)
        assertSame(firstRhs, checkNotNull(aliasGraph.getCompletedSupertypeProduct(alias.symbol)).superTypeRefs.single())
        var symbolReads = 0
        var rhsReads = 0
        fixture.onClassLookup = { if (it == aliasId) symbolReads++ }
        aliasGraph.onRead = { if (it === alias.symbol) rhsReads++ }
        fun reads() = symbolReads to rhsReads
        // use-site 展开缓存固定为 Int64，失败/恢复必须由正式选定的 RHS 决定。
        val pattern = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(ConeTypeAliasType(aliasId, expandedType = ConePrimitiveType.INT64)))
        val selectedMember = fixture.extendFunction("selected", pattern)
        val selected = fixture.extend(pattern, listOf(selectedMember))
        val peerSymbol = CfirExtendSymbol()
        val peerParameter = fixture.extendParameter(peerSymbol)
        val peerPattern = ConeClassLikeType(hostSymbol.toLookupTag(), listOf(peerParameter.symbol.constructType()))
        val peerMember = fixture.extendFunction("peer", peerPattern, peerParameter.symbol.constructType())
        val peer = fixture.extend(peerPattern, listOf(peerMember), peerSymbol, listOf(peerParameter))
        val key = CfirExtendTargetKey.ClassLike(hostSymbol.classId)
        val extends = fixture.installExtends(listOf(selected to key, peer to key))
        val originals = listOf(selectedMember, peerMember).map(::originalFunctionInput)
        val ownerRevision = ownerGraph.modificationCount
        val extendRevision = extends.modificationCount
        val storage = ScopeSession()
        val independentFixture = Fixture()
        val independentOwner = independentFixture.interfaceDeclaration("Independent")
        independentFixture.install(independentOwner)
        val independent = independentFixture.scope(independentOwner, storage)
        fun scope() = fixture.scopes.getExtendMemberScope(fixture.session, storage, key, receiver, CfirCjmpMemberScopeView.RAW, null)
        fun useSiteScope() = fixture.scope(host, storage, type = receiver)
        val names = listOf(selectedMember.name, peerMember.name)
        fun expected(accepted: Boolean) = if (accepted) listOf(selectedMember, peerMember) else listOf(peerMember)
        fun assertPreparedInput(accepted: Boolean) {
            val input = fixture.prepareExtendInput(key, receiver, extends)
            assertEquals(listOf(selected, peer), input.candidates.map { it.extend })
            assertEquals(setOf(selected, peer), input.owners.keys)
            assertEquals(listOf(selectedMember), input.owners.getValue(selected).members)
            val selectedInput = input.receivers.getValue(CfirExtendMemberScope.ReceiverKey(selected, receiver))
            assertEquals(if (accepted) CfirExtendMemberScope.Applicability.EXACT else CfirExtendMemberScope.Applicability.INAPPLICABLE,
                selectedInput.applicability)
            if (accepted) {
                assertTrue(checkNotNull(selectedInput.directSubstitution).replacements.isEmpty())
                assertEquals(receiver, checkNotNull(selectedInput.ownerSubstitution).receiver)
                assertEquals(setOf(selectedMember.symbol), selectedInput.memberSubstitutions.keys)
            } else {
                assertNull(selectedInput.directSubstitution)
                assertNull(selectedInput.ownerSubstitution)
                assertTrue(selectedInput.memberSubstitutions.isEmpty())
            }
            val peerInput = input.receivers.getValue(CfirExtendMemberScope.ReceiverKey(peer, receiver))
            assertEquals(CfirExtendMemberScope.Applicability.EXACT, peerInput.applicability)
            assertEquals(mapOf(peerParameter.symbol.toLookupTag() to ConePrimitiveType.INT64),
                peerInput.memberSubstitutions.getValue(peerMember.symbol).replacements)
        }
        fun assertPreparedMemberMappings(scope: CfirExtendMemberScope, accepted: Boolean) {
            repeat(2) {
                val before = reads()
                val selectedMapping = scope.cjmpOwnerSubstitutorFor(selectedMember.symbol)
                if (accepted) assertEquals(ConePrimitiveType.INT64,
                    checkNotNull(selectedMapping).substituteOrSelf(checkNotNull(selectedMember.returnTypeRef.coneTypeOrNull)))
                else assertNull(selectedMapping)
                val peerMapping = checkNotNull(scope.cjmpOwnerSubstitutorFor(peerMember.symbol))
                assertEquals(ConePrimitiveType.INT64, peerMapping.substituteOrSelf(peerParameter.symbol.constructType()))
                assertEquals(before, reads(), "成员映射查询只能消费冻结 plain map")
            }
        }
        val first = scope()
        val firstUseSite = useSiteScope()
        assertTrue(symbolReads > 0, "准备必须实际选择 alias symbol")
        assertTrue(rhsReads > 0, "准备必须实际读取所选 Source RHS")
        assertFrozenQueries(first.value, expected(initiallyAccepted), names, ::reads)
        assertFrozenQueries(firstUseSite, expected(initiallyAccepted), names, ::reads)
        assertPreparedMemberMappings(first.value, initiallyAccepted)
        assertPreparedInput(initiallyAccepted)
        assertSame(first.value, scope().value)
        assertSame(firstUseSite, useSiteScope())
        assertTrue(first.isCurrent())

        for (accepted in listOf(!initiallyAccepted, initiallyAccepted)) {
            val previous = scope()
            val previousUseSite = useSiteScope()
            val nextRhs = aliasFixture.sourceRef(if (accepted) ConePrimitiveType.INT64 else ConePrimitiveType.BOOLEAN,
                "SelectedAlias.RHS.$accepted")
            val revision = aliasGraph.modificationCount
            alias.replaceExpandedTypeRef(nextRhs)
            assertEquals(revision, aliasGraph.modificationCount)
            assertSame(previous.value, scope().value, "工作 RHS 未发布不能改变完成产品")
            assertSame(previousUseSite, useSiteScope())
            assertFrozenQueries(previous.value, expected(!accepted), names, ::reads)
            assertFrozenQueries(previousUseSite, expected(!accepted), names, ::reads)
            aliasGraph.publish(alias, listOf(nextRhs), emptyList(), emptyList())
            assertSame(nextRhs, checkNotNull(aliasGraph.getCompletedSupertypeProduct(alias.symbol)).superTypeRefs.single())
            assertFalse(previous.isCurrent(), "失败匹配也必须保存 alias 的真实 Source epoch")
            val current = scope()
            val currentUseSite = useSiteScope()
            assertNotSame(previous.value, current.value)
            assertNotSame(previousUseSite, currentUseSite)
            assertTrue(current.isCurrent())
            assertTrue(aliasGraph.modificationCount > revision)
            assertFrozenQueries(current.value, expected(accepted), names, ::reads)
            assertFrozenQueries(currentUseSite, expected(accepted), names, ::reads)
            assertPreparedMemberMappings(current.value, accepted)
            assertPreparedInput(accepted)
            assertSame(current.value, scope().value)
            assertSame(currentUseSite, useSiteScope())
            assertSame(independent, independentFixture.scope(independentOwner, storage))
            assertEquals(ownerRevision, ownerGraph.modificationCount)
            assertEquals(extendRevision, extends.modificationCount)
            assertEquals(ConePrimitiveType.BOOLEAN, canonical.expandedTypeRef.coneTypeOrNull)
            listOf(selectedMember, peerMember).zip(originals).forEach { (function, input) -> assertOriginalFunctionUnchanged(function, input) }
        }
    }

    /** Source root 与 stdlib header 各自保留真实父引用，不由 scope 按空父集补造根。 */
    private fun assertPublishedSourceRoot(
        root: CfirClass,
        objectRef: CfirResolvedTypeRef,
        core: Fixture.CoreModel,
        graph: MutableGraph,
    ) {
        assertTrue(root.origin.fromSource)
        assertNotNull(root.source)
        assertSame(graph, root.moduleData.session.directSupertypeProviderOrNull)
        val product = checkNotNull(graph.getCompletedSupertypeProduct(root.symbol))
        assertSame(objectRef, root.superTypeRefs.single())
        assertSame(objectRef, product.superTypeRefs.single())
        assertSame(objectRef, product.declaredSuperTypes.single().typeRef)
        assertSame(core.objectDeclaration.symbol, product.declaredSuperTypes.single().resolvedClassSymbol)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, objectRef.typeUseOrigin)
        val objectProduct = checkNotNull(graph.getCompletedSupertypeProduct(core.objectDeclaration.symbol))
        assertSame(core.anyRef, objectProduct.superTypeRefs.single())
        assertEquals(CfirTypeRefOrigin.SOURCE, core.anyRef.typeUseOrigin)
        assertTrue(checkNotNull(graph.getCompletedSupertypeProduct(core.any.symbol)).superTypeRefs.isEmpty())
    }

    /** 名称、函数、属性和 callable 首次/重复查询分别计数；包含不存在的成员，避免只测成功路径。 */
    private fun assertFrozenQueries(
        scope: CfirTypeScope,
        expected: List<CfirNamedFunction>,
        queriedNames: List<Name>,
        reads: () -> Pair<Int, Int> = { 0 to 0 },
    ) {
        repeat(2) { round ->
            for (name in queriedNames) {
                val before = reads()
                val functions = mutableListOf<CfirNamedFunctionSymbol>()
                scope.processFunctionsByName(name, functions::add)
                assertEquals(expected.filter { it.name == name }.map { it.symbol }, functions)
                assertEquals(before, reads(), "round=$round function=$name")
                val properties = mutableListOf<CfirPropertySymbol>()
                scope.processPropertiesByName(name, properties::add)
                assertTrue(properties.isEmpty())
                assertEquals(before, reads(), "round=$round property=$name")
                val callables = mutableListOf<CfirCallableSymbol<*>>()
                scope.processCallablesByName(name, callables::add)
                assertEquals(expected.filter { it.name == name }.map { it.symbol }, callables)
                assertEquals(before, reads(), "round=$round callable=$name")
            }
            val before = reads()
            assertEquals(expected.map { it.name }.toSet(), scope.getCallableNames())
            assertTrue(scope.getClassifierNames().isEmpty())
            assertEquals(before, reads(), "round=$round names")
        }
    }

    /** 保留原签名及非空 Body 的对象身份，替换输入不能反向修改完成声明。 */
    private data class OriginalFunctionInput(
        val symbol: CfirNamedFunctionSymbol,
        val phase: CfirResolvePhase,
        val status: CfirDeclarationStatus,
        val attributes: CfirDeclarationAttributes,
        val returnTypeRef: CfirTypeRef,
        val receiver: ConeSimpleCangJieType?,
        val typeParameters: List<CfirTypeParameter>,
        val parameters: List<CfirValueParameter>,
        val parameterRefs: List<CfirTypeRef>,
        val defaults: List<CfirExpression?>,
        val body: CfirBlock,
    )

    private fun originalFunctionInput(function: CfirNamedFunction): OriginalFunctionInput {
        val body = checkNotNull(function.body)
        assertFalse(body.statements.isEmpty())
        assertEquals(function.returnTypeRef.coneTypeOrNull, body.coneTypeOrNull)
        return OriginalFunctionInput(
            function.symbol, function.resolvePhase, function.status, function.attributes, function.returnTypeRef,
            function.dispatchReceiverType, function.typeParameters.toList(), function.valueParameters.toList(),
            function.valueParameters.map { it.returnTypeRef }, function.valueParameters.map { it.defaultValue }, body,
        )
    }

    private fun assertOriginalFunctionUnchanged(function: CfirNamedFunction, input: OriginalFunctionInput) {
        assertSame(input.symbol, function.symbol)
        assertEquals(input.phase, function.resolvePhase)
        assertSame(input.status, function.status)
        assertSame(input.attributes, function.attributes)
        assertSame(input.returnTypeRef, function.returnTypeRef)
        assertSame(input.receiver, function.dispatchReceiverType)
        assertEquals(input.typeParameters.size, function.typeParameters.size)
        function.typeParameters.forEachIndexed { index, parameter -> assertSame(input.typeParameters[index], parameter) }
        assertEquals(input.parameters.size, function.valueParameters.size)
        function.valueParameters.forEachIndexed { index, parameter ->
            assertSame(input.parameters[index], parameter)
            assertSame(input.parameterRefs[index], parameter.returnTypeRef)
            assertSame(input.defaults[index], parameter.defaultValue)
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
        }
        assertSame(input.body, function.body)
        assertFalse(input.body.statements.isEmpty())
    }

    /** 正式索引 fixture 仅保存给定桶与 owner；不实现目标匹配、替换或缓存算法。 */
    private class FrozenExtendProvider(
        entries: List<Pair<CfirExtend, CfirExtendTargetKey>>,
        private val builtins: Map<PrimitiveTypeKind, List<CfirExtend>> = emptyMap(),
    ) : CfirExtendProvider {
        private val buckets = entries.groupBy({ it.second }, { it.first })
        private val all = (entries.map { it.first } + builtins.values.flatten()).distinct()
        private val memberOwners = all.flatMap { owner -> owner.declarations.map { it.symbol to owner } }.toMap()
        override val modificationCount: Long = 0L
        override fun getAllExtends(): List<CfirExtend> = all
        override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> = buckets[targetKey].orEmpty()
        override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))
        override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = all.filter { getPackageFqName(it) == packageFqName }
        override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> = builtins[kind].orEmpty()
        override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? = memberOwners[symbol]
        override fun getPackageFqName(extend: CfirExtend): FqName = FqName("cache.inputs")
    }

    /** 只模拟解析回调的正式输入发布，不模拟 scope 算法，也不生产 body 产品。 */
    private class SupertypePreparationResolver(
        private val prepare: (CfirElementWithResolveState) -> Unit,
    ) : CfirLazyDeclarationResolver() {
        var callablePreparationCalls: Int = 0
            private set
        override fun startResolvingPhase(phase: CfirResolvePhase) {}
        override fun finishResolvingPhase(phase: CfirResolvePhase) {}
        override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
            check(toPhase == CfirResolvePhase.SUPER_TYPES)
            if (element.resolvePhase < toPhase) prepare(element)
        }
        override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClassLikeDeclaration, toPhase: CfirResolvePhase) {
            callablePreparationCalls++
            error("null member phase must not request callable preparation")
        }
        override fun lazyResolveToPhaseWithCallableMembersInSupertypes(
            clazz: CfirClassLikeDeclaration, useSiteSession: CfirSession, toPhase: CfirResolvePhase,
        ) {
            callablePreparationCalls++
            error("null member phase must not request hierarchy callable preparation")
        }
    }

    /** 按真实 symbol 公布正式 refs/edges；未知节点保持未发布，不模拟 scope 失效算法。 */
    private class MutableGraph : CfirDirectSupertypeProvider {
        private val completedProducts = mutableMapOf<CfirClassLikeSymbol<*>, CfirCompletedSupertypeProduct>()
        var onRead: ((CfirClassLikeSymbol<*>) -> Unit)? = null
        override var modificationCount: Long = 0L
            private set

        fun publish(
            owner: CfirClassLikeDeclaration,
            refs: List<CfirTypeRef>,
            declaredEdges: List<CfirSuperTypeGraphEdge>,
            extendedEdges: List<CfirSuperTypeGraphEdge>,
        ) {
            val previous = completedProducts[owner.symbol]
            if (previous != null && previous.superTypeRefs == refs &&
                previous.declaredSuperTypes == declaredEdges && previous.extendedSuperTypes == extendedEdges &&
                previous.isInReferenceCycle == owner.isInReferenceCycle
            ) return
            completedProducts[owner.symbol] = CfirCompletedSupertypeProduct(
                owner.symbol, refs.toList(), declaredEdges.toList(), extendedEdges.toList(), owner.isInReferenceCycle,
            )
            modificationCount++
        }

        override fun getCompletedSupertypeProduct(ownerSymbol: CfirClassLikeSymbol<*>): CfirCompletedSupertypeProduct? {
            onRead?.invoke(ownerSymbol)
            return completedProducts[ownerSymbol]
        }

        override fun getPublishedReferenceCycleState(ownerSymbol: CfirClassLikeSymbol<*>): Boolean? =
            completedProducts[ownerSymbol]?.isInReferenceCycle

        override fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> {
            val product = completedProducts.values.singleOrNull { it.ownerSymbol.classId == ownerClassId } ?: return emptyList()
            return mergeSuperTypes(product.declaredSuperTypes, product.extendedSuperTypes)
        }

        override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> =
            getDirectSuperTypeEdges(ownerClassId).map { it.typeRef }
    }

    private class Fixture {
        val session: CfirSession = object : CfirSession(Kind.Source) {}
        val module: CfirModuleData = CfirSourceModuleData(
            Name.identifier("scope-cache"), emptyList(), emptyList(), CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also {
            it.bindSession(session)
            session.register(CfirModuleData::class, it)
        }
        val scopes = CfirCangJieScopeProvider()
        private val declarations = mutableListOf<CfirClassLikeDeclaration>()
        var onClassLookup: ((ClassId) -> Unit)? = null

        init {
            session.register(CfirCangJieScopeProvider::class, scopes)
            session.register(CfirExtendProvider::class, CfirEmptyExtendProvider())
            session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            session.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            session.register(TypeComponents::class, TypeComponents(session))
        }

        fun install(vararg inputs: CfirClassLikeDeclaration, selectedSymbols: Map<ClassId, CfirClassLikeSymbol<*>> = emptyMap()) {
            declarations += inputs
            val provider = object : CfirSymbolProvider(session) {
                override val symbolNamesProvider = CfirNullSymbolNamesProvider
                override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
                    onClassLookup?.invoke(classId)
                    return if (classId in selectedSymbols) selectedSymbols.getValue(classId)
                    else declarations.singleOrNull { it.symbol.classId == classId }?.symbol
                }
                override fun hasPackage(fqName: FqName): Boolean = declarations.any { it.symbol.classId.packageFqName == fqName }
                override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}
                override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}
                override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {}
            }
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        }

        /** 每个用例显式安装 core；普通 class 的父 ref 由调用者装配。 */
        inner class CoreModel {
            val any = interfaceDeclaration("Any", symbol = CfirInterfaceSymbol(StdlibClassIds.Any))
            val anyRef = ref(type(any)).also { it.replaceTypeUseOrigin(CfirTypeRefOrigin.SOURCE) }
            val objectDeclaration = classDeclaration(
                "Object", listOf(anyRef), CfirClassSymbol(StdlibClassIds.Object),
            )
            fun objectRef(): CfirResolvedTypeRef =
                ref(type(objectDeclaration)).also { it.replaceTypeUseOrigin(CfirTypeRefOrigin.COMPILER_ADDED) }
        }

        fun coreModel(): CoreModel = CoreModel()

        /** 发布全部递归输入，缺失 target 是 fixture 装配错误，不能作为合法空成功。 */
        fun publish(graph: MutableGraph, vararg owners: CfirClassLikeDeclaration) {
            for (owner in owners) {
                val edges = owner.superTypeRefs.mapNotNull { ref ->
                    check(ref is CfirResolvedTypeRef) { "Only completed references may be published" }
                    if (ref.coneType is ConeErrorType) return@mapNotNull null
                    val nominal = assertInstanceOf(ConeClassLikeType::class.java, ref.coneType)
                    val target = when (val tag = nominal.lookupTag) {
                        is ConeClassLikeLookupTagWithFixedSymbol -> tag.symbol
                        else -> checkNotNull(declarations.singleOrNull { it.symbol.classId == tag.classId }).symbol
                    }
                    CfirSuperTypeGraphEdge(ref, ref.toString(), target)
                }
                graph.publish(owner, owner.superTypeRefs, edges, emptyList())
            }
        }

        fun classId(name: String): ClassId = ClassId(FqName("cache.inputs"), Name.identifier(name))

        fun type(declaration: CfirClassLikeDeclaration): ConeClassLikeType =
            ConeClassLikeType(declaration.symbol.toLookupTag(), isInterface = declaration is CfirInterface)

        fun ref(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }

        /** 显式源码类型用途与 compiler-added Object ref 分开构造。 */
        fun sourceRef(type: ConeCangJieType, identity: String): CfirResolvedTypeRef = buildResolvedTypeRef {
            source = TestSource(identity)
            typeUseOrigin = CfirTypeRefOrigin.SOURCE
            coneType = type
        }

        fun parameter(
            owner: CfirClassLikeSymbol<*>,
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Library,
            source: CjSourceElement? = null,
        ): CfirTypeParameter = buildTypeParameter {
            this.source = source
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            this.origin = origin
            attributes = CfirDeclarationAttributes()
            containingDeclarationSymbol = owner
            symbol = CfirTypeParameterSymbol()
            name = Name.identifier("T")
        }

        fun function(owner: String, name: String, result: ConeCangJieType): CfirNamedFunction = buildNamedFunction {
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            status = resolvedStatus()
            isLocal = false
            isMut = false
            this.name = Name.identifier(name)
            symbol = CfirNamedFunctionSymbol(CallableId(classId(owner), this.name))
            // dispatch receiver 是不可变签名输入；创建时登记真实声明查找标签。
            dispatchReceiverType = ConeClassLikeType(classId(owner).toLookupTag())
            returnTypeRef = ref(result)
        }

        /** alias 属于自己的 Source session；完成 RHS 由该 session 的图发布，工作引用独立可变。 */
        fun sourceAlias(id: ClassId, rhs: CfirTypeRef): CfirTypeAlias = buildTypeAlias {
            source = object : CjBinarySourceElement(id.shortClassName.asString(), null, "source:$id") {}
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            status = resolvedStatus()
            scopeProvider = scopes
            symbol = CfirTypeAliasSymbol(id)
            name = id.shortClassName
            expandedTypeRef = rhs
        }

        fun extendParameter(owner: CfirExtendSymbol): CfirTypeParameter = buildTypeParameter {
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            containingDeclarationSymbol = owner
            symbol = CfirTypeParameterSymbol()
            name = Name.identifier("T")
        }

        fun extend(
            receiver: ConeCangJieType,
            members: List<CfirDeclaration>,
            symbol: CfirExtendSymbol = CfirExtendSymbol(),
            parameters: List<CfirTypeParameter> = emptyList(),
        ): CfirExtend = buildExtend {
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            status = resolvedStatus()
            this.symbol = symbol
            extendedTypeRef = ref(receiver)
            typeParameters += parameters
            declarations += members
        }

        /** 完整 identity/常量函数保留实际 Body，查询只能读取签名，不能删除或实化原声明。 */
        fun extendFunction(
            name: String,
            receiver: ConeCangJieType,
            parameterType: ConeCangJieType? = null,
            isStatic: Boolean = false,
        ): CfirNamedFunction {
            val functionSymbol = CfirNamedFunctionSymbol(CallableId(FqName("cache.inputs"), Name.identifier(name)))
            val parameter = parameterType?.let { type -> buildValueParameter {
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes()
                isLocal = false
                isNamed = false
                this.name = Name.identifier("x")
                symbol = CfirValueParameterSymbol(CallableId(this.name))
                containingDeclarationSymbol = functionSymbol
                returnTypeRef = ref(type)
            } }
            val resultType = parameterType ?: ConePrimitiveType.INT64
            val expression = if (parameter != null) buildNamedAccessExpression {
                coneTypeOrNull = resultType
                calleeReference = buildResolvedNamedReference {
                    this.name = parameter.name
                    resolvedSymbol = parameter.symbol
                }
            } else buildLiteralExpression {
                coneTypeOrNull = ConePrimitiveType.INT64
                kind = CfirLiteralKind.INT
                value = 7L
            }
            return buildNamedFunction {
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes()
                status = resolvedStatus(isStatic)
                isLocal = false
                isMut = false
                this.name = Name.identifier(name)
                symbol = functionSymbol
                // 声明 dispatch 槽仅容纳 Simple；primitive 目标保留在 extend 引用与准备映射中。
                dispatchReceiverType = when {
                    isStatic || receiver is ConePrimitiveType -> null
                    else -> assertInstanceOf(ConeSimpleCangJieType::class.java, receiver)
                }
                returnTypeRef = ref(resultType)
                if (parameter != null) valueParameters += parameter
                body = buildBlock {
                    coneTypeOrNull = resultType
                    statements += expression
                }
            }
        }

        fun installExtends(
            entries: List<Pair<CfirExtend, CfirExtendTargetKey>>,
            builtins: Map<PrimitiveTypeKind, List<CfirExtend>> = emptyMap(),
        ): FrozenExtendProvider = FrozenExtendProvider(entries, builtins).also {
            session.register(CfirExtendProvider::class, it)
        }

        /** 直接消费正式准备 API，不在 fixture 内实现 applicability 或 substitution。 */
        fun prepareExtendInput(
            key: CfirExtendTargetKey,
            receiver: ConeCangJieType,
            extends: CfirExtendProvider,
            allowBare: Boolean = false,
        ): CfirExtendMemberScope.ScopeInput = CfirExtendMemberScope.prepareScopeInput(
            targetKey = key,
            extendProvider = extends,
            session = session,
            receiverType = receiver,
            memberScopeView = CfirCjmpMemberScopeView.RAW,
            requiredPhase = null,
            scopeOwner = CfirMemberScopeOwner.UseSite,
            instantiationOwner = null,
            sources = CfirTypeAwareSupertypeSources(session, session.symbolProvider, extends, session.directSupertypeProviderOrNull),
            structuralProvider = null,
            allowBare = allowBare,
            aliasExpansion = CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
            registerParameterOwner = { parameter -> check(parameter.resolvedBounds.all { it.coneTypeOrNull != null }) },
        )

        /** 返回生成式声明实现，父引用换代直接操作其正式可变列表。 */
        fun interfaceDeclaration(
            name: String,
            parents: List<ConeCangJieType> = emptyList(),
            parameters: List<CfirTypeParameter> = emptyList(),
            members: List<CfirDeclaration> = emptyList(),
            symbol: CfirInterfaceSymbol = CfirInterfaceSymbol(classId(name)),
            initialPhase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE,
        ): CfirInterfaceImpl = CfirInterfaceImpl(
            source = null,
            moduleData = module,
            resolvePhase = initialPhase,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes(),
            deprecationsProvider = EmptyDeprecationsProvider,
            scopeProvider = scopes,
            typeParameters = parameters.toMutableList(),
            status = resolvedStatus(),
            symbol = symbol,
            superTypeRefs = parents.map(::ref).toMutableList(),
            declarations = members.toMutableList(),
            name = Name.identifier(name),
        )

        /** class fixture 同样保留生成式实现的父引用列表，不依赖 resolve 私有扩展。 */
        fun classDeclaration(
            name: String,
            parents: List<CfirTypeRef>,
            symbol: CfirClassSymbol = CfirClassSymbol(classId(name)),
            parameters: List<CfirTypeParameter> = emptyList(),
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Library,
            source: CjSourceElement? = null,
        ): CfirClassImpl = CfirClassImpl(
            source = source,
            moduleData = module,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = origin,
            attributes = CfirDeclarationAttributes(),
            deprecationsProvider = EmptyDeprecationsProvider,
            scopeProvider = scopes,
            typeParameters = parameters.toMutableList(),
            status = resolvedStatus(),
            symbol = symbol,
            superTypeRefs = parents.toMutableList(),
            declarations = mutableListOf(),
            name = Name.identifier(name),
        )

        fun scope(
            declaration: CfirClassLikeDeclaration,
            scopeSession: ScopeSession,
            type: ConeCangJieType = type(declaration),
            graph: CfirDirectSupertypeProvider? = session.directSupertypeProviderOrNull,
            requiredPhase: CfirResolvePhase? = null,
        ): CfirClassUseSiteMemberScope = scopes.getUseSiteMemberScope(
            declaration.symbol, session, scopeSession,
            ownerType = type, dispatchReceiverType = type,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
            memberRequiredPhase = requiredPhase,
            directSupertypeProvider = graph,
        )

        private fun resolvedStatus(isStatic: Boolean = false): CfirResolvedDeclarationStatus = buildResolvedDeclarationStatus {
            visibility = Visibilities.Public
            modality = Modality.FINAL
            isVisibilityExplicit = true
            isModalityExplicit = false
            isAbstractExplicit = false
            isOverride = false
            isOperator = false
            this.isStatic = isStatic
            isConst = false
            isMut = false
            isUnsafe = false
            isForeign = false
            isC = false
            isCommon = false
            isSpecific = false
            isCommonWithDefault = false
            isRedef = false
            isDefault = false
            isAbstract = false
            isOpen = false
            isSealed = false
        }
    }

    private class TestSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}
