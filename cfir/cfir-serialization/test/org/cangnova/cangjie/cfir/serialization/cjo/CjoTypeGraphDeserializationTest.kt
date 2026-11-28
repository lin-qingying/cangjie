@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.GenericTyInfo
import PackageFormat.Package
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjSourceLocation
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * S5 的真实 CJO 类型图测试：writer 字节经过正式声明、类型 loader 及库 provider。
 *
 * 权威：s5-type-identity-native-authority-20261119.md，固定 native 14276740 的
 * ReferenceLoader::SetGenericTy / SetTypeTy。源码正例已用官方 1.1.3 cjc 编译：
 * Pair/Relay 泛型 Alias、Relation<T> F-bound、Relation<U> 前向参数及 Box<T>.choose<U>。
 * 所有 d/t 引用在 metadata 中为 1-based；公开 loader 下标为 0-based。
 */
class CjoTypeGraphDeserializationTest {
    /** Alias self、RHS 和实际应用分别保留 classifier 与各自的参数域。 */
    @Test
    fun aliasSelfRhsAndUseKeepOwnParameterIdentityInEitherLoadOrder() {
        for (typeFirst in listOf(false, true)) {
            val context = context(aliasMetadata())
            val types = CfirTypeDeserializer(context)
            if (typeFirst) {
                types.deserializeType(7) // Relay<Int64>：应用不应被误认作 Relay 的 self。
                types.deserializeType(5) // Relay<T> self 会先请求其真实 owner 和 own TP。
                types.deserializeType(2) // Pair<T> self。
            }
            val declarations = CfirDeclDeserializer(context, types)
            val pair = assertIs<CfirTypeAlias>(declarations.deserializeDecl(0))
            val relay = assertIs<CfirTypeAlias>(declarations.deserializeDecl(2))
            val pairParameter = assertIs<CfirTypeParameter>(pair.typeParameters.single())
            val relayParameter = assertIs<CfirTypeParameter>(relay.typeParameters.single())
            assertEquals(pairParameter.name, relayParameter.name)
            assertNotSame(pairParameter.symbol, relayParameter.symbol)
            assertSame(pair.symbol, pairParameter.containingDeclarationSymbol)
            assertSame(relay.symbol, relayParameter.containingDeclarationSymbol)
            assertSame(pairParameter, context.declCache[1])
            assertSame(relayParameter, context.declCache[3])

            val pairSelf = assertIs<ConeClassLikeType>(types.deserializeType(2))
            val relaySelf = assertIs<ConeClassLikeType>(types.deserializeType(5))
            assertEquals(pair.symbol.classId, pairSelf.classId)
            assertEquals(relay.symbol.classId, relaySelf.classId)
            assertParameter(pairSelf.typeArguments.single().type, pairParameter.symbol)
            assertParameter(relaySelf.typeArguments.single().type, relayParameter.symbol)
            val pairRhs = assertIs<ConeTupleType>(pair.expandedTypeRef.coneType)
            assertEquals(2, pairRhs.elementTypes.size)
            pairRhs.elementTypes.forEach { assertParameter(it, pairParameter.symbol) }
            val relayRhs = assertIs<ConeClassLikeType>(relay.expandedTypeRef.coneType)
            assertEquals(pair.symbol.classId, relayRhs.classId)
            assertParameter(relayRhs.typeArguments.single().type, relayParameter.symbol)
            assertSame(relayRhs, types.deserializeType(4))

            val use = assertIs<ConeClassLikeType>(types.deserializeType(7))
            assertEquals(relay.symbol.classId, use.classId)
            assertEquals(ConePrimitiveType.INT64, use.typeArguments.single().type)
            val function = assertIs<CfirNamedFunction>(declarations.deserializeDecl(4))
            assertSame(use, function.returnTypeRef.coneType)
            assertSame(use, function.valueParameters.single().returnTypeRef.coneType)
            assertSame(function.symbol, function.valueParameters.single().containingDeclarationSymbol)
            assertSame(use, CfirTypeDeserializer(context).deserializeType(7))
            assertSame(pair, context.createDeclDeserializer().deserializeDecl(0))
            assertSame(relay, context.createDeclDeserializer().deserializeDecl(2))
            assertTrue(pairParameter.bounds.isEmpty(), "present-empty final bounds 不能被默认 Any 填充")
            assertTrue(relayParameter.bounds.isEmpty())
            assertTrue(context.reservedTypeParameters.isEmpty(), "完成的请求不能泄漏 reservation")
        }
    }

    /** 回环是 T → Relation<T> → T，不是非法的直接 T <: T。 */
    @Test
    fun fBoundKeepsTheCanonicalParameterWhenTypeOrDeclarationIsRequestedFirst() {
        for (typeFirst in listOf(false, true)) {
            val context = context(recursiveMetadata(mutual = false))
            val types = CfirTypeDeserializer(context)
            val firstType = if (typeFirst) types.deserializeType(2) else null
            val function = assertIs<CfirNamedFunction>(CfirDeclDeserializer(context, types).deserializeDecl(2))
            val parameter = assertIs<CfirTypeParameter>(function.typeParameters.single())
            val generic = types.deserializeType(2)
            assertParameter(generic, parameter.symbol)
            if (typeFirst) assertSame(firstType, generic)
            val bound = assertIs<ConeClassLikeType>(parameter.bounds.single().coneType)
            assertEquals(classId("typegraph", "Relation"), bound.classId)
            assertParameter(bound.typeArguments.single().type, parameter.symbol)
            assertSame(generic, bound.typeArguments.single().type)
            assertSame(bound, types.deserializeType(3))
            assertSame(parameter, context.declCache[3])
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
            assertParameter(function.returnTypeRef.coneType, parameter.symbol)
            assertParameter(function.valueParameters.single().returnTypeRef.coneType, parameter.symbol)
            assertSame(function, context.createDeclDeserializer().deserializeDecl(2))
            assertTrue(context.reservedTypeParameters.isEmpty())
            assertTrue(context.genericBoundsInProgress.isEmpty())
        }
    }

