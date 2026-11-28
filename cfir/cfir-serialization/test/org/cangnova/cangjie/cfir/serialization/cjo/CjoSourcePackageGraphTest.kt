@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.BlockInfo
import PackageFormat.ConstValue
import PackageFormat.DeclInfo
import PackageFormat.DeclKind
import PackageFormat.EnumInfo
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FuncBody
import PackageFormat.FuncInfo
import PackageFormat.LitConstKind
import PackageFormat.Package
import PackageFormat.ParamInfo
import PackageFormat.Pattern
import PackageFormat.PatternKind
import PackageFormat.PropInfo
import PackageFormat.ReferenceInfo
import PackageFormat.StructInfo
import PackageFormat.TypeKind
import PackageFormat.VarInfo
import PackageFormat.VarWithPatternInfo
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.registerTypeAwareSupertypeComponents
import org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceImportedPosition
import org.cangnova.cangjie.cfir.source.CfirSourcePatternKind
import org.cangnova.cangjie.cfir.source.CfirSourceSlot
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjSourceLocation
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 正规包 SOURCE 能力：Writer → Manager 的实际 snapshot → Library provider → 完成包图。
 *
 * 依据 build/cjmp-owned-closure-20261120/unified-encoder-{native,kotlin}-map.md 的包产品合同。
 * 固定 native 14276740 的 ASTWriter.cpp:1219–1292、ExprWriter.cpp:689–781、
 * ASTLoader.cpp:704–738：global const 可独立保存 initializer，VarWithPattern 必须由局部根拥有，
 * F header 与 body 槽分离。固定 Kotlin 3c7a504 的 IrDeclarationDeserializer.kt:540–575
 * 和 IrFileDeserializer.kt:251–278：原库表域、真实 binder 和声明/参数身份不可脱离来源。
 * 语义正例使用整数 const、不可反驳 tuple 绑定、普通属性与无参 enum 构造器；
 * body0 只表示正文未保存。这里不调用 internal decoder，不测试尚待集成的 encoder，
 * 也不假设包图将来承载 VarInfo typed values 的字段名称。
 */
class CjoSourcePackageGraphTest {
    /** 包内根本没有 FuncDecl；通过真实 global const 取得其 initializer/type/位置产品。 */
    @Test
    fun globalConstInitializerWithoutAnyFunctionHasACanonicalPackageGraph(@TempDir root: Path) {
        val fixture = fixture(root, constantMetadata("42"))
        assertEquals(listOf("answer"), fixture.rootNames())
        assertFalse((0 until fixture.wire.allDeclsLength).any { fixture.wire.allDecls(it)?.kind == DeclKind.FuncDecl })
        val variable = fixture.variable("answer")
        assertProvenance(variable, fixture.path, 0)
        val graph = assertNotNull(fixture.source.packageSourceGraphOf(variable))
        assertEquals(setOf(variable), graph.initializers.keys)
        assertTrue(graph.functionHeaders.isEmpty())
        assertTrue(graph.functionBodies.isEmpty())
        assertTrue(graph.defaults.isEmpty())
        assertTrue(graph.declarationPatterns.isEmpty())
        assertEquals(1, graph.expressions.size)
        val identity = graph.initializers.getValue(variable)
        val record = graph.expression(identity)
        assertSame(variable, record.context)
        assertSame(variable.returnTypeRef.coneType, record.type)
        assertEquals(ConePrimitiveType.INT64, record.type)
        assertEquals("42", assertIs<CfirSourceExpressionPayload.Literal>(record.payload).text)
        assertEquals(position(2, 30, 32), record.importedPosition)
        assertNull(record.span)
        assertNull(graph.declarationOwners.getValue(variable))
        repeat(3) {
            assertSame(variable, fixture.variable("answer"))
            assertSame(variable.symbol, fixture.variable("answer").symbol)
            assertSame(graph, fixture.source.packageSourceGraphOf(variable))
            assertSame(identity, graph.initializers.getValue(variable))
            assertSame(record, graph.expression(identity))
        }
    }

