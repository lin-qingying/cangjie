@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirCliExceptionHandler
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAvailabilityProvider
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.isInReferenceCycle
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.CfirDefaultImportsProvider
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolverImpl
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompletedSupertypeProduct
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirEmptyExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.constructorDependencyTypeOrNull
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.resolve.providers.ordinaryMemberSupertypeOrNull
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.resolve.services.CfirTypeAwareSupertypeProviderImpl
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 通过真实阶段入口验证完成引用身份、原拓扑、原生循环标记及普通成员投影。 */
class CfirSupertypesPublicationTest {
    @Test
    fun publishesBoundObjectBeforeInterfacesAndLeavesExplicitSuperclassInItsSlot() {
        val fixture = Fixture()
        val core = fixture.coreFile()
        val objectClass = core.declarations.filterIsInstance<CfirClass>().single()
        val anyRef = objectClass.superTypeRefs.single()
        val parent = ExtendTestFixtures.newClass(fixture.module, "Parent", classId("Parent"))
        val interfaceDeclaration = ExtendTestFixtures.newInterface(fixture.module, "I", classId("I"))
        val interfaceRef = ExtendTestFixtures.classTypeRef(interfaceDeclaration.symbol.classId, isInterface = true)
        val implicit = ExtendTestFixtures.newClass(fixture.module, "Implicit", classId("Implicit"), listOf(interfaceRef))
        val parentRef = ExtendTestFixtures.classTypeRef(parent.symbol.classId)
        val parentAlias = fixture.alias("ParentAlias", parentRef)
        val aliasRef = ConeTypeAliasType(parentAlias.symbol.classId, expandedType = parentRef.coneType).toCfirResolvedTypeRef(null)
        val explicit = ExtendTestFixtures.newClass(fixture.module, "Explicit", classId("Explicit"), listOf(aliasRef))
        val file = fixture.file("inheritance.cj", interfaceDeclaration, parent, parentAlias, implicit, explicit)
        fixture.install(core, file)

        val transformer = CfirSupertypeResolverTransformer(fixture.session, ScopeSession())
        transformer.transformFile(core, null)
        transformer.transformFile(file, null)
        val generated = assertInstanceOf(CfirResolvedTypeRef::class.java, implicit.superTypeRefs.first())
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, generated.typeUseOrigin)
        assertEquals(2, implicit.superTypeRefs.size)
        assertSame(interfaceRef, implicit.superTypeRefs[1])
        val edges = fixture.graph.getDirectSuperTypeEdges(implicit.symbol.classId)
        assertSame(generated, edges.first().typeRef)
        assertSame(objectClass.symbol, edges.first().resolvedClassSymbol)
        assertSame(parent.symbol, fixture.graph.getDirectSuperTypeEdges(explicit.symbol.classId).single().resolvedClassSymbol)
        assertEquals(1, explicit.superTypeRefs.size)
        assertSame(anyRef, objectClass.superTypeRefs.single())
        assertEquals(CfirTypeRefOrigin.SOURCE, anyRef.typeUseOrigin)
        val product = checkNotNull(fixture.graph.getCompletedSupertypeProduct(implicit.symbol))
        assertSame(implicit.symbol, product.ownerSymbol)
        assertSame(generated, product.superTypeRefs.first())
        assertSame(generated, product.declaredSuperTypes.first().typeRef)
        assertSame(objectClass.symbol, product.declaredSuperTypes.first().resolvedClassSymbol)
        val revision = fixture.graph.modificationCount
        transformer.transformFile(file, null)
        assertSame(generated, implicit.superTypeRefs.first())
        assertEquals(revision, fixture.graph.modificationCount)