    /** T 的 nominal 上界先用后列 U；U 的 nominal 上界回指 T，全部 TP 必须先有真实身份。 */
    @Test
    fun forwardParameterAndMutualNominalBoundsKeepBothBinders() {
        for (firstTypeIndex in listOf<Int?>(null, 2, 3, 4, 5)) {
            val context = context(recursiveMetadata(mutual = true))
            val types = CfirTypeDeserializer(context)
            firstTypeIndex?.let(types::deserializeType)
            val function = assertIs<CfirNamedFunction>(CfirDeclDeserializer(context, types).deserializeDecl(2))
            val parameters = function.typeParameters.map { assertIs<CfirTypeParameter>(it) }
            assertEquals(listOf("T", "U"), parameters.map { it.name.asString() })
            val (t, u) = parameters
            assertNotSame(t.symbol, u.symbol)
            assertSame(function.symbol, t.containingDeclarationSymbol)
            assertSame(function.symbol, u.containingDeclarationSymbol)
            assertSame(t, context.declCache[3])
            assertSame(u, context.declCache[4])
            assertParameter(types.deserializeType(2), t.symbol)
            assertParameter(types.deserializeType(3), u.symbol)
            val tBound = assertIs<ConeClassLikeType>(t.bounds.single().coneType)
            val uBound = assertIs<ConeClassLikeType>(u.bounds.single().coneType)
            assertEquals(classId("typegraph", "Relation"), tBound.classId)
            assertEquals(tBound.classId, uBound.classId)
            assertParameter(tBound.typeArguments.single().type, u.symbol)
            assertParameter(uBound.typeArguments.single().type, t.symbol)
            assertSame(types.deserializeType(3), tBound.typeArguments.single().type)
            assertSame(types.deserializeType(2), uBound.typeArguments.single().type)
            val result = assertIs<ConeTupleType>(function.returnTypeRef.coneType)
            assertParameter(result.elementTypes[0], t.symbol)
            assertParameter(result.elementTypes[1], u.symbol)
            assertEquals(2, function.valueParameters.size)
            function.valueParameters.zip(parameters).forEach { (value, parameter) ->
                assertParameter(value.returnTypeRef.coneType, parameter.symbol)
                assertSame(function.symbol, value.containingDeclarationSymbol)
            }
            assertTrue(context.reservedTypeParameters.isEmpty())
            assertTrue(context.genericBoundsInProgress.isEmpty())
        }
    }

    /** 原组的重复 RHS 与同 subject 的派生组独立保留；不构造源码禁止的重复显式 subject。 */
    @Test
    fun repeatedAndImplicitConstraintGroupsRemainIndependentOfSemanticBounds() {
        val input = recursiveMetadata(mutual = false)
        val originalGroups = listOf(
            CjoConstraintMetadata(3u, listOf(4u, 4u), begin = CjoPositionMetadata(file = 1u, line = 19, column = 11),
                end = CjoPositionMetadata(file = 1u, line = 19, column = 40)),
            CjoConstraintMetadata(3u, listOf(4u), isImplicitlyIntroduced = true),
        )
        val declarations = input.declarations.toMutableList()
        declarations[2] = declarations[2].copy(generic = CjoGenericMetadata(listOf(4u), originalGroups))
        val metadata = input.copy(declarations = declarations, allFiles = listOf("s5-type-graph-authority.cj"))
        for (typeFirst in listOf(false, true)) {
            val context = context(metadata)
            val types = CfirTypeDeserializer(context)
            if (typeFirst) types.deserializeType(2)
            val function = assertIs<CfirNamedFunction>(CfirDeclDeserializer(context, types).deserializeDecl(2))
            val parameter = assertIs<CfirTypeParameter>(function.typeParameters.single())
            val groups = assertNotNull(context.genericConstraints[function.symbol])
            assertEquals(2, groups.size)
            assertEquals(listOf(2, 1), groups.map { it.upperBounds.size })
            assertEquals(listOf(false, true), groups.map { it.isImplicitlyIntroduced })
            val bound = types.deserializeType(3)
            groups.forEach { group ->
                assertParameter(group.subject, parameter.symbol)
                assertSame(types.deserializeType(2), group.subject)
                group.upperBounds.forEach { assertSame(bound, it.coneType) }
            }
            assertSame(groups[0].upperBounds[0].coneType, groups[0].upperBounds[1].coneType)
            assertEquals(CjSourceLocation("s5-type-graph-authority.cj", 19, 11), groups[0].begin)
            assertEquals(CjSourceLocation("s5-type-graph-authority.cj", 19, 40), groups[0].end)
            assertNull(groups[1].begin)
            assertNull(groups[1].end)
            assertSame(bound, parameter.bounds.single().coneType)
            assertSame(function, context.createDeclDeserializer().deserializeDecl(2))
            assertSame(groups, context.genericConstraints[function.symbol])
            assertEquals(1, parameter.bounds.size, "原组的重复 RHS 不能追加到最终语义上界")
            assertTrue(context.reservedTypeParameters.isEmpty())
        }
    }