    /** const func bind(): Int64 { let (bound, _) = (7, 9); return bound } 的真实 mixed Block。 */
    @Test
    fun localTuplePatternKeepsItsBindingSymbolAndInitializerInThePackageGraph(@TempDir root: Path) {
        val fixture = fixture(root, patternMetadata())
        assertEquals(listOf("bind"), fixture.rootNames())
        val function = fixture.function("bind")
        val header = assertNotNull(fixture.source.sourceHeaderOf(function))
        assertTrue(header.bodyAvailable)
        val graph = assertNotNull(fixture.source.packageSourceGraphOf(function))
        val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(function)).payload)
        assertEquals(2, block.statements.size)
        val variable = assertIs<CfirPatternVariable>(assertIs<CfirSourceSlot.Declaration>(block.statements[0]).declaration)
        assertTrue(variable.isLocal)
        assertProvenance(variable, fixture.path, 1)
        assertSame(function, graph.declarationOwners.getValue(variable))
        val normal = assertIs<CfirTuplePattern>(variable.pattern)
        assertEquals(2, normal.elements.size)
        val binding = assertNotNull(assertIs<CfirBindingPattern>(normal.elements[0]).bindingVariable)
        val pattern = graph.declarationPatterns.getValue(variable)
        assertEquals(CfirSourcePatternKind.TUPLE, pattern.kind)
        assertTrue(pattern.slots.isEmpty())
        assertEquals(2, pattern.patterns.size)
        assertEquals(assertIs<ConeTupleType>(variable.returnTypeRef.coneType), pattern.types.single())
        assertEquals(position(3, 9, 19), pattern.importedPosition)
        val boundPattern = pattern.patterns[0]
        assertEquals(CfirSourcePatternKind.VARIABLE, boundPattern.kind)
        assertEquals(listOf(ConePrimitiveType.INT64), boundPattern.types)
        assertEquals(position(3, 10, 15), boundPattern.importedPosition)
        val sourceBinding = assertIs<CfirPatternBindingVariable>(assertIs<CfirSourceSlot.Declaration>(boundPattern.slots.single()).declaration)
        assertSame(binding, sourceBinding)
        assertSame(binding.symbol, sourceBinding.symbol)
        assertSame(sourceBinding, sourceBinding.symbol.cfir)
        assertTrue(sourceBinding.isLocal)
        assertTrue(sourceBinding.symbol.callableId.isLocal)
        assertProvenance(sourceBinding, fixture.path, 2)
        assertEquals(CfirSourcePatternKind.WILDCARD, pattern.patterns[1].kind)
        assertTrue(pattern.patterns[1].slots.isEmpty())
        assertTrue(pattern.constantValues.isEmpty())
        val initializer = graph.initializers.getValue(variable)
        val record = graph.expression(initializer)
        assertSame(variable, record.context)
        assertEquals(variable.returnTypeRef.coneType, record.type)
        assertEquals(position(3, 22, 28), record.importedPosition)
        val tuple = assertIs<CfirSourceExpressionPayload.Tuple>(record.payload)
        assertEquals(listOf("7", "9"), tuple.elements.map { assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(it).payload).text })
        assertNotSame(tuple.elements[0], tuple.elements[1])
        val returned = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(assertIs<CfirSourceSlot.Expression>(block.statements[1]).identity).payload)
        assertSame(function, returned.function)
        assertSame(sourceBinding, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(returned.value).payload).target).declaration)
        assertEquals(header, graph.functionHeaders.getValue(function))
        assertTrue(fixture.provider.getTopLevelCallableSymbols(FqName(fixture.packageName), Name.identifier("bound")).isEmpty())
        repeat(3) {
            for (declaration in listOf(function, variable, sourceBinding)) {
                assertSame(graph, fixture.source.packageSourceGraphOf(declaration))
            }
            assertSame(graph, fixture.source.packageSourceGraphOf(function))
            assertSame(pattern, graph.declarationPatterns.getValue(variable))
            assertSame(record, graph.expression(initializer))
            assertSame(sourceBinding.symbol, binding.symbol)
        }
        assertNull(function.body)
    }

    /** 两个真实 named F 全部 body0；空组、零 helper、原 retType0 与参数身份仍保留。 */
    @Test
    fun bodylessNamedFunctionsExposePackageHeadersWithoutInventingBodies(@TempDir root: Path) {
        for (headerFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(headerFirst.toString()), headersMetadata())
            assertEquals(listOf("header", "second"), fixture.rootNames())
            val function = fixture.function("header")
            val second = fixture.function("second")
            val before = if (headerFirst) assertNotNull(fixture.source.sourceHeaderOf(function)) else null
            assertNull(fixture.source.sourceGraphOf(function))
            assertNull(fixture.source.sourceGraphOf(second))
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(function))
            assertEquals(setOf(function, second), graph.functionHeaders.keys)
            assertTrue(graph.functionBodies.isEmpty())
            assertTrue(graph.expressions.isEmpty())
            assertTrue(graph.initializers.isEmpty())
            val header = graph.functionHeaders.getValue(function)
            if (before != null) assertEquals(before, header)
            assertFalse(header.bodyAvailable)
            assertNull(header.returnType)
            assertEquals(ConePrimitiveType.INT64, function.returnTypeRef.coneType)
            assertEquals(listOf(0, 1), header.parameterLists.map { it.size })
            assertEquals(listOf(0, 1), header.desugaredParameterLists.map { it.size })
            assertNull(header.desugaredParameterLists[1].single())
            val parameter = function.valueParameters.single()
            assertSame(parameter, header.parameterLists[1].single())
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
            assertProvenance(parameter, fixture.path, 1)
            assertFalse(graph.functionHeaders.getValue(second).bodyAvailable)
            assertEquals(ConePrimitiveType.INT64, graph.functionHeaders.getValue(second).returnType)
            repeat(3) {
                for (declaration in listOf(function, second, parameter)) {
                    assertSame(graph, fixture.source.packageSourceGraphOf(declaration))
                }
                assertNull(fixture.source.sourceGraphOf(function))
                assertNull(fixture.source.sourceGraphOf(second))
                assertEquals(header, fixture.source.sourceHeaderOf(function))
                assertSame(parameter, assertNotNull(fixture.source.sourceHeaderOf(function)).parameterLists[1].single())
            }
            assertNull(function.body)
            assertNull(second.body)
            assertNull(fixture.source.sourceBodyOf(function))
            assertNull(fixture.source.sourceBodyOf(second))
        }
    }

    /** enum State { Ready } 的无参构造器来自 VarDecl，真实 CFIR function 并没有 wire F。 */
    @Test
    fun aRealEnumConstructorWithoutAnFHasNoHeaderButItsPackageIsReadable(@TempDir root: Path) {
        val fixture = fixture(root, enumMetadata())
        val owner = assertIs<CfirEnum>(assertNotNull(fixture.provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("${fixture.packageName}.State")))).cfir)
        val constructor = owner.declarations.filterIsInstance<CfirEnumConstructor>().single()
        assertEquals("Ready", constructor.name.asString())
        assertProvenance(constructor, fixture.path, 1)
        assertEquals(DeclKind.VarDecl, assertNotNull(fixture.wire.allDecls(1)).kind)
        val graph = assertNotNull(fixture.source.packageSourceGraphOf(constructor))
        assertTrue(graph.functionHeaders.isEmpty())
        assertTrue(graph.functionBodies.isEmpty())
        assertTrue(graph.expressions.isEmpty())
        assertSame(owner, graph.declarationOwners.getValue(constructor))
        assertNull(fixture.source.sourceBodyOf(constructor))
        repeat(3) {
            assertSame(graph, fixture.source.packageSourceGraphOf(owner))
            assertSame(graph, fixture.source.packageSourceGraphOf(constructor))
            assertSame(constructor, owner.declarations.filterIsInstance<CfirEnumConstructor>().single())
        }
    }

    /** 任意真实 global/member/property/parameter 首次请求都定位同一包；无正文入口函数。 */
    @Test
    fun globalMemberPropertyAndParameterQueriesShareTheSamePackageGraph(@TempDir root: Path) {
        for (first in 0..3) {
            val fixture = fixture(root.resolve(first.toString()), ownersMetadata())
            assertEquals(listOf("answer", "Holder", "api"), fixture.rootNames())
            val global = fixture.variable("answer")
            val owner = fixture.struct("Holder")
            val member = owner.declarations.filterIsInstance<CfirFieldVariable>().single()
            val property = owner.declarations.filterIsInstance<CfirProperty>().single()
            val getter = assertNotNull(property.getter)
            val api = fixture.function("api")
            val parameter = api.valueParameters.single()
            val declarations = listOf(global, member, property, parameter)
            listOf(0, 2, 3, 7).zip(declarations).forEach { (index, declaration) -> assertProvenance(declaration, fixture.path, index) }
            val header = assertNotNull(fixture.source.sourceHeaderOf(getter))
            assertFalse(header.bodyAvailable)
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(declarations[first]))
            assertEquals(setOf(global), graph.initializers.keys)
            assertEquals("42", literal(graph, global))
            assertSame(owner, graph.declarationOwners.getValue(member))
            assertSame(owner, graph.declarationOwners.getValue(property))
            assertSame(property, graph.declarationOwners.getValue(getter))
            assertSame(api, graph.declarationOwners.getValue(parameter))
            assertSame(property.symbol, getter.propertySymbol)
            assertSame(api.symbol, parameter.containingDeclarationSymbol)
            assertSame(parameter, graph.functionHeaders.getValue(api).parameterLists.single().single())
            assertEquals(header, graph.functionHeaders.getValue(getter))
            assertTrue(graph.functionBodies.isEmpty())
            repeat(3) {
                for (declaration in declarations + listOf(owner, api, getter)) {
                    assertSame(graph, fixture.source.packageSourceGraphOf(declaration))
                }
                assertSame(global, fixture.variable("answer"))
                assertSame(owner, fixture.struct("Holder"))
                assertSame(property, fixture.struct("Holder").declarations.filterIsInstance<CfirProperty>().single())
                assertEquals(header, fixture.source.sourceHeaderOf(getter))
                assertNull(fixture.source.sourceGraphOf(getter))
                assertNull(fixture.source.sourceGraphOf(api))
            }
        }
    }

    /** 相同包名与 d0 在不同搜索路径/Manager/session 中独立，不按裸 ordinal 合并。 */
    @Test
    fun identicalRawIndicesInDifferentPathsKeepTheirOwnedPackageGraphs(@TempDir root: Path) {
        val left = fixture(root.resolve("left"), constantMetadata("7"))
        val right = fixture(root.resolve("right"), constantMetadata("9"))
        assertNotSame(left.manager, right.manager)
        assertNotSame(left.module.session, right.module.session)
        assertTrue(left.path != right.path)
        assertIsolated(left, right, "7", "9")
    }

    /** 同路径、同字节但独立 Manager 的实际 snapshots 仍属于两个 provider。 */
    @Test
    fun independentManagersReadingTheSamePathRejectEachOthersDeclarations(@TempDir root: Path) {
        val left = fixture(root, constantMetadata("7"))
        val right = attach(root, left.input, manager(root), left.originalWire)
        assertEquals(left.path, right.path)
        assertNotSame(left.snapshot, right.snapshot)
        assertIsolated(left, right, "7", "7")
    }

    /** 连实际 snapshot 都相同，两个 Library binder 的 canonical declaration 仍不能混用。 */
    @Test
    fun differentLibrarySessionsSharingAManagerRejectForeignCanonicalDeclarations(@TempDir root: Path) {
        val left = fixture(root, constantMetadata("7"))
        val right = attach(root, left.input, left.manager, left.originalWire)
        assertSame(left.manager, right.manager)
        assertSame(left.snapshot, right.snapshot)
        assertNotSame(left.module.session, right.module.session)
        assertIsolated(left, right, "7", "7")
    }

    /** 旧代尚未请求图时替换磁盘；旧 provider 仍从原 loaded snapshot 完成原 initializer。 */
    @Test
    fun replacingAFileAtTheSamePathDoesNotChangeTheOriginalSnapshotGraph(@TempDir root: Path) {
        val old = fixture(root, constantMetadata("7"))
        val oldVariable = old.variable("answer")
        val oldSnapshot = old.snapshot
        val replacement = fixture(root, constantMetadata("9"))
        assertEquals(old.path, replacement.path)
        assertNotSame(oldSnapshot, replacement.snapshot)
        assertSame(oldSnapshot, old.manager.loadedPackageSnapshot(old.path))
        assertSame(oldVariable, old.variable("answer"))
        assertIsolated(old, replacement, "7", "9")
        assertEquals("7", literal(assertNotNull(old.source.packageSourceGraphOf(oldVariable)), oldVariable))
    }

    /** 合法 writer 文件后仅破坏已选 F 的必需 body e 引用；包图失败不等于缺 SOURCE。 */
    @Test
    fun invalidSelectedBodyFailsPackageQueriesAndPreservesTheCompletedHeader(@TempDir root: Path) {
        val fixture = fixture(root, bodyMetadata(), mutateBodyAt = 0)
        val function = fixture.function("read")
        val global = fixture.variable("answer")
        val header = assertNotNull(fixture.source.sourceHeaderOf(function))
        val parameter = function.valueParameters.single()
        assertTrue(header.bodyAvailable)
        assertSame(parameter, header.parameterLists.single().single())
        val originalBody = assertNotNull(assertIs<FuncInfo>(assertNotNull(fixture.originalWire.allDecls(0)).info(FuncInfo())).funcBody)
        assertEquals(1u, originalBody.body)
        assertEquals(ExprKind.Block, assertNotNull(fixture.originalWire.allExprs(0)).kind)
        val invalid = (fixture.wire.allExprsLength + 1).toUInt()
        assertEquals(invalid, assertNotNull(assertIs<FuncInfo>(assertNotNull(fixture.wire.allDecls(0)).info(FuncInfo())).funcBody).body)
        repeat(3) {
            for (declaration in listOf<CfirDeclaration>(global, parameter, function)) {
                val failure = assertFailsWith<IllegalArgumentException> { fixture.source.packageSourceGraphOf(declaration) }
                assertTrue(assertNotNull(failure.message).contains("Invalid SOURCE e reference $invalid"))
                assertSame(function, fixture.function("read"))
                assertSame(parameter, function.valueParameters.single())
                assertSame(header, fixture.source.sourceHeaderOf(function))
                assertSame(function.symbol, parameter.containingDeclarationSymbol)
                assertNull(function.body)
            }
            assertFailsWith<IllegalArgumentException> { fixture.source.sourceGraphOf(function) }
            assertSame(header, fixture.source.sourceHeaderOf(function))
            assertWireStructure(fixture.input, fixture.originalWire)
        }
    }

    /** 先尝试跨 provider，随后本地完成，再重复交叉请求；拒绝不应污染任何一边的完成图。 */
    private fun assertIsolated(left: Fixture, right: Fixture, leftText: String, rightText: String) {
        val a = left.variable("answer")
        val b = right.variable("answer")
        assertEquals(left.packageName, right.packageName)
        assertProvenance(a, left.path, 0)
        assertProvenance(b, right.path, 0)
        assertNotSame(a, b)
        assertNotSame(a.symbol, b.symbol)
        assertNull(left.source.packageSourceGraphOf(b))
        assertNull(right.source.packageSourceGraphOf(a))
        val first = assertNotNull(left.source.packageSourceGraphOf(a))
        val second = assertNotNull(right.source.packageSourceGraphOf(b))
        assertNotSame(first, second)
        assertEquals(setOf(a), first.initializers.keys)
        assertEquals(setOf(b), second.initializers.keys)
        val firstIdentity = first.initializers.getValue(a)
        val secondIdentity = second.initializers.getValue(b)
        assertNotSame(firstIdentity, secondIdentity)
        val firstRecord = first.expression(firstIdentity)
        val secondRecord = second.expression(secondIdentity)
        assertNotSame(firstRecord, secondRecord)
        repeat(3) {
            assertNull(left.source.packageSourceGraphOf(b))
            assertNull(right.source.packageSourceGraphOf(a))
            assertSame(first, left.source.packageSourceGraphOf(a))
            assertSame(second, right.source.packageSourceGraphOf(b))
            assertSame(firstRecord, first.expression(firstIdentity))
            assertSame(secondRecord, second.expression(secondIdentity))
            assertSame(a, firstRecord.context)
            assertSame(b, secondRecord.context)
            assertEquals(leftText, literal(first, a))
            assertEquals(rightText, literal(second, b))
            assertSame(left.snapshot, left.manager.loadedPackageSnapshot(left.path))
            assertSame(right.snapshot, right.manager.loadedPackageSnapshot(right.path))
        }
    }

    private fun literal(graph: CfirSourceGraph, variable: CfirFieldVariable): String =
        assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(graph.initializers.getValue(variable)).payload).text

    private fun assertProvenance(declaration: CfirDeclaration, path: Path, index: Int) {
        val origin = assertNotNull(declaration.cjoDeclarationOrigin)
        assertEquals(path, Path.of(origin.packagePath).toAbsolutePath().normalize())
        assertEquals(index, origin.declarationIndex)
        assertSame(declaration, declaration.symbol.cfir)
    }

    private fun position(line: Int, begin: Int, end: Int): CfirSourceImportedPosition =
        CfirSourceImportedPosition(CjSourceLocation(SOURCE_FILE, line, begin), CjSourceLocation(SOURCE_FILE, line, end))

    private fun rawPosition(line: Int, column: Int): CjoPositionMetadata = CjoPositionMetadata(file = 1u, line = line, column = column)

    /** public const answer: Int64 = 42；无 value union，SOURCE literal 仍属于 e。 */
    private fun constantMetadata(value: String): CjoPackageMetadata = metadata("packageconstant", listOf(
        globalConstant(1u),
    ), listOf(CjoTypeMetadata(TypeKind.Int64)), listOf(integer(value)))

    private fun globalConstant(initializer: UInt): CjoPackageDeclaration = CjoPackageDeclaration(
        "answer", DeclKind.VarDecl, type = 1u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
        info = CjoVariableInfo(isConst = true, initializer = initializer))

    /** d1 F 拥有 e1 Block；e1 的 d2 槽拥有 tuple P，P 的子槽拥有 d3 binding。 */
    private fun patternMetadata(): CjoPackageMetadata = metadata("packagepattern", listOf(
        CjoPackageDeclaration("bind", type = 4u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(isConst = true, body = body(1u, 1u))),
        CjoPackageDeclaration("", DeclKind.VarWithPatternDecl, isTopLevel = false, type = 3u,
            info = CjoVarWithPatternInfo(initializer = 2u, irrefutablePattern = CjoPatternMetadata(
                PatternKind.TuplePattern, begin = rawPosition(3, 9), end = rawPosition(3, 19), types = listOf(3u),
                patterns = listOf(
                    CjoPatternMetadata(PatternKind.VarPattern, begin = rawPosition(3, 10), end = rawPosition(3, 15), types = listOf(1u), exprs = listOf(3u)),
                    CjoPatternMetadata(PatternKind.WildcardPattern, begin = rawPosition(3, 17), end = rawPosition(3, 18), types = listOf(1u)),
                )))),
        CjoPackageDeclaration("bound", DeclKind.VarDecl, isTopLevel = false, type = 1u, info = CjoVariableInfo()),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing),
        CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 1u)), functionType(emptyList(), 1u)), listOf(
        block(2u, listOf(2u, 5u), listOf(false, true)), // e1：d2、e5，原槽序不变
        CjoExpressionMetadata(ExprKind.TupleLit, begin = rawPosition(3, 22), end = rawPosition(3, 28), type = 3u, operands = listOf(3u, 4u)),
        integer("7", 3, 23), integer("9", 3, 26),
        CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(6u)),
        reference("bound", 3u),
    ))

    private fun headersMetadata(): CjoPackageMetadata = metadata("packageheaders", listOf(
        CjoPackageDeclaration("header", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                parameterLists = listOf(emptyList(), listOf(2u)), desugaredParameterLists = listOf(emptyList(), listOf(0u)), returnType = 0u))),
        parameter("value"),
        CjoPackageDeclaration("second", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = body(0u, 1u))),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(listOf(1u), 1u), functionType(emptyList(), 1u)))

    private fun enumMetadata(): CjoPackageMetadata = metadata("packageenum", listOf(
        CjoPackageDeclaration("State", DeclKind.EnumDecl, type = 1u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoEnumInfo(body = listOf(2u))),
        CjoPackageDeclaration("Ready", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC, Attribute.ENUM_CONSTRUCTOR), info = CjoVariableInfo()),
    ), listOf(CjoTypeMetadata(TypeKind.Enum, semanticInfo = CjoCompositeTypeInfoMetadata(1u))))

    /** 源序根 d1/d2/d7；struct body 拥有 d3/d4/d6，property 拥有 d5，api F 拥有 d8。 */
    private fun ownersMetadata(): CjoPackageMetadata = metadata("packageowners", listOf(
        globalConstant(1u),
        CjoPackageDeclaration("Holder", DeclKind.StructDecl, type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoStructInfo(body = listOf(3u, 4u, 6u))),
        CjoPackageDeclaration("storage", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC), info = CjoVariableInfo()),
        CjoPackageDeclaration("value", DeclKind.PropDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC), info = CjoPropertyInfo(getters = listOf(5u))),
        CjoPackageDeclaration("value", isTopLevel = false, type = 4u,
            attributes = flags(Attribute.PUBLIC), info = CjoFunctionInfo(body = body(0u, 1u))),
        CjoPackageDeclaration("init", isTopLevel = false, type = 5u,
            attributes = flags(Attribute.PUBLIC, Attribute.CONSTRUCTOR), info = CjoFunctionInfo(body = body(0u, 2u))),
        CjoPackageDeclaration("api", type = 6u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = body(0u, 1u, listOf(8u)))),
        parameter("argument"),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Unit),
        CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(2u)),
        functionType(emptyList(), 1u), functionType(emptyList(), 2u), functionType(listOf(1u), 1u)), listOf(integer("42")))

    /** const func read(value: Int64): Int64 { return value }；另有独立 global const 根。 */
    private fun bodyMetadata(): CjoPackageMetadata = metadata("packagefailure", listOf(
        CjoPackageDeclaration("read", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(isConst = true, body = body(1u, 1u, listOf(2u)))),
        parameter("value"), globalConstant(4u),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing), functionType(listOf(1u), 1u)), listOf(
        block(2u, listOf(2u), listOf(true)),
        CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(3u)),
        reference("value", 2u), integer("42"),
    ))

    private fun parameter(name: String): CjoPackageDeclaration = CjoPackageDeclaration(
        name, DeclKind.FuncParam, isTopLevel = false, type = 1u, info = CjoParameterInfo(isNamed = false, isMember = false))

    private fun body(expression: UInt, result: UInt, parameters: List<UInt> = emptyList()): CjoFunctionBodyInfo =
        CjoFunctionBodyInfo(listOf(parameters), listOf(List(parameters.size) { 0u }), returnType = result, body = expression)

    private fun functionType(parameters: List<UInt>, result: UInt): CjoTypeMetadata =
        CjoTypeMetadata(TypeKind.Func, parameters, semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))

    private fun integer(text: String, line: Int = 2, column: Int = 30): CjoExpressionMetadata = CjoExpressionMetadata(
        ExprKind.LitConstExpr, begin = rawPosition(line, column), end = rawPosition(line, column + text.length), type = 1u,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.Integer))

    private fun reference(name: String, declaration: UInt): CjoExpressionMetadata = CjoExpressionMetadata(
        ExprKind.RefExpr, type = 1u, reference = CjoReferenceExpressionMetadata(name,
            CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = declaration)))

    private fun block(type: UInt, statements: List<UInt>, roles: List<Boolean>): CjoExpressionMetadata =
        CjoExpressionMetadata(ExprKind.Block, type = type, operands = statements,
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(roles + false))

    private fun metadata(name: String, declarations: List<CjoPackageDeclaration>, types: List<CjoTypeMetadata>,
                         expressions: List<CjoExpressionMetadata> = emptyList()): CjoPackageMetadata = CjoPackageMetadata(
        fullPackageName = name, moduleName = name, schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
        cjoVersion = CjoFormatVersion(0u, 1u, 0u), allFiles = listOf(SOURCE_FILE),
        fileImports = listOf(CjoPackageFileImports(emptyList())), declarations = declarations, types = types, expressions = expressions)

    private data class Fixture(
        val input: CjoPackageMetadata,
        val path: Path,
        val module: Module,
        val manager: CjoManager,
        val provider: CfirDeserializedSymbolProvider,
        val snapshot: CjoLoadedPackage,
        val originalWire: Package,
    ) {
        val packageName: String get() = input.fullPackageName
        val wire: Package get() = snapshot.pkg
        val source: CfirImportedSourceGraphProvider get() = provider
        fun function(name: String): CfirNamedFunction = assertIs<CfirNamedFunction>(callable(name))
        fun variable(name: String): CfirFieldVariable = assertIs<CfirFieldVariable>(callable(name))
        fun callable(name: String): CfirDeclaration = provider.getTopLevelCallableSymbols(FqName(packageName), Name.identifier(name)).single().cfir
        fun struct(name: String): CfirStruct = assertIs<CfirStruct>(assertNotNull(provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("$packageName.$name")))).cfir)
        fun rootNames(): List<String?> = (0 until wire.allDeclsLength).map { assertNotNull(wire.allDecls(it)) }
            .filter { it.isTopLevel }.map { it.identifier }
    }

    /** 正式 writer 生成并核验合法输入；负例只用 generated mutateBody 改一个非零已选槽。 */
    private fun fixture(root: Path, input: CjoPackageMetadata, mutateBodyAt: Int? = null): Fixture {
        Files.createDirectories(root)
        val bytes = CjoPackageWriter.toByteArray(input)
        val originalWire = Package.getRootAsPackage(ByteBuffer.wrap(bytes.copyOf()))
        assertWireStructure(input, originalWire)
        if (mutateBodyAt != null) {
            val wire = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
            val body = assertNotNull(assertIs<FuncInfo>(assertNotNull(wire.allDecls(mutateBodyAt)).info(FuncInfo())).funcBody)
            assertTrue(body.body != 0u)
            assertEquals(ExprKind.Block, assertNotNull(wire.allExprs(body.body.toInt() - 1)).kind)
            assertTrue(body.mutateBody((wire.allExprsLength + 1).toUInt()))
        }
        Files.write(root.resolve(input.fullPackageName + ".cjo"), bytes)
        return attach(root, input, manager(root), originalWire)
    }

    /** attach 只为真实已写文件建立正规 Library/provider；共享 Manager 用于独立 binder 身份测试。 */
    private fun attach(root: Path, input: CjoPackageMetadata, manager: CjoManager, originalWire: Package): Fixture {
        val path = root.resolve(input.fullPackageName + ".cjo").toAbsolutePath().normalize()
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.registerProvider(provider)
        val snapshot = assertNotNull(manager.loadPackageSnapshot(input.fullPackageName))
        assertEquals(path, snapshot.sourcePath)
        assertSame(snapshot, manager.loadedPackageSnapshot(path))
        return Fixture(input, path, module, manager, provider, snapshot, originalWire)
    }

    private fun manager(root: Path): CjoManager = CjoManager(
        CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
        CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))

    /** 消费前核验真实 d/e/t 和各域槽序；不进行语义解码，不把表内 local 当作顶层根。 */
    private fun assertWireStructure(input: CjoPackageMetadata, wire: Package) {
        assertEquals(input.declarations.size, wire.allDeclsLength)
        assertEquals(input.types.size, wire.allTypesLength)
        assertEquals(input.expressions.size, wire.allExprsLength)
        assertEquals(0, wire.allValuesLength)
        assertEquals(input.declarations.filter { it.isTopLevel }.map { it.identifier },
            (0 until wire.allDeclsLength).map { assertNotNull(wire.allDecls(it)) }.filter { it.isTopLevel }.map { it.identifier })
        fun validType(reference: UInt) { assertTrue(reference in 1u..wire.allTypesLength.toUInt()) }
        fun validDeclaration(reference: UInt) { assertTrue(reference in 1u..wire.allDeclsLength.toUInt()) }
        fun validExpression(reference: UInt) { assertTrue(reference in 1u..wire.allExprsLength.toUInt()) }
        fun assertBody(expected: CjoFunctionBodyInfo, actual: FuncBody) {
            assertEquals(expected.body, actual.body)
            if (expected.body != 0u) {
                validExpression(actual.body)
                assertEquals(ExprKind.Block, assertNotNull(wire.allExprs(actual.body.toInt() - 1)).kind)
            }
            assertEquals(expected.returnType, actual.retType)
            if (actual.retType != 0u) validType(actual.retType)
            assertEquals(expected.parameterLists.size, actual.paramListsLength)
            expected.parameterLists.forEachIndexed { index, parameters ->
                val list = assertNotNull(actual.paramLists(index))
                assertEquals(parameters, (0 until list.paramsLength).map { list.params(it) })
                assertEquals(expected.desugaredParameterLists[index], (0 until list.desugarsLength).map { list.desugars(it) })
                parameters.forEach { validDeclaration(it); assertEquals(DeclKind.FuncParam, assertNotNull(wire.allDecls(it.toInt() - 1)).kind) }
            }
        }
        fun assertPattern(expected: CjoPatternMetadata, actual: Pattern) {
            assertEquals(expected.kind, actual.kind)
            assertEquals(expected.types, (0 until actual.typesLength).map { actual.types(it) })
            expected.types.forEach(::validType)
            assertEquals(expected.exprs, (0 until actual.exprsLength).map { actual.exprs(it) })
            if (expected.kind == PatternKind.VarPattern) expected.exprs.forEach(::validDeclaration)
            else expected.exprs.forEach(::validExpression)
            assertEquals(expected.patterns.size, actual.patternsLength)
            expected.patterns.forEachIndexed { index, nested -> assertPattern(nested, assertNotNull(actual.patterns(index))) }
        }
        input.types.forEachIndexed { index, expected ->
            assertEquals(expected.kind, assertNotNull(wire.allTypes(index)).kind)
            expected.typeArguments.forEach(::validType)
        }
        input.declarations.forEachIndexed { index, expected ->
            val declaration = assertNotNull(wire.allDecls(index))
            assertEquals(expected.kind, declaration.kind)
            assertEquals(expected.isTopLevel, declaration.isTopLevel)
            validType(declaration.type)
            when (val info = assertNotNull(expected.info)) {
                is CjoFunctionInfo -> {
                    assertEquals(DeclInfo.FuncInfo, declaration.infoType)
                    assertBody(info.body, assertNotNull(assertIs<FuncInfo>(declaration.info(FuncInfo())).funcBody))
                }
                is CjoVariableInfo -> {
                    assertEquals(DeclInfo.VarInfo, declaration.infoType)
                    val actual = assertIs<VarInfo>(declaration.info(VarInfo()))
                    assertEquals(info.initializer, actual.initializer)
                    assertEquals(info.isConst, actual.isConst)
                    assertEquals(ConstValue.NONE, actual.valueType)
                    if (actual.initializer != 0u) validExpression(actual.initializer)
                }
                is CjoVarWithPatternInfo -> {
                    assertFalse(declaration.isTopLevel)
                    assertEquals(DeclInfo.VarWithPatternInfo, declaration.infoType)
                    val actual = assertIs<VarWithPatternInfo>(declaration.info(VarWithPatternInfo()))
                    assertEquals(info.initializer, actual.initializer)
                    validExpression(actual.initializer)
                    assertPattern(info.irrefutablePattern, assertNotNull(actual.irrefutablePattern))
                }
                is CjoParameterInfo -> {
                    assertEquals(DeclInfo.ParamInfo, declaration.infoType)
                    assertEquals(0u, assertIs<ParamInfo>(declaration.info(ParamInfo())).defaultVal)
                }
                is CjoStructInfo -> {
                    val actual = assertIs<StructInfo>(declaration.info(StructInfo()))
                    assertEquals(info.body, (0 until actual.bodyLength).map { actual.body(it) })
                    info.body.forEach(::validDeclaration)
                }
                is CjoEnumInfo -> {
                    val actual = assertIs<EnumInfo>(declaration.info(EnumInfo()))
                    assertEquals(info.body, (0 until actual.bodyLength).map { actual.body(it) })
                    info.body.forEach(::validDeclaration)
                }
                is CjoPropertyInfo -> {
                    val actual = assertIs<PropInfo>(declaration.info(PropInfo()))
                    assertEquals(info.getters, (0 until actual.gettersLength).map { actual.getters(it) })
                    assertEquals(info.setters, (0 until actual.settersLength).map { actual.setters(it) })
                    (info.getters + info.setters).forEach(::validDeclaration)
                }
                else -> error("This fixture must explicitly check its declaration union")
            }
        }
        input.expressions.forEachIndexed { index, expected ->
            val expression = assertNotNull(wire.allExprs(index))
            assertEquals(expected.kind, expression.kind)
            validType(expression.type)
            assertEquals(expected.operands, (0 until expression.operandsLength).map { expression.operands(it) })
            when (expected.kind) {
                ExprKind.Block -> {
                    assertEquals(ExprInfo.BlockInfo, expression.infoType)
                    val actual = assertIs<BlockInfo>(expression.info(BlockInfo()))
                    val roles = assertIs<CjoAdditionalExpressionInfoMetadata.Block>(expected.additionalInfo).expressions
                    assertEquals(roles, (0 until actual.isExprLength).map { actual.isExpr(it) })
                    assertEquals(expression.operandsLength + 1, actual.isExprLength)
                    assertFalse(actual.isExpr(expression.operandsLength))
                    expected.operands.forEachIndexed { slot, reference ->
                        if (actual.isExpr(slot)) validExpression(reference) else validDeclaration(reference)
                    }
                }
                ExprKind.LitConstExpr -> assertEquals(ExprInfo.LitConstInfo, expression.infoType)
                ExprKind.TupleLit, ExprKind.ReturnExpr -> {
                    assertEquals(ExprInfo.NONE, expression.infoType)
                    expected.operands.forEach(::validExpression)
                    if (expected.kind == ExprKind.ReturnExpr) assertEquals(1, expression.operandsLength)
                }
                ExprKind.RefExpr -> {
                    assertEquals(ExprInfo.ReferenceInfo, expression.infoType)
                    val actual = assertNotNull(assertIs<ReferenceInfo>(expression.info(ReferenceInfo())).target)
                    assertEquals(PackageIndex.CURRENT.value, actual.pkgId)
                    assertEquals(assertNotNull(expected.reference?.target).index, actual.index)
                    validDeclaration(actual.index)
                }
                else -> error("This fixture must explicitly check its expression union")
            }
        }
    }

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }

    /** 与既有 TypedConstantGraphTest 同样的真实 Library session、类型组件及 symbol/extend provider。 */
    private class Module : CfirModuleData() {
        override val name: Name = Name.identifier("cjo-source-package-graph-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = "cjo-source-package-graph-test"
        override val session: CfirSession = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(TypeComponents::class, TypeComponents(it))
            it.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            it.registerTypeAwareSupertypeComponents()
        }
        init { bindSession(session) }

        fun registerProvider(provider: CfirSymbolProvider) {
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirExtendProvider::class, CfirExtendProviderComposer.fromSymbolProviders(listOf(provider)))
        }
    }

    private companion object {
        const val SOURCE_FILE: String = "source-package.cj"
    }
}