        val typed = CfirTypeAwareSupertypeProviderImpl(fixture.session)
        fixture.session.register(CfirTypeAwareSupertypeProvider::class, typed)
        val objectDescriptor = typed.getDirectSupertypeDescriptors(implicit.symbol.constructType())
            .single { it.type.classId == StdlibClassIds.Object }
        assertSame(generated, (objectDescriptor.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        val anyDescriptor = typed.getDirectSupertypeDescriptors(objectClass.symbol.constructType()).single()
        assertEquals(StdlibClassIds.Any, anyDescriptor.type.classId)
        assertSame(anyRef, (anyDescriptor.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        for (kind in CfirClassMemberScopeKind.entries) {
            val scope = fixture.scopeProvider.getUseSiteMemberScope(
                implicit.symbol, fixture.session, ScopeSession(), scopeKind = kind,
                memberRequiredPhase = null, directSupertypeProvider = fixture.graph,
            )
            val scopeObjectRef = objectSourceRef(scope)
            assertSame(generated, scopeObjectRef)
            assertSame((objectDescriptor.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef, scopeObjectRef)
        }
    }

    /** 缺少真实 Object 是语义错误；错误首槽和原接口必须作为同一产品发布。 */
    @Test
    fun missingObjectPublishesStructuredErrorBeforeOriginalInterfaces() {
        val fixture = Fixture()
        val i = ExtendTestFixtures.newInterface(fixture.module, "I", classId("I"))
        val iRef = ExtendTestFixtures.classTypeRef(i.symbol.classId, isInterface = true)
        val owner = ExtendTestFixtures.newClass(fixture.module, "MissingCoreOwner", classId("MissingCoreOwner"), listOf(iRef))
        val file = fixture.file("missing-core.cj", i, owner)
        fixture.install(file)
        val transformer = CfirSupertypeResolverTransformer(fixture.session, ScopeSession())

        assertDoesNotThrow { transformer.transformFile(file, null) }
        assertEquals(2, owner.superTypeRefs.size)
        val errorRef = assertInstanceOf(CfirErrorTypeRef::class.java, owner.superTypeRefs.first())
        val diagnostic = assertInstanceOf(ConeSimpleDiagnostic::class.java, errorRef.diagnostic)
        assertEquals(DiagnosticKind.NoCoreObject, diagnostic.kind)
        assertEquals("`core` package should be imported", diagnostic.reason)
        assertSame(iRef, owner.superTypeRefs[1])
        val product = checkNotNull(fixture.graph.getCompletedSupertypeProduct(owner.symbol))
        assertSame(errorRef, product.superTypeRefs.first())
        assertSame(iRef, product.superTypeRefs[1])
        assertTrue(product.declaredSuperTypes.none { it.resolvedClassSymbol?.classId == StdlibClassIds.Object })
        val revision = fixture.graph.modificationCount
        assertDoesNotThrow { transformer.transformFile(file, null) }
        assertEquals(revision, fixture.graph.modificationCount)
        assertEquals(2, owner.superTypeRefs.size)
        assertSame(errorRef, owner.superTypeRefs.first())
        assertSame(iRef, owner.superTypeRefs[1])
    }

    /** 缺省 Object 必须进入计算拓扑；断环之后补父会漏掉 Object→X→Object。 */
    @Test
    fun defaultObjectReferenceParticipatesInCycleDetectionBeforePublication() {
        val fixture = Fixture()
        val x = ExtendTestFixtures.newClass(fixture.module, "X", classId("X"))
        val xRef = ExtendTestFixtures.classTypeRef(x.symbol.classId)
        val objectDeclaration = ExtendTestFixtures.newClass(fixture.module, "Object", StdlibClassIds.Object, listOf(xRef))
        val any = ExtendTestFixtures.newInterface(fixture.module, "Any", StdlibClassIds.Any)
        val core = ExtendTestFixtures.newFile(fixture.module, StdlibClassIds.Object.packageFqName, listOf(any, objectDeclaration), "cyclic-core.cj")
        val file = fixture.file("default-object-cycle.cj", x)
        fixture.install(core, file)
        val transformer = CfirSupertypeResolverTransformer(fixture.session, ScopeSession())
        transformer.transformFile(file, null)

        assertTrue(objectDeclaration.isInReferenceCycle)
        assertTrue(x.isInReferenceCycle)
        assertSame(xRef, objectDeclaration.superTypeRefs.single())
        val errorRef = assertInstanceOf(CfirErrorTypeRef::class.java, x.superTypeRefs.single())
        val diagnostic = assertInstanceOf(ConeSimpleDiagnostic::class.java, errorRef.diagnostic)
        assertEquals(DiagnosticKind.LoopInSupertype, diagnostic.kind)
        val objectRef = assertInstanceOf(CfirResolvedTypeRef::class.java, errorRef.delegatedTypeRef)
        assertEquals(StdlibClassIds.Object, objectRef.coneType.classId)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, objectRef.typeUseOrigin)
        assertSame(objectDeclaration.symbol, assertInstanceOf(ConeClassLikeType::class.java, objectRef.coneType).lookupTag.toSymbol(fixture.session))
        val xProduct = checkNotNull(fixture.graph.getCompletedSupertypeProduct(x.symbol))
        val objectProduct = checkNotNull(fixture.graph.getCompletedSupertypeProduct(objectDeclaration.symbol))
        assertTrue(xProduct.isInReferenceCycle)
        assertTrue(objectProduct.isInReferenceCycle)
        assertSame(errorRef, xProduct.superTypeRefs.single())
        assertSame(objectRef, errorRef.delegatedTypeRef)
        assertSame(xRef, objectProduct.superTypeRefs.single())
        assertTrue(xProduct.declaredSuperTypes.isEmpty())
        assertTrue(objectProduct.declaredSuperTypes.isEmpty())
        assertEquals(true, fixture.graph.getPublishedReferenceCycleState(x.symbol))
        assertEquals(true, fixture.graph.getPublishedReferenceCycleState(objectDeclaration.symbol))
        val revision = fixture.graph.modificationCount
        transformer.transformFile(file, null)
        assertSame(errorRef, x.superTypeRefs.single())
        assertSame(objectRef, errorRef.delegatedTypeRef)
        assertSame(xRef, objectDeclaration.superTypeRefs.single())
        assertEquals(revision, fixture.graph.modificationCount)
    }

    /** 多文件入口不得重复生产 alias 展开引用或在完成发布后再次改写声明树。 */
    @Test
    fun aliasReferencesAreCanonicalAcrossFilePublication() {
        val fixture = Fixture()
        val parent = ExtendTestFixtures.newInterface(fixture.module, "I", classId("I"))
        val parentRef = ExtendTestFixtures.classTypeRef(parent.symbol.classId, isInterface = true)
        val alias = fixture.alias("Alias", parentRef)
        val aliasRef = ConeTypeAliasType(alias.symbol.classId, expandedType = parentRef.coneType)
            .toCfirResolvedTypeRef(null)
        val child = ExtendTestFixtures.newClass(fixture.module, "Child", classId("Child"), listOf(aliasRef))
        val root = fixture.file("root.cj", child, alias, parent)
        val empty = fixture.file("empty.cj")
        fixture.install(fixture.coreFile(), root, empty)
        val transformer = CfirSupertypeResolverTransformer(fixture.session, ScopeSession())

        transformer.transformFile(root, null)
        val generated = child.superTypeRefs.first()
        val canonical = child.superTypeRefs[1]
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, generated.typeUseOrigin)
        assertEquals(2, child.superTypeRefs.size)
        val aliasRhs = alias.expandedTypeRef
        val revision = fixture.graph.modificationCount
        val expanded = assertInstanceOf(ConeClassLikeType::class.java, (canonical as CfirResolvedTypeRef).coneType)
        assertEquals(parent.symbol.classId, expanded.classId)
        assertTrue(expanded.isInterface)
        assertEquals(parentRef.coneType.typeArguments, expanded.typeArguments)
        // 展开类型仍携带原 alias 缩写身份，不能按无缩写属性的类型对象要求结构相等。
        assertSame(aliasRef.coneType, expanded.abbreviatedType)
        assertSame(canonical, fixture.graph.getDirectSuperTypes(child.symbol.classId)[1])

        transformer.transformFile(empty, null)
        assertSame(generated, child.superTypeRefs.first())
        assertSame(canonical, child.superTypeRefs[1])
        assertSame(aliasRhs, alias.expandedTypeRef)
        assertSame(canonical, fixture.graph.getDirectSuperTypes(child.symbol.classId)[1])
        assertEquals(revision, fixture.graph.modificationCount)
    }

    /** SDK prefix-before 的诊断由 extend 承载；同输入重放及预采集重建均保留原 carrier。 */
    @Test
    fun extendCarrierKeepsItsReferenceAndTargetAfterReplayAndRecollection() {
        val fixture = Fixture()
        val owner = ExtendTestFixtures.newClass(fixture.module, "Owner", classId("Owner"))
        val iRef = ExtendTestFixtures.classTypeRef(classId("I"), isInterface = true)
        val jRef = ExtendTestFixtures.classTypeRef(classId("J"), isInterface = true)
        val i = ExtendTestFixtures.newInterface(fixture.module, "I", classId("I"), listOf(jRef))
        val j = ExtendTestFixtures.newInterface(fixture.module, "J", classId("J"), listOf(iRef))
        val extend = ExtendTestFixtures.newExtend(
            fixture.module, ExtendTestFixtures.classTypeRef(owner.symbol.classId), listOf(iRef),
        )
        val root = fixture.file("cycle.cj", owner, i, j, extend)
        val empty = fixture.file("empty.cj")
        fixture.install(fixture.coreFile(), root, empty)
        val processor = CfirSupertypeResolverProcessor(fixture.session, ScopeSession())
        processor.beforePhase()
        processor.transformer.transformFile(root, null)
        val carrier = assertInstanceOf(CfirErrorTypeRef::class.java, extend.superTypeRefs.single())
        assertEquals(DiagnosticKind.LoopInSupertype, (carrier.diagnostic as ConeSimpleDiagnostic).kind)
        assertSame(iRef, carrier.delegatedTypeRef)
        assertFalse(owner.isInReferenceCycle)
        assertTrue(i.isInReferenceCycle)
        assertTrue(j.isInReferenceCycle)
        assertSame(jRef, i.superTypeRefs.single())
        assertSame(iRef, j.superTypeRefs.single())
        val revision = fixture.graph.modificationCount

        processor.transformer.transformFile(empty, null)
        processor.beforePhase()
        processor.transformer.transformFile(root, null)
        val edge = fixture.graph.getDirectSuperTypeEdges(owner.symbol.classId).single { it.sourceExtend === extend }
        assertSame(carrier, extend.superTypeRefs.single())
        assertSame(carrier, edge.typeRef)
        assertSame(i.symbol, edge.resolvedClassSymbol)
        assertSame(extend, edge.sourceExtend)
        assertEquals(revision, fixture.graph.modificationCount)
        assertTrue(i.isInReferenceCycle)
        assertTrue(j.isInReferenceCycle)
        assertFalse(owner.isInReferenceCycle)
        assertTrue(fixture.graph.getDirectSuperTypes(i.symbol.classId).isEmpty())
        assertTrue(fixture.graph.getDirectSuperTypes(j.symbol.classId).isEmpty())
    }

    /** 原 GetTarget 可保留元数错误的 nominal 目标，普通成员及构造器投影仍拒绝该边。 */
    @Test
    fun recoverableArityErrorParticipatesOnlyInReferenceTopology() {
        val fixture = Fixture()
        val parameter = ExtendTestFixtures.newTypeParameter(fixture.module, "T")
        val aRef = ExtendTestFixtures.classTypeRef(classId("A"), isInterface = true)
        val b = ExtendTestFixtures.newInterface(
            fixture.module, "B", classId("B"), listOf(aRef), typeParameters = listOf(parameter),
        )
        val provided = listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN).map { it.toCfirResolvedTypeRef(null) }
        val nominal = ExtendTestFixtures.classTypeRef(
            b.symbol.classId, listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN), isInterface = true,
        )
        val arityError = ConeErrorType(
            ConeUnmatchedTypeArgumentsError(b.symbol, expectedCount = 1, actualCount = 2, providedTypeArguments = provided),
            delegatedType = nominal.coneType,
        ).toCfirResolvedTypeRef(null, nominal)
        val a = ExtendTestFixtures.newInterface(fixture.module, "A", classId("A"), listOf(arityError))
        val root = fixture.file("arity.cj", a, b)
        fixture.install(root)

        assertNull(arityError.ordinaryMemberSupertypeOrNull(a, fixture.session))
        assertNull(arityError.classifyDeclaredSupertype(fixture.session).constructorDependencyTypeOrNull())
        CfirSupertypeResolverTransformer(fixture.session, ScopeSession()).transformFile(root, null)
        assertTrue(a.isInReferenceCycle)
        assertTrue(b.isInReferenceCycle)
        assertTrue(fixture.graph.getDirectSuperTypes(a.symbol.classId).isEmpty())
        assertTrue(fixture.graph.getDirectSuperTypes(b.symbol.classId).isEmpty())
    }