    /** 可缺省的 Decl.type 不改变 TP 身份；最终 bounds 必须从同表的真实 GenericTyInfo 恢复。 */
    @Test
    fun missingParameterTypeFieldUsesTheSameTableSemanticGenericEntry() {
        val input = recursiveMetadata(mutual = false)
        val declarations = input.declarations.toMutableList()
        declarations[3] = declarations[3].copy(type = 0u)
        val metadata = input.copy(declarations = declarations)
        for (typeFirst in listOf(false, true)) {
            val context = context(metadata, requireExplicitParameterTypes = false)
            val types = CfirTypeDeserializer(context)
            if (typeFirst) types.deserializeType(2)
            val function = assertIs<CfirNamedFunction>(CfirDeclDeserializer(context, types).deserializeDecl(2))
            val parameter = assertIs<CfirTypeParameter>(function.typeParameters.single())
            assertSame(parameter, context.declCache[3])
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
            assertParameter(types.deserializeType(2), parameter.symbol)
            val semanticBound = types.deserializeType(3)
            assertSame(semanticBound, parameter.bounds.single().coneType)
            assertParameter(assertIs<ConeClassLikeType>(semanticBound).typeArguments.single().type, parameter.symbol)
            val groups = assertNotNull(context.genericConstraints[function.symbol])
            assertEquals(1, groups.size)
            assertSame(semanticBound, groups.single().upperBounds.single().coneType)
            assertParameter(groups.single().subject, parameter.symbol)
            assertTrue(context.reservedTypeParameters.isEmpty())
        }
    }

    /** type-first 的成员 header 也必须经 class owner 恢复外层 TP 与构造器借用关系。 */
    @Test
    fun ownAndOuterHeaderParametersRemainDistinctInEitherLoadOrder() {
        for (firstTypeIndex in listOf<Int?>(null, 0, 4, 7)) {
            val context = context(headerMetadata())
            val types = CfirTypeDeserializer(context)
            firstTypeIndex?.let(types::deserializeType)
            val owner = assertIs<CfirClass>(CfirDeclDeserializer(context, types).deserializeDecl(0))
            assertHeader(owner)
            val outer = assertIs<CfirTypeParameter>(owner.typeParameters.single())
            val method = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
            val own = assertIs<CfirTypeParameter>(method.typeParameters.single())
            assertSame(outer, context.declCache[1])
            assertSame(own, context.declCache[5])
            assertParameter(types.deserializeType(0), outer.symbol)
            assertParameter(types.deserializeType(4), own.symbol)
            val signature = assertIs<ConeFunctionType>(types.deserializeType(7))
            assertParameter(signature.parameterTypes[0], outer.symbol)
            assertParameter(signature.parameterTypes[1], own.symbol)
            assertSame(signature.returnType, method.returnTypeRef.coneType)
            assertSame(owner, context.createDeclDeserializer().deserializeDecl(0))
            assertTrue(context.reservedTypeParameters.isEmpty())
        }
    }

    /** 与现有 CJO 集成测试相同的 manager/session/provider 装配，验证正式 classifier 查询。 */
    @Test
    fun providerPublishesTheCompleteOwnAndOuterHeader(@TempDir tempDir: Path) {
        val fixture = providerFixture(tempDir, listOf(headerMetadata()))
        val id = classId("typegraph", "Box")
        val owner = assertIs<CfirClass>(assertNotNull(fixture.provider.getClassLikeSymbolByClassId(id)).cfir)
        assertHeader(owner)
        assertSame(owner.symbol, fixture.provider.getClassLikeSymbolByClassId(id))
        val canonical = fixture.provider.deserializeCjoDeclaration(
            tempDir.resolve("typegraph.cjo"), "typegraph", 5,
        )
        val method = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertSame(method.typeParameters.single(), canonical)
        assertHeader(assertIs<CfirClass>(assertNotNull(fixture.provider.getClassLikeSymbolByClassId(id)).cfir))
    }

