@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.Package
import PackageFormat.ReferenceInfo
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.registerTypeAwareSupertypeComponents
import org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceAbsence
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceRole
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 正式 Writer → Manager → Library provider → canonical SOURCE 的引用形式回归。
 * 不构造 SOURCE target DTO，不借上下文补造 Declaration，也不修改已有损坏 wire 负例。
 *
 * 固定 N 14276740ffec1262b81b6b5236b1d840af270b37 的 ExprWriter.cpp 中 RefExpr
 * 保存 ReferenceInfo(reference, GetFullDeclIndex(target), instTys, matchedParentTy)；
 * 原生 this/super 使用 RefExpr 的保留名称。ASTSerializeUtils.h 的 FullId 包索引
 * -1/-2/-3 分别表示无效、当前声明、package 引用。字段缺失与显式 -1 分开保存。
 * this 位于 primitive extend；package 位于合法静态成员链；super 位于子类成员访问。
 */
class CjoSourceReferenceFormsTest {
    @Test
    fun builtinExtendThisWithoutTargetKeepsFieldMissing(@TempDir root: Path) {
        assertBuiltinThis(root, null, CfirSourceReferenceAbsence.FIELD_MISSING)
    }

    @Test
    fun builtinExtendThisWithMinusOneKeepsTheExplicitSentinel(@TempDir root: Path) {
        assertBuiltinThis(root, CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value, decl = ""),
            CfirSourceReferenceAbsence.INVALID_SENTINEL)
    }

    private fun assertBuiltinThis(root: Path, target: CjoExpressionTargetMetadata?, reason: CfirSourceReferenceAbsence) {
        // extend Int64 { public func probe(): Int64 { return this } }
        val fixture = load(root, metadata(listOf(
            CjoPackageDeclaration("", kind = DeclKind.ExtendDecl, type = 1u,
                info = CjoExtendInfo(body = listOf(2u))),
            CjoPackageDeclaration("probe", kind = DeclKind.FuncDecl, type = 2u, isTopLevel = false,
                attributes = flags(Attribute.PUBLIC, Attribute.IN_EXTEND), info = function(3u)),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType()), listOf(
            reference("this", 1u, target),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 1u, operands = listOf(1u)),
            block(2u),
        )))
        val info = fixture.wire.allExprs(0)!!.info(ReferenceInfo()) as ReferenceInfo
        assertEquals("this", info.reference)
        if (target == null) assertNull(info.target) else {
            assertEquals(-1, assertNotNull(info.target).pkgId)
            assertEquals("", info.target!!.decl)
            assertEquals(0u, info.target!!.index)
        }
        val owner = fixture.module.session.extendProvider.getExtendsForBuiltinType(ConePrimitiveType.INT64.kind).single()
        assertIs<CfirExtend>(owner)
        val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val graph = fixture.graph(function)
        val identity = graph.functionBodies.getValue(function)
        val body = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(identity).payload)
        val returned = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(
            assertIs<org.cangnova.cangjie.cfir.source.CfirSourceSlot.Expression>(body.statements.single()).identity).payload)
        val record = graph.expression(returned.value)
        val payload = assertIs<CfirSourceExpressionPayload.Reference>(record.payload)
        assertEquals(Name.identifier("this"), payload.name)
        assertEquals(CfirSourceReferenceRole.THIS, payload.role)
        assertEquals(reason, assertIs<CfirSourceReferenceTarget.Absent>(payload.target).reason)
        assertNotEquals(CfirSourceReferenceTarget.Absent(CfirSourceReferenceAbsence.FIELD_MISSING),
            CfirSourceReferenceTarget.Absent(CfirSourceReferenceAbsence.INVALID_SENTINEL))
        assertSame(CfirReferenceParentType.Absent, payload.matchedParentType)
        assertTrue(payload.instantiatedTypes.isEmpty())
        assertEquals(ConePrimitiveType.INT64, record.type)
        assertSame(owner, graph.declarationOwners.getValue(function))
        repeat(3) {
            assertSame(graph, fixture.graph(function))
            assertSame(record, graph.expression(returned.value))
            assertSame(payload, graph.expression(returned.value).payload)
        }
    }

    @Test
    fun packageQualifierRemainsPackageBeforeClassAndStaticFieldDeclarations(@TempDir root: Path) {
        // public class Owner { public static let answer = 7 }
        // public func probe(): Int64 { return referenceforms.Owner.answer }
        val fixture = load(root, metadata(listOf(
            CjoPackageDeclaration("Owner", kind = DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC),
                info = CjoClassInfo(body = listOf(2u))),
            CjoPackageDeclaration("answer", kind = DeclKind.VarDecl, type = 1u, isTopLevel = false,
                attributes = flags(Attribute.PUBLIC, Attribute.STATIC), info = CjoVariableInfo(isVar = false, initializer = 6u)),
            CjoPackageDeclaration("probe", kind = DeclKind.FuncDecl, type = 3u, attributes = flags(Attribute.PUBLIC), info = function(5u)),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), classType(1u), functionType(), CjoTypeMetadata(TypeKind.Invalid)), listOf(
            reference(PACKAGE, 4u, CjoExpressionTargetMetadata(pkgId = PackageIndex.PACKAGE_REFERENCE.value, decl = PACKAGE)),
            CjoExpressionMetadata(ExprKind.MemberAccess, type = 2u, operands = listOf(1u),
                reference = CjoReferenceExpressionMetadata("Owner", declarationTarget(1u))),
            CjoExpressionMetadata(ExprKind.MemberAccess, type = 1u, operands = listOf(2u),
                reference = CjoReferenceExpressionMetadata("answer", declarationTarget(2u))),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 1u, operands = listOf(3u)),
            block(4u),
            integer(),
        )))
        val packageInfo = fixture.wire.allExprs(0)!!.info(ReferenceInfo()) as ReferenceInfo
        val packageTarget = assertNotNull(packageInfo.target)
        assertEquals(-3, packageTarget.pkgId)
        assertEquals(PACKAGE, packageTarget.decl)
        val owner = fixture.classDeclaration("Owner")
        val answer = owner.declarations.filterIsInstance<CfirFieldVariable>().single()
        val function = fixture.topLevelFunction()
        val graph = fixture.graph(function)
        val body = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(function)).payload)
        val returned = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(
            assertIs<org.cangnova.cangjie.cfir.source.CfirSourceSlot.Expression>(body.statements.single()).identity).payload)
        val field = assertIs<CfirSourceExpressionPayload.Member>(graph.expression(returned.value).payload)
        val typeMember = assertIs<CfirSourceExpressionPayload.Member>(graph.expression(field.receiver).payload)
        val packageRecord = graph.expression(typeMember.receiver)
        val qualifier = assertIs<CfirSourceExpressionPayload.Reference>(packageRecord.payload)
        assertEquals(CfirSourceReferenceRole.ORDINARY, qualifier.role)
        assertEquals(FqName(PACKAGE), assertIs<CfirSourceReferenceTarget.Package>(qualifier.target).packageName)
        assertIs<ConeErrorType>(packageRecord.type, "package qualifier 没有值类型，不能填入 Int64")
        assertSame(owner, assertIs<CfirSourceReferenceTarget.Declaration>(typeMember.reference.target).declaration)
        assertSame(answer, assertIs<CfirSourceReferenceTarget.Declaration>(field.reference.target).declaration)
        assertSame(owner, graph.declarationOwners.getValue(answer))
        assertEquals(CfirSourceReferenceRole.ORDINARY, typeMember.reference.role)
        assertEquals(CfirSourceReferenceRole.ORDINARY, field.reference.role)
        assertEquals(ConePrimitiveType.INT64, graph.expression(returned.value).type)
        repeat(3) {
            assertSame(graph, fixture.graph(function))
            assertSame(packageRecord, graph.expression(typeMember.receiver))
            assertSame(owner, fixture.classDeclaration("Owner"))
        }
    }

    @Test
    fun superMemberReceiverRetainsItsDeclarationAndMatchedParentType(@TempDir root: Path) {
        // public open class Parent { public let answer = 7 }
        // public class Child <: Parent { public func probe(): Int64 { return super.answer } }
        val fixture = load(root, metadata(listOf(
            CjoPackageDeclaration("Parent", kind = DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.OPEN),
                info = CjoClassInfo(body = listOf(2u))),
            CjoPackageDeclaration("answer", kind = DeclKind.VarDecl, type = 1u, isTopLevel = false,
                attributes = flags(Attribute.PUBLIC), info = CjoVariableInfo(isVar = false, initializer = 5u)),
            CjoPackageDeclaration("Child", kind = DeclKind.ClassDecl, type = 3u, attributes = flags(Attribute.PUBLIC),
                info = CjoClassInfo(inheritedTypes = listOf(2u), body = listOf(4u))),
            CjoPackageDeclaration("probe", kind = DeclKind.FuncDecl, type = 4u, isTopLevel = false,
                attributes = flags(Attribute.PUBLIC), info = function(4u)),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), classType(1u), classType(3u), functionType()), listOf(
            reference("super", 2u, declarationTarget(1u), parentType = 2u),
            CjoExpressionMetadata(ExprKind.MemberAccess, type = 1u, operands = listOf(1u),
                reference = CjoReferenceExpressionMetadata("answer", declarationTarget(2u), matchedParentType = 2u)),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 1u, operands = listOf(2u)),
            block(3u), integer(),
        )))
        val parent = fixture.classDeclaration("Parent")
        val child = fixture.classDeclaration("Child")
        val function = child.declarations.filterIsInstance<CfirNamedFunction>().single()
        val answer = parent.declarations.filterIsInstance<CfirFieldVariable>().single()
        val graph = fixture.graph(function)
        val body = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(function)).payload)
        val returned = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(
            assertIs<org.cangnova.cangjie.cfir.source.CfirSourceSlot.Expression>(body.statements.single()).identity).payload)
        val field = assertIs<CfirSourceExpressionPayload.Member>(graph.expression(returned.value).payload)
        val receiver = assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(field.receiver).payload)
        assertEquals(CfirSourceReferenceRole.SUPER, receiver.role)
        assertEquals(Name.identifier("super"), receiver.name)
        assertSame(parent, assertIs<CfirSourceReferenceTarget.Declaration>(receiver.target).declaration)
        val type = assertIs<ConeClassLikeType>(assertIs<CfirReferenceParentType.Instantiated>(receiver.matchedParentType).type)
        assertEquals(parent.symbol.classId, type.lookupTag.classId)
        assertSame(answer, assertIs<CfirSourceReferenceTarget.Declaration>(field.reference.target).declaration)
        assertEquals(CfirSourceReferenceRole.ORDINARY, field.reference.role)
        assertSame(child, graph.declarationOwners.getValue(function))
        repeat(3) { assertSame(graph, fixture.graph(function)); assertSame(receiver, graph.expression(field.receiver).payload) }
    }

    /** 正文 FullId 引入签名中没有的 borrowed target；准备 header 不授予该变量 initializer。 */
    @Test
    fun exportPreparationFollowsCanonicalHiddenReferenceWithoutBorrowingInitializer(@TempDir root: Path) {
        val borrowed = metadata(
            listOf(CjoPackageDeclaration("hidden", DeclKind.VarDecl, type = 1u,
                exportId = "r38.borrowed::hidden", attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoVariableInfo(isVar = false, initializer = 1u))),
            listOf(CjoTypeMetadata(TypeKind.Int64)), listOf(integer())
        ).copy(fullPackageName = "r38.borrowed", moduleName = "r38-borrowed")
        CjoPackageWriter.write(root.resolve("r38.borrowed.cjo"), borrowed)
        val seed = metadata(
            listOf(CjoPackageDeclaration("probe", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = function(3u))),
            listOf(CjoTypeMetadata(TypeKind.Int64), functionType()),
            listOf(reference("hidden", 1u, CjoExpressionTargetMetadata(pkgId = 0, decl = "r38.borrowed::hidden")),
                CjoExpressionMetadata(ExprKind.ReturnExpr, type = 1u, operands = listOf(1u)), block(2u))
        ).copy(imports = listOf("r38.borrowed"))
        val fixture = load(root, seed)
        val function = fixture.topLevelFunction()
        val graph = fixture.graph(function)
        val reference = assertIs<CfirSourceExpressionPayload.Reference>(
            graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Reference }.payload)
        val hidden = assertIs<CfirPatternBindingVariable>(assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
        val hiddenRoot = assertIs<CfirPatternVariable>(hidden.patternTreeOwner)
        assertSame(hidden, hiddenRoot.pattern.bindingVariables().single())
        val origin = assertNotNull(hidden.cjoDeclarationOrigin)
        assertEquals(root.resolve("r38.borrowed.cjo").toAbsolutePath().normalize().toString(), origin.packagePath)
        assertEquals(0, origin.declarationIndex)
        assertNull(hidden.initializer)
        val guard = R38ExportPreparationGuard(fixture.module.session, fixture.provider)
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            emptyList(), emptyList(), ::r38PhysicalMembers, canonicalSources = listOf(graph))
        guard.observeOwners(prepared.declarations)
        assertTrue(prepared.declarations.any { it === hidden })
        guard.assertHeaderReady(hidden)
        val input = guard.readOnly {
            org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                emptyList(), emptyList(), ::r38PhysicalMembers, canonicalSources = listOf(graph), preparedInput = prepared)
        }
        guard.readOnly {
            assertNull(hidden.initializer)
            kotlin.test.assertFalse(input.hasCanonicalSourceOf(hidden))
            assertSame(graph, input.canonicalSourceGraphOf(function))
            assertSame(hidden, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
            input.validateSourceUnchanged()
        }
        assertNotNull(fixture.provider.sourceInitializerOf(hidden))
        // 已加载的 borrowed 初值仍不属于此 Analysis 的 SOURCE 内容拥有域。
        kotlin.test.assertFailsWith<IllegalStateException> { input.initializerOf(hidden) }
        guard.readOnly { kotlin.test.assertFailsWith<IllegalStateException> { input.validateSourceUnchanged() } }
    }

    private data class Fixture(val module: Module, val provider: CfirDeserializedSymbolProvider, val wire: Package) {
        fun graph(function: CfirNamedFunction): CfirSourceGraph = assertNotNull((provider as CfirImportedSourceGraphProvider).sourceGraphOf(function))
        fun classDeclaration(name: String): CfirClass = assertIs<CfirClass>(assertNotNull(provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName(PACKAGE + "." + name)))).cfir)
        fun topLevelFunction(): CfirNamedFunction = assertIs<CfirNamedFunction>(
            provider.getTopLevelCallableSymbols(FqName(PACKAGE), Name.identifier("probe")).single().cfir)
    }

    private fun load(root: Path, metadata: CjoPackageMetadata): Fixture {
        val path = root.resolve(PACKAGE + ".cjo")
        Files.write(path, CjoPackageWriter.toByteArray(metadata))
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(root)),
        )
        module.session.register(CfirSymbolProvider::class, provider)
        module.session.register(CfirExtendProvider::class, CfirExtendProviderComposer.fromSymbolProviders(listOf(provider)))
        val loaded = assertNotNull(manager.loadPackageSnapshot(PACKAGE))
        assertEquals(path.toAbsolutePath().normalize(), loaded.sourcePath)
        val wire = loaded.pkg
        assertEquals(metadata.expressions.size, wire.allExprsLength)
        assertEquals(metadata.declarations.size, wire.allDeclsLength)
        return Fixture(module, provider, wire)
    }

    private class Module : CfirModuleData() {
        override val name: Name = Name.identifier("source-reference-forms")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "source-reference-forms"
        override val session: CfirSession = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(TypeComponents::class, TypeComponents(it))
            it.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            it.registerTypeAwareSupertypeComponents()
        }
        init { bindSession(session); session.register(CfirModuleData::class, this) }
    }

    private companion object {
        const val PACKAGE = "referenceforms"
        fun metadata(declarations: List<CjoPackageDeclaration>, types: List<CjoTypeMetadata>, expressions: List<CjoExpressionMetadata>) =
            CjoPackageMetadata(fullPackageName = PACKAGE, moduleName = PACKAGE, schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
                cjoVersion = CjoFormatVersion(0u, 1u, 0u), declarations = declarations, types = types, expressions = expressions)
        fun function(body: UInt): CjoFunctionInfo = CjoFunctionInfo(body = CjoFunctionBodyInfo(
            parameterLists = listOf(emptyList()), desugaredParameterLists = listOf(emptyList()), returnType = 1u, body = body))
        fun functionType(): CjoTypeMetadata = CjoTypeMetadata(TypeKind.Func,
            semanticInfo = CjoFunctionTypeInfoMetadata(returnType = 1u, isC = false, hasVariableLenArg = false))
        fun classType(declaration: UInt): CjoTypeMetadata = CjoTypeMetadata(TypeKind.Class,
            semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration))
        fun declarationTarget(declaration: UInt): CjoExpressionTargetMetadata = CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = declaration)
        fun reference(name: String, type: UInt, target: CjoExpressionTargetMetadata?, parentType: UInt = 0u): CjoExpressionMetadata =
            CjoExpressionMetadata(ExprKind.RefExpr, type = type, reference = CjoReferenceExpressionMetadata(name, target, matchedParentType = parentType))
        fun block(statement: UInt): CjoExpressionMetadata = CjoExpressionMetadata(ExprKind.Block, type = 1u,
            operands = listOf(statement), additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(true, false)))
        fun integer(): CjoExpressionMetadata = CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
            literal = CjoLiteralExpressionMetadata("7", PackageFormat.LitConstKind.Integer))
        fun flags(vararg attributes: Attribute): List<ULong> = MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }
    }
}