    /** alias 透明地连接两个接口，不能把 alias 声明或环外前缀标记为原生 nominal 环节点。 */
    @Test
    fun aliasIsTransparentToNominalReferenceCycleMembership() {
        val fixture = Fixture()
        val iRef = ExtendTestFixtures.classTypeRef(classId("I"), isInterface = true)
        val jRef = ExtendTestFixtures.classTypeRef(classId("J"), isInterface = true)
        val alias = fixture.alias("ToJ", jRef)
        val aliasRef = ConeTypeAliasType(alias.symbol.classId, expandedType = jRef.coneType).toCfirResolvedTypeRef(null)
        val i = ExtendTestFixtures.newInterface(fixture.module, "I", classId("I"), listOf(aliasRef))
        val j = ExtendTestFixtures.newInterface(fixture.module, "J", classId("J"), listOf(iRef))
        val prefix = ExtendTestFixtures.newClass(fixture.module, "Prefix", classId("Prefix"), listOf(iRef))
        val root = fixture.file("alias-cycle.cj", prefix, i, j, alias)
        fixture.install(fixture.coreFile(), root)

        CfirSupertypeResolverTransformer(fixture.session, ScopeSession()).transformFile(root, null)
        assertTrue(i.isInReferenceCycle)
        assertTrue(j.isInReferenceCycle)
        assertFalse(alias.isInReferenceCycle)
        assertFalse(prefix.isInReferenceCycle)
        val remaining = fixture.graph.getDirectSuperTypeEdges(prefix.symbol.classId).single()
        assertEquals(StdlibClassIds.Object, remaining.resolvedClassSymbol?.classId)
        assertSame(prefix.superTypeRefs.first(), remaining.typeRef)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, remaining.typeRef.typeUseOrigin)
    }

    /** 指定根阶段可以发布外根循环标记，必须保留外根的原始未解析引用及 IMPORTS 阶段。 */
    @Test
    fun markerOnlyDeclarationKeepsAllOriginalReferencesAndPhase() {
        val fixture = Fixture()
        val future = buildUserTypeRef {
            source = object : CjBinarySourceElement("Future", null, "pending:Future") {}
            qualifier += buildQualifierPart { name = Name.identifier("Future") }
        }
        val aRef = ExtendTestFixtures.classTypeRef(classId("A"), isInterface = true)
        val bRef = ExtendTestFixtures.classTypeRef(classId("B"), isInterface = true)
        val a = ExtendTestFixtures.newInterface(fixture.module, "A", classId("A"), listOf(bRef))
        val b = ExtendTestFixtures.newInterface(fixture.module, "B", classId("B"), listOf(aRef, future))
        val root = fixture.file("designated.cj", a, b)
        fixture.install(root)
        val bPhase = b.resolvePhase
        val bRefs = b.superTypeRefs.toList()

        a.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration(fixture.session, ScopeSession(), root)
        assertTrue(a.isInReferenceCycle)
        assertTrue(b.isInReferenceCycle)
        assertEquals(CfirResolvePhase.SUPER_TYPES, a.resolvePhase)
        assertEquals(bPhase, b.resolvePhase)
        assertEquals(CfirResolvePhase.IMPORTS, b.resolvePhase)
        assertEquals(bRefs.size, b.superTypeRefs.size)
        bRefs.zip(b.superTypeRefs).forEach { (before, after) -> assertSame(before, after) }
        assertNull(fixture.graph.getCompletedSupertypeProduct(b.symbol))
        assertEquals(true, fixture.graph.getPublishedReferenceCycleState(b.symbol))
        assertEquals(true, fixture.graph.getPublishedReferenceCycleState(a.symbol))
    }

    /** 原 ref 合同的 DirectSupertypeProvider 也必须经同一实际 interface owner 判据。 */
    @Test
    fun declarationEdgeProviderCannotSupplyClassMembersToInterfaceOwner() {
        val fixture = Fixture()
        val member = buildNamedFunction {
            moduleData = fixture.module
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            isLocal = false
            isMut = false
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(classId("Base"), Name.identifier("classMember")))
            name = Name.identifier("classMember")
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        }
        val base = ExtendTestFixtures.newClass(fixture.module, "Base", classId("Base"), declarations = listOf(member))
        val baseRef = ExtendTestFixtures.classTypeRef(base.symbol.classId)
        val owner = ExtendTestFixtures.newInterface(fixture.module, "Illegal", classId("Illegal"), listOf(baseRef))
        val core = fixture.coreFile()
        val file = fixture.file("illegal.cj", base, owner)
        fixture.install(core, file)
        val transformer = CfirSupertypeResolverTransformer(fixture.session, ScopeSession())
        transformer.transformFile(core, null)
        transformer.transformFile(file, null)
        val completedProducts = (core.declarations.filterIsInstance<CfirClassLikeDeclaration>() + listOf(base, owner))
            .associate { declaration -> declaration.symbol to checkNotNull(fixture.graph.getCompletedSupertypeProduct(declaration.symbol)) }
        val publishedRevision = fixture.graph.modificationCount
        val rawProvider = object : CfirDirectSupertypeProvider {
            override val modificationCount: Long = publishedRevision
            override fun getCompletedSupertypeProduct(ownerSymbol: CfirClassLikeSymbol<*>): CfirCompletedSupertypeProduct? =
                completedProducts[ownerSymbol]
            override fun getPublishedReferenceCycleState(ownerSymbol: CfirClassLikeSymbol<*>): Boolean? =
                completedProducts[ownerSymbol]?.isInReferenceCycle
            override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> =
                completedProducts.values.singleOrNull { it.ownerSymbol.classId == ownerClassId }
                    ?.superTypes?.map { it.typeRef }.orEmpty()
        }
        val ownerProduct = checkNotNull(rawProvider.getCompletedSupertypeProduct(owner.symbol))
        assertSame(baseRef, ownerProduct.superTypeRefs.single())
        assertTrue(ownerProduct.declaredSuperTypes.isEmpty())
        assertEquals(false, rawProvider.getPublishedReferenceCycleState(owner.symbol))
        val unknown = ExtendTestFixtures.newInterface(fixture.module, "Unknown", classId("Unknown"))
        assertNull(rawProvider.getPublishedReferenceCycleState(unknown.symbol))
        for (kind in CfirClassMemberScopeKind.entries) {
            val scope = fixture.scopeProvider.getUseSiteMemberScope(
                owner.symbol, fixture.session, ScopeSession(), scopeKind = kind,
                memberRequiredPhase = null, directSupertypeProvider = rawProvider,
            )
            assertFalse(member.name in scope.getCallableNames())
        }
    }

    /** 跨模块内部观察只限此测试 helper，生产 scope API 保持既有可见性。 */
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    private fun objectSourceRef(scope: CfirClassUseSiteMemberScope): CfirTypeRef {
        val objectInput = scope.parentScopeInputs.single { it.first.type.classId == StdlibClassIds.Object }
        return (objectInput.first.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef
    }

    /** 注册完整正式 construction 产品与真正解析组件，不以测试 resolver 替代生产阶段。 */
    private class Fixture {
        val sessionAndModule = ExtendTestFixtures.newSessionAndModule("supertype-products")
        val session = sessionAndModule.first
        val module = sessionAndModule.second
        val graph = CfirSuperTypeGraphStore()
        val scopeProvider = CfirCangJieScopeProvider()
        private val provider = CfirProviderImpl(session, scopeProvider)

        init {
            session.register(CfirProvider::class, provider)
            session.register(CfirSymbolProvider::class, provider.symbolProvider)
            session.register(CfirCangJieScopeProvider::class, scopeProvider)
            session.register(CfirExtendProvider::class, CfirEmptyExtendProvider())
            session.register(CfirImportBindingStore::class, CfirImportBindingStore())
            session.register(CfirExceptionHandler::class, CfirCliExceptionHandler)
            session.register(CfirDirectSupertypeProvider::class, graph)
            session.register(CfirSuperTypeGraphStore::class, graph)
            session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
            session.register(CfirDeclarationAvailabilityProvider::class, CfirDeclarationAvailabilityProvider(session))
            session.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(session))
            session.register(CfirTypeResolver::class, CfirTypeResolverImpl(session))
            session.register(
                CfirDefaultImportsProviderHolder::class,
                CfirDefaultImportsProviderHolder.of(CfirDefaultImportsProvider),
            )
        }

        /** core 模型显式安装；缺库负例不得由 fixture 默认补齐。 */
        fun coreFile(): CfirFile {
            val any = ExtendTestFixtures.newInterface(module, "Any", StdlibClassIds.Any)
            val anyRef = ExtendTestFixtures.classTypeRef(StdlibClassIds.Any, isInterface = true).also {
                it.replaceTypeUseOrigin(CfirTypeRefOrigin.SOURCE)
            }
            val objectDeclaration = ExtendTestFixtures.newClass(module, "Object", StdlibClassIds.Object, listOf(anyRef))
            return ExtendTestFixtures.newFile(module, StdlibClassIds.Object.packageFqName, listOf(any, objectDeclaration), "core.cj")
        }

        fun alias(name: String, expanded: CfirTypeRef): CfirTypeAlias = buildTypeAlias {
            moduleData = module
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            status = CfirDeclarationStatusImpl()
            symbol = CfirTypeAliasSymbol(classId(name))
            this.name = Name.identifier(name)
            scopeProvider = this@Fixture.scopeProvider
            expandedTypeRef = expanded
        }

        fun file(name: String, vararg declarations: CfirDeclaration): CfirFile =
            ExtendTestFixtures.newFile(module, PACKAGE, declarations.toList(), name)

        fun install(vararg files: CfirFile) {
            val pre = buildPreMacroRawFiles(
                session, files.map { PreMacroCfirFile(it, annotationMetadata = CfirAnnotationMetadataBuilder()) },
            )
            val result = MacroConstructionService.successOf(pre, files.toList(), MacroExpansionRegistry.EMPTY)
            recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
            val imports = CfirImportResolveTransformer(session, CfirDiagnosticCollector())
            files.forEach { imports.transformDeclaration(it, null) }
        }
    }

    companion object {
        private val PACKAGE = FqName("supertype.products")
        private fun classId(name: String): ClassId = ClassId(PACKAGE, Name.identifier(name))
    }
}