    /** imported allTypes 的 Generic FullId 由真实 provider callback 路由，不伪造外包 TP 声明。 */
    @Test
    fun importedGenericReferencesUseTheProviderCanonicalOwnerInEitherOrder(@TempDir tempDir: Path) {
        val dependency = aliasMetadata("dependency")
        val consumer = metadata("consumer", emptyList(), listOf(
            CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(
                declarationIndex = 0u, packageId = 0, declarationKey = "dependency::Pair.T",
            )),
            CjoTypeMetadata(TypeKind.Int64),
            CjoTypeMetadata(TypeKind.Type, listOf(2u), semanticInfo = CjoCompositeTypeInfoMetadata(
                declarationIndex = 0u, packageId = 0, declarationKey = "dependency::Pair",
            )),
        ), imports = listOf("dependency"))
        for (typeFirst in listOf(false, true)) {
            val root = tempDir.resolve(if (typeFirst) "type-first" else "owner-first")
            val fixture = providerFixture(root, listOf(dependency, consumer))
            val loaded = fixture.manager.loadPackageSnapshots("consumer").single()
            val context = CfirDeserializationContext(
                loaded.pkg, loaded.header, fixture.module, fixture.manager, loaded.sourcePath,
                declarationResolver = fixture.provider::deserializeCjoDeclaration,
            )
            val pairId = classId("dependency", "Pair")
            if (!typeFirst) fixture.provider.getClassLikeSymbolByClassId(pairId)
            val imported = CfirTypeDeserializer(context).deserializeType(0)
            val pair = assertIs<CfirTypeAlias>(assertNotNull(fixture.provider.getClassLikeSymbolByClassId(pairId)).cfir)
            val parameter = assertIs<CfirTypeParameter>(pair.typeParameters.single())
            assertParameter(imported, parameter.symbol)
            assertSame(pair.symbol, parameter.containingDeclarationSymbol)
            val dependencyPath = root.resolve("dependency.cjo")
            assertSame(parameter, fixture.provider.deserializeCjoDeclaration(dependencyPath, "dependency", 1))
            assertSame(imported, CfirTypeDeserializer(context).deserializeType(0))
            assertTrue(context.declCache.isEmpty(), "外包 canonical TP 不属于 consumer 的 d 缓存")
            assertTrue(context.reservedTypeParameters.isEmpty())
            val use = assertIs<ConeClassLikeType>(CfirTypeDeserializer(context).deserializeType(2))
            assertEquals(pairId, use.classId)
            assertEquals(ConePrimitiveType.INT64, use.typeArguments.single().type)
            val relay = assertIs<CfirTypeAlias>(assertNotNull(
                fixture.provider.getClassLikeSymbolByClassId(classId("dependency", "Relay")),
            ).cfir)
            assertNotSame(parameter.symbol, relay.typeParameters.single().symbol)
            assertParameter(assertIs<ConeClassLikeType>(relay.expandedTypeRef.coneType).typeArguments.single().type,
                relay.typeParameters.single().symbol)
        }
    }

    /** 同包/同 exportId 的两个 parent 输入按真实模块依赖选 Q2，不能按 manager 的第一个快照选择。 */
    @Test
    fun doubleParentFullIdSelectsOnlyTheReachableSnapshotAndRejectsAmbiguity(@TempDir tempDir: Path) {
        for (selection in ParentSelection.entries) {
            val root = tempDir.resolve(selection.name)
            val q1Path = root.resolve("q1/parent.cjo")
            val q2Path = root.resolve("q2/parent.cjo")
            val parent = aliasMetadata("parent")
            CjoPackageWriter.write(q1Path, parent.copy(moduleName = "Q1"))
            CjoPackageWriter.write(q2Path, parent.copy(moduleName = "Q2"))
            val manager = CjoManager(
                CjoSearchPath(envProvider = { null }),
                CjoFormatVersionPolicy.DeferredToConsumerSession,
                explicitCommonPartPaths = listOf(q1Path, q2Path),
            )
            val q1 = Module("Q1")
            val q2 = Module("Q2")
            val bridge = Module("bridge", refinementDependencies = listOf(q2))
            val library = Module("library")
            val p = Module("P", dependencies = when (selection) {
                ParentSelection.Q2 -> listOf(q2)
                ParentSelection.TRANSITIVE_REFINEMENT -> listOf(bridge)
                ParentSelection.BOTH -> listOf(q1, q2)
                ParentSelection.NONE -> emptyList()
            })
            val moduleDataProvider = MultipleModuleDataProvider(
                linkedMapOf(
                    library to LibraryPathFilter.RejectAll,
                    q1 to LibraryPathFilter.LibraryList(setOf(q1Path)),
                    q2 to LibraryPathFilter.LibraryList(setOf(q2Path)),
                ),
                regularDependenciesModuleData = library,
            )
            val provider = CfirDeserializedSymbolProvider(
                library.session, manager, CfirCangJieScopeProvider(), library, moduleDataProvider,
            )
            library.session.register(CfirSymbolProvider::class, provider)
            p.session.register(CfirSymbolProvider::class, provider)
            val bytes = CjoPackageWriter.toByteArray(metadata("consumer", emptyList(), listOf(
                CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(
                    0u, packageId = 0, declarationKey = "parent::Pair.T",
                )),
                CjoTypeMetadata(TypeKind.Int64),
            ), imports = listOf("parent")))
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
            val context = CfirDeserializationContext(
                pkg, CjoPackageHeader.fromPackage(pkg), p, manager,
                declarationResolver = provider::deserializeCjoDeclaration,
                dependencySnapshotResolver = { provider.selectImportedSnapshot(p, it) },
            )
            val snapshots = manager.loadPackageSnapshots("parent")
            assertEquals(listOf("Q1", "Q2"), snapshots.map { it.pkg.moduleName })
            val types = CfirTypeDeserializer(context)
            val completedInt64 = types.deserializeType(1)
            val completedTypes = context.typeCache
            val completedDeclarations = context.declCache
            val completedConstraints = context.genericConstraints
            if (selection == ParentSelection.Q2 || selection == ParentSelection.TRANSITIVE_REFINEMENT) {
                val selected = assertNotNull(provider.selectImportedSnapshot(p, "parent"))
                assertSame(snapshots[1], selected)
                assertEquals(q2Path.toAbsolutePath().normalize(), selected.sourcePath)
                val imported = types.deserializeType(0)
                val parameter = assertIs<CfirTypeParameter>(provider.deserializeCjoDeclaration(q2Path, "parent", 1))
                assertParameter(imported, parameter.symbol)
                assertSame(q2, parameter.moduleData)
                assertSame(parameter.symbol, assertIs<ConeTypeParameterType>(imported).lookupTag.typeParameterSymbol)
                assertEquals(selected.sourcePath, context.importedPackageIndices[0]?.sourcePath)
                assertSame(imported, CfirTypeDeserializer(context).deserializeType(0))
                val other = assertIs<CfirTypeParameter>(provider.deserializeCjoDeclaration(q1Path, "parent", 1))
                assertSame(q1, other.moduleData)
                assertEquals(parameter.name, other.name)
                assertNotSame(parameter.symbol, other.symbol)
                assertParameter(CfirTypeDeserializer(context).deserializeType(0), parameter.symbol)
            } else {
                assertFailsWith<IllegalStateException> { provider.selectImportedSnapshot(p, "parent") }
                assertFailsWith<IllegalStateException> { types.deserializeType(0) }
                assertSame(completedTypes, context.typeCache)
                assertSame(completedDeclarations, context.declCache)
                assertSame(completedConstraints, context.genericConstraints)
                assertEquals(setOf(1), context.typeCache.keys)
                assertTrue(context.importedPackageIndices.isEmpty())
                assertTrue(context.genericTypes.isEmpty())
                assertTrue(context.genericBounds.isEmpty())
                assertTrue(context.reservedTypeParameters.isEmpty())
                assertTrue(context.genericBoundsInProgress.isEmpty())
                assertTrue(context.types.isEmpty())
                assertTrue(context.declarations.isEmpty())
                assertTrue(context.constraints.isEmpty())
                assertFailsWith<IllegalStateException> { CfirTypeDeserializer(context).deserializeType(0) }
                assertSame(completedTypes, context.typeCache)
            }
            assertSame(completedInt64, CfirTypeDeserializer(context).deserializeType(1))
            assertTrue(context.declCache.isEmpty(), "consumer 不应拥有 parent TP")
        }
    }

    /** R2：缺失 ArrayTyInfo 的损坏 VArray 明确拒绝，不能制造 size=0 的成功类型。 */
    @Test
    fun missingVArraySizeInfoRejectsWithoutPollutingCompletedCaches() {
        val context = context(metadata("missingvarrayinfo", emptyList(), listOf(
            CjoTypeMetadata(TypeKind.Int64),
            CjoTypeMetadata(TypeKind.VArray, listOf(1u)),
        )))
        val types = CfirTypeDeserializer(context)
        val completedElement = types.deserializeType(0)
        val completedTypes = context.typeCache
        val completedDeclarations = context.declCache
        val completedConstraints = context.genericConstraints
        assertFailsWith<IllegalStateException> { types.deserializeType(1) }
        assertSame(completedTypes, context.typeCache)
        assertSame(completedDeclarations, context.declCache)
        assertSame(completedConstraints, context.genericConstraints)
        assertEquals(setOf(0), context.typeCache.keys)
        assertSame(completedElement, CfirTypeDeserializer(context).deserializeType(0))
        assertTrue(context.reservedTypeParameters.isEmpty())
        assertTrue(context.genericTypes.isEmpty())
        assertTrue(context.types.isEmpty())
        assertTrue(context.declarations.isEmpty())
        assertTrue(context.constraints.isEmpty())
    }

    /** 真实存在的 ArrayTyInfo.dimsOrSize=0 是合法尺寸，与缺失 info 不同。 */
    @Test
    fun explicitVArraySizeZeroPreservesTheNativeDimensionField() {
        val context = context(metadata("zerovarray", emptyList(), listOf(
            CjoTypeMetadata(TypeKind.Int64),
            CjoTypeMetadata(TypeKind.VArray, listOf(1u), semanticInfo = CjoArrayTypeInfoMetadata(0L)),
        )))
        val array = assertIs<ConeVArrayType>(CfirTypeDeserializer(context).deserializeType(1))
        assertEquals(0L, array.size)
        assertEquals(ConePrimitiveType.INT64, array.elementType)
        assertSame(context.typeCache[0], array.elementType)
        assertSame(array, CfirTypeDeserializer(context).deserializeType(1))
        assertTrue(context.declCache.isEmpty())
        assertTrue(context.reservedTypeParameters.isEmpty())
    }

    private enum class ParentSelection { Q2, TRANSITIVE_REFINEMENT, BOTH, NONE }

    /** native wire 0 是 Invalid；显式 Unit 必须占一个真实 SemaTy 槽。 */
    @Test
    fun zeroTypeFieldsKeepStructuredErrorsInsteadOfBecomingUnit() {
        val context = context(metadata("invalidfields", emptyList(), listOf(
            CjoTypeMetadata(TypeKind.Unit),
            CjoTypeMetadata(TypeKind.Int64),
            CjoTypeMetadata(TypeKind.Tuple, listOf(0u, 2u)),
            functionType(listOf(2u), 0u),
        )))
        val types = CfirTypeDeserializer(context)
        assertError(types.deserializeTypeFromField(0u))
        assertEquals(ConePrimitiveType.UNIT, types.deserializeTypeFromField(1u))
        val tuple = assertIs<ConeTupleType>(types.deserializeType(2))
        assertError(tuple.elementTypes[0])
        assertEquals(ConePrimitiveType.INT64, tuple.elementTypes[1])
        val function = assertIs<ConeFunctionType>(types.deserializeType(3))
        assertError(function.returnType)
        assertEquals(listOf(ConePrimitiveType.INT64), function.parameterTypes)
        assertFalse(context.typeCache.containsKey(-1))
        assertTrue(context.declCache.isEmpty())
        assertTrue(context.reservedTypeParameters.isEmpty())
    }

    /** 损坏的纯结构环不能借用 Generic 的合法 identity-ready 回边合同。 */
    @Test
    fun structuralCyclesRejectAndRollBackWithoutPublishingTypeParameters() {
        for (firstIndex in listOf(0, 1)) {
            val context = context(metadata("structuralcycle", emptyList(), listOf(
                CjoTypeMetadata(TypeKind.Tuple, listOf(2u, 3u)),
                CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 3u)),
                CjoTypeMetadata(TypeKind.Int64),
            )))
            val types = CfirTypeDeserializer(context)
            val completedPrimitive = types.deserializeType(2)
            val completedTypes = context.typeCache
            val completedDeclarations = context.declCache
            val completedConstraints = context.genericConstraints
            assertFailsWith<IllegalStateException> { types.deserializeType(firstIndex) }
            assertSame(completedTypes, context.typeCache)
            assertSame(completedDeclarations, context.declCache)
            assertSame(completedConstraints, context.genericConstraints)
            assertEquals(setOf(2), context.typeCache.keys)
            assertSame(completedPrimitive, CfirTypeDeserializer(context).deserializeType(2))
            // 换入口和 decoder 再请求同一个损坏图，也不能留下本次事务的部分节点。
            assertFailsWith<IllegalStateException> { CfirTypeDeserializer(context).deserializeType(1 - firstIndex) }
            assertSame(completedTypes, context.typeCache)
            assertTrue(context.declCache.isEmpty())
            assertTrue(context.reservedTypeParameters.isEmpty())
            assertTrue(context.genericTypes.isEmpty())
            assertTrue(context.genericBoundsInProgress.isEmpty())
            assertTrue(context.types.isEmpty())
            assertTrue(context.declarations.isEmpty())
            assertTrue(context.constraints.isEmpty())
        }
    }

    /** 完整 header：构造器借用 Box.T；choose 的 U、参数、返回值、receiver 和 bound 各保留身份。 */
    private fun assertHeader(owner: CfirClass) {
        val outer = assertIs<CfirTypeParameter>(owner.typeParameters.single())
        val method = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val own = assertIs<CfirTypeParameter>(method.typeParameters.single())
        assertEquals("T", outer.name.asString())
        assertEquals("U", own.name.asString())
        assertNotSame(outer.symbol, own.symbol)
        assertSame(owner.symbol, outer.containingDeclarationSymbol)
        assertSame(method.symbol, own.containingDeclarationSymbol)
        assertTrue(outer.bounds.isEmpty())
        val bound = assertIs<ConeClassLikeType>(own.bounds.single().coneType)
        assertEquals(classId("typegraph", "Relation"), bound.classId)
        assertParameter(bound.typeArguments.single().type, outer.symbol)
        assertEquals(classId("typegraph", "Box"), assertIs<ConeClassLikeType>(method.dispatchReceiverType).classId)
        assertParameter(assertIs<ConeClassLikeType>(method.dispatchReceiverType).typeArguments.single().type, outer.symbol)
        assertEquals(2, method.valueParameters.size)
        assertParameter(method.valueParameters[0].returnTypeRef.coneType, outer.symbol)
        assertParameter(method.valueParameters[1].returnTypeRef.coneType, own.symbol)
        method.valueParameters.forEach { assertSame(method.symbol, it.containingDeclarationSymbol) }
        val result = assertIs<ConeTupleType>(method.returnTypeRef.coneType)
        assertEquals(2, result.elementTypes.size)
        assertParameter(result.elementTypes[0], outer.symbol)
        assertParameter(result.elementTypes[1], own.symbol)
        val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
        assertFalse(constructor.typeParameters.single() is CfirTypeParameter)
        assertSame(outer.symbol, constructor.typeParameters.single().symbol)
        assertSame(owner.symbol, outer.containingDeclarationSymbol)
        assertParameter(constructor.valueParameters.single().returnTypeRef.coneType, outer.symbol)
        assertSame(constructor.symbol, constructor.valueParameters.single().containingDeclarationSymbol)
        assertNull(constructor.dispatchReceiverType)
        val constructed = assertIs<ConeClassLikeType>(constructor.returnTypeRef.coneType)
        assertEquals(owner.symbol.classId, constructed.classId)
        assertParameter(constructed.typeArguments.single().type, outer.symbol)
    }

    private fun assertParameter(type: ConeCangJieType, symbol: CfirTypeParameterSymbol) {
        val parameterType = assertIs<ConeTypeParameterType>(type)
        assertSame(symbol, parameterType.lookupTag.typeParameterSymbol)
        assertSame(symbol.cfir, parameterType.lookupTag.typeParameterSymbol.cfir)
    }

    private fun assertError(type: ConeCangJieType) {
        assertTrue(assertIs<ConeErrorType>(type).diagnostic.reason.isNotBlank())
    }

    /** 仅 additional type 引入 Alias/TP；捕获必须消费准备结果，不能再次查询或展开别名。 */
    @Test
    fun exportPreparationClosesAliasAndParameterIdentitiesBeforeReadOnlyCapture(@TempDir root: Path) {
        val fixture = providerFixture(root, listOf(aliasMetadata("r38.alias")))
        val provider = fixture.provider
        val path = root.resolve("r38.alias.cjo").toAbsolutePath().normalize()
        val function = assertIs<CfirNamedFunction>(provider.deserializeCjoDeclaration(path, "r38.alias", 4))
        val use = function.returnTypeRef.coneType
        fixture.module.session.register(org.cangnova.cangjie.cfir.types.TypeComponents::class,
            org.cangnova.cangjie.cfir.types.TypeComponents(fixture.module.session))
        val guard = R38ExportPreparationGuard(fixture.module.session, provider)
        guard.proveGuards(function)
        val inputs = mapOf(fixture.module.session to listOf(use))
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            emptyList(), emptyList(), ::r38PhysicalMembers, additionalTypeInputs = inputs)
        guard.observeOwners(prepared.declarations)
        val pair = assertIs<CfirTypeAlias>(provider.deserializeCjoDeclaration(path, "r38.alias", 0))
        val relay = assertIs<CfirTypeAlias>(provider.deserializeCjoDeclaration(path, "r38.alias", 2))
        val pairParameter = assertIs<CfirTypeParameter>(pair.typeParameters.single())
        val relayParameter = assertIs<CfirTypeParameter>(relay.typeParameters.single())
        listOf(pair, relay, pairParameter, relayParameter).forEach { target ->
            assertTrue(prepared.declarations.any { it === target })
            guard.assertHeaderReady(target)
        }
        assertNotSame(pairParameter.symbol, relayParameter.symbol)
        assertTrue(pairParameter.bounds.isEmpty())
        assertTrue(relayParameter.bounds.isEmpty())
        val input = guard.readOnly {
            org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                emptyList(), emptyList(), ::r38PhysicalMembers, additionalTypeInputs = inputs, preparedInput = prepared)
        }
        guard.readOnly {
            val expanded = assertIs<ConeTupleType>(input.typeElement(use, fixture.module.session).expandedType)
            assertEquals(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64), expanded.elementTypes)
            assertSame(pair.expandedTypeRef.coneType, input.aliasUnderlyingTypeOf(pair))
            assertSame(relay.expandedTypeRef.coneType, input.aliasUnderlyingTypeOf(relay))
            assertSame(relay, input.typeElement(expanded, fixture.module.session).abbreviatedDeclaration)
            assertSame(pair.symbol, pairParameter.containingDeclarationSymbol)
            assertSame(relay.symbol, relayParameter.containingDeclarationSymbol)
            input.validateSourceUnchanged()
        }
    }

    /** 真正的 F-bound 为 T -> Relation<T> -> T；闭包不能用默认上界或按短名合并 TP。 */
    @Test
    fun exportPreparationClosesRecursiveGenericBoundBeforeReadOnlyCapture(@TempDir root: Path) {
        val fixture = providerFixture(root, listOf(recursiveMetadata(mutual = false)))
        val function = assertIs<CfirNamedFunction>(fixture.provider.deserializeCjoDeclaration(
            root.resolve("typegraph.cjo").toAbsolutePath().normalize(), "typegraph", 2))
        val parameter = assertIs<CfirTypeParameter>(function.typeParameters.single())
        val bound = parameter.bounds.single().coneType
        val generic = function.returnTypeRef.coneType
        val guard = R38ExportPreparationGuard(fixture.module.session, fixture.provider)
        val inputs = mapOf(fixture.module.session to listOf(generic))
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            emptyList(), emptyList(), ::r38PhysicalMembers, additionalTypeInputs = inputs)
        guard.observeOwners(prepared.declarations)
        assertTrue(prepared.declarations.any { it === parameter })
        val input = guard.readOnly {
            org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                emptyList(), emptyList(), ::r38PhysicalMembers, additionalTypeInputs = inputs, preparedInput = prepared)
        }
        guard.readOnly {
            assertSame(bound, assertIs<org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypePayload.Generic>(
                input.typeElement(assertNotNull(input.declarationTypeOf(parameter)), fixture.module.session).payload).bounds.single())
            val payload = assertIs<org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypePayload.Generic>(
                input.typeElement(generic, fixture.module.session).payload)
            assertSame(parameter, payload.parameter)
            assertSame(bound, payload.bounds.single())
            val nominal = assertIs<org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypePayload.Nominal>(
                input.typeElement(bound, fixture.module.session).payload)
            assertSame(parameter.symbol, assertIs<ConeTypeParameterType>(nominal.arguments.single()).lookupTag.typeParameterSymbol)
            assertNotNull(nominal.declaration)
            input.validateSourceUnchanged()
        }
    }

    /** Pair<T>=(T,T)，Relay<T>=Pair<T>，aliasIdentity 接收/返回 Relay<Int64>。 */
    private fun aliasMetadata(packageName: String = "typegraph"): CjoPackageMetadata = metadata(packageName, listOf(
        CjoPackageDeclaration("Pair", DeclKind.TypeAliasDecl, type = 3u, attributes = flags(Attribute.PUBLIC),
            exportId = "$packageName::Pair", generic = CjoGenericMetadata(listOf(2u)), info = CjoAliasInfo(2u)),
        parameter("T", 1u, exportId = "$packageName::Pair.T"),
        CjoPackageDeclaration("Relay", DeclKind.TypeAliasDecl, type = 6u, attributes = flags(Attribute.PUBLIC),
            exportId = "$packageName::Relay", generic = CjoGenericMetadata(listOf(4u)), info = CjoAliasInfo(5u)),
        parameter("T", 4u, exportId = "$packageName::Relay.T"),
        CjoPackageDeclaration("aliasIdentity", type = 9u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = functionInfo(listOf(6u), 8u)),
        valueParameter("value", 8u),
    ), listOf(
        genericType(2u),
        CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 1u)),
        nominal(TypeKind.Type, 1u, listOf(1u)),
        genericType(4u),
        nominal(TypeKind.Type, 1u, listOf(4u)),
        nominal(TypeKind.Type, 3u, listOf(4u)),
        CjoTypeMetadata(TypeKind.Int64),
        nominal(TypeKind.Type, 3u, listOf(7u)),
        functionType(listOf(8u), 8u),
    ))

    /** Relation<X> 无约束；成功例只在 nominal 上界参数中形成回边。 */
    private fun recursiveMetadata(mutual: Boolean): CjoPackageMetadata {
        val declarations = mutableListOf(
            CjoPackageDeclaration("Relation", DeclKind.InterfaceDecl, type = 1u, attributes = flags(Attribute.PUBLIC),
                generic = CjoGenericMetadata(listOf(2u)), info = CjoInterfaceInfo()),
            parameter("X", 2u),
        )
        if (mutual) {
            declarations += listOf(
                CjoPackageDeclaration("weave", type = 8u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                    generic = CjoGenericMetadata(listOf(4u, 5u), listOf(
                        CjoConstraintMetadata(3u, listOf(5u)), CjoConstraintMetadata(4u, listOf(6u)),
                    )), info = functionInfo(listOf(6u, 7u), 7u)),
                parameter("T", 3u), parameter("U", 4u),
                valueParameter("left", 3u), valueParameter("right", 4u),
            )
            return metadata("typegraph", declarations, listOf(
                nominal(TypeKind.Interface, 1u, listOf(2u)), genericType(2u),
                genericType(4u, listOf(5u)), genericType(5u, listOf(6u)),
                nominal(TypeKind.Interface, 1u, listOf(4u)), nominal(TypeKind.Interface, 1u, listOf(3u)),
                CjoTypeMetadata(TypeKind.Tuple, listOf(3u, 4u)), functionType(listOf(3u, 4u), 7u),
            ))
        }
        declarations += listOf(
            CjoPackageDeclaration("recur", type = 5u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                generic = CjoGenericMetadata(listOf(4u), listOf(CjoConstraintMetadata(3u, listOf(4u)))),
                info = functionInfo(listOf(5u), 3u)),
            parameter("T", 3u), valueParameter("value", 3u),
        )
        return metadata("typegraph", declarations, listOf(
            nominal(TypeKind.Interface, 1u, listOf(2u)), genericType(2u),
            genericType(4u, listOf(4u)), nominal(TypeKind.Interface, 1u, listOf(3u)),
            functionType(listOf(3u), 3u),
        ))
    }

    private fun headerMetadata(): CjoPackageMetadata = metadata("typegraph", listOf(
        CjoPackageDeclaration("Box", DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC),
            generic = CjoGenericMetadata(listOf(2u)), info = CjoClassInfo(body = listOf(3u, 5u))),
        parameter("T", 1u),
        CjoPackageDeclaration("init", isTopLevel = false, type = 4u, attributes = flags(Attribute.PUBLIC, Attribute.CONSTRUCTOR),
            info = functionInfo(listOf(4u), 3u)),
        valueParameter("value", 1u),
        CjoPackageDeclaration("choose", isTopLevel = false, type = 8u, attributes = flags(Attribute.PUBLIC),
            generic = CjoGenericMetadata(listOf(6u), listOf(CjoConstraintMetadata(5u, listOf(7u)))),
            info = functionInfo(listOf(7u, 8u), 6u)),
        parameter("U", 5u), valueParameter("outer", 1u), valueParameter("own", 5u),
        CjoPackageDeclaration("Relation", DeclKind.InterfaceDecl, type = 10u, attributes = flags(Attribute.PUBLIC),
            generic = CjoGenericMetadata(listOf(10u)), info = CjoInterfaceInfo()),
        parameter("X", 9u),
    ), listOf(
        genericType(2u), nominal(TypeKind.Class, 1u, listOf(1u)), CjoTypeMetadata(TypeKind.Unit),
        functionType(listOf(1u), 3u), genericType(6u, listOf(7u)), CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 5u)),
        nominal(TypeKind.Interface, 9u, listOf(1u)), functionType(listOf(1u, 5u), 6u),
        genericType(10u), nominal(TypeKind.Interface, 9u, listOf(9u)),
    ))

    private fun parameter(name: String, type: UInt, exportId: String? = null) =
        CjoPackageDeclaration(name, DeclKind.GenericParamDecl, isTopLevel = false, type = type, exportId = exportId)

    private fun valueParameter(name: String, type: UInt) =
        CjoPackageDeclaration(name, DeclKind.FuncParam, isTopLevel = false, type = type,
            info = CjoParameterInfo(isNamed = false, isMember = false))

    private fun genericType(declaration: UInt, upperBounds: List<UInt> = emptyList()) =
        CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(declaration, upperBounds))

    private fun nominal(kind: UShort, declaration: UInt, arguments: List<UInt>) =
        CjoTypeMetadata(kind, arguments, semanticInfo = CjoCompositeTypeInfoMetadata(declaration))

    private fun functionType(parameters: List<UInt>, result: UInt) =
        CjoTypeMetadata(TypeKind.Func, parameters, semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))

    private fun functionInfo(parameters: List<UInt>, result: UInt) =
        CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(parameters), listOf(emptyList()), result))

    /** 固定 1.1.3 schema；CJO header 的 0.1.0 是格式版本，不是语言版本。 */
    private fun metadata(
        packageName: String,
        declarations: List<CjoPackageDeclaration>,
        types: List<CjoTypeMetadata>,
        imports: List<String> = emptyList(),
    ) = CjoPackageMetadata(
        fullPackageName = packageName, moduleName = packageName,
        schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3, cjoVersion = CjoFormatVersion(0u, 1u, 0u),
        declarations = declarations, types = types, imports = imports,
    )

    private fun context(metadata: CjoPackageMetadata, requireExplicitParameterTypes: Boolean = true): CfirDeserializationContext {
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))
        // 在进入 loader 前确认 TP.type 真正指向自己的 Generic，而非零字段或另一 TP。
        metadata.declarations.forEachIndexed { index, declaration ->
            if (declaration.kind != DeclKind.GenericParamDecl) return@forEachIndexed
            if (declaration.type == 0u) {
                assertFalse(requireExplicitParameterTypes, "普通正例 TP 必须有真实 Generic type 字段")
                return@forEachIndexed
            }
            assertTrue(declaration.type > 0u)
            val type = assertNotNull(pkg.allTypes(declaration.type.toInt() - 1))
            assertEquals(TypeKind.Generic, type.kind)
            val info = assertIs<GenericTyInfo>(type.info(GenericTyInfo()))
            assertEquals(-2, info.declPtr?.pkgId)
            assertEquals((index + 1).toUInt(), info.declPtr?.index)
        }
        return CfirDeserializationContext(
            pkg, CjoPackageHeader.fromPackage(pkg), Module(),
            CjoManager(CjoSearchPath { null }, CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT)),
        )
    }

    private data class ProviderFixture(
        val module: Module,
        val manager: CjoManager,
        val provider: CfirDeserializedSymbolProvider,
    )

    private fun providerFixture(root: Path, packages: List<CjoPackageMetadata>): ProviderFixture {
        packages.forEach { CjoPackageWriter.write(root.resolve(it.fullPackageName + ".cjo"), it) }
        val manager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile())),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
        )
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.session.register(CfirSymbolProvider::class, provider)
        return ProviderFixture(module, manager, provider)
    }

    private fun classId(packageName: String, name: String) = ClassId(FqName(packageName), Name.identifier(name))

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }

    /** 复用 CjoDefaultValueMetadataTest / CjdBinaryAnnotationIntegrationTest 的正式 library session 装配。 */
    private class Module(
        moduleName: String = "cjo-type-graph-test",
        override val dependencies: List<CfirModuleData> = emptyList(),
        override val refinementDependencies: List<CfirModuleData> = emptyList(),
    ) : CfirModuleData() {
        override val name = Name.identifier(moduleName)
        override val allRefinementDependencies = refinementDependencies
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = moduleName
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }
}
