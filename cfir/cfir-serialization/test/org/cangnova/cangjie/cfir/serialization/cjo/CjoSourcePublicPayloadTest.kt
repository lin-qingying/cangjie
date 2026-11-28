@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import PackageFormat.DeclKind
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.LitConstInfo
import PackageFormat.LitConstKind
import PackageFormat.MatchCaseInfo
import PackageFormat.Package
import PackageFormat.PatternKind
import PackageFormat.StringKind
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceAbsence
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceRole
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader
import org.cangnova.cangjie.cfir.source.CfirSourceImportedPosition
import org.cangnova.cangjie.cfir.source.CfirSourcePatternKind
import org.cangnova.cangjie.cfir.source.CfirSourceSlot
import org.cangnova.cangjie.cfir.source.CfirSourceStringKind
import org.cangnova.cangjie.cfir.source.expressionChildren
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjSourceLocation
import org.junit.jupiter.api.io.TempDir
import java.math.BigInteger
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
 * mainS5 F7/F8/F9：正式 Writer → Manager → library session/provider → 公共 SOURCE 产品。
 *
 * 固定 native 14276740 的 FuncBody、StringKind、MatchCase.ctxExpr 合同；
 * Kotlin 3c7a504 的签名/正文读取边界。此处不直接调用 internal decoder 或提供测试解析器。
 */
class CjoSourcePublicPayloadTest {
    /** body=0 仅表示正文未保存，不会抹掉参数分组、对齐 helper 槽及 retType=0。 */
    @Test
    fun publicHeaderPreservesBodylessFunctionAndRealHelper(@TempDir root: Path) {
        val fixture = fixture(root, headerMetadata())
        val function = fixture.function("publicHeader")
        val header = assertNotNull(fixture.source.sourceHeaderOf(function))
        assertGroupedHeader(function, header)
        assertFalse(header.bodyAvailable)
        assertNull(fixture.source.sourceGraphOf(function))
        val repeated = assertNotNull(fixture.source.sourceHeaderOf(fixture.function("publicHeader")))
        assertEquals(header, repeated)
        assertGroupedHeader(function, repeated)

        // 正常 CFIR 的 flattened 参数和 SOURCE 的两组参数借用同一真实声明。
        val helper = assertIs<CfirNamedFunction>(header.desugaredParameterLists[1].single())
        assertSame(helper, function.valueParameters[1].defaultValueMetadata?.function)
        assertSame(helper.symbol.cfir, helper)
        assertEquals("default_second", helper.name.asString())
        assertEquals(ConePrimitiveType.INT64, helper.returnTypeRef.coneType)
        val helperHeader = assertNotNull(fixture.source.sourceHeaderOf(helper))
        assertTrue(helperHeader.bodyAvailable)
        assertEquals(ConePrimitiveType.INT64, helperHeader.returnType)
        assertNull(function.body)
        assertNull(helper.body)
    }

    /** 公共 graph 保存 named headers，包括 body0 函数；查询与正文材料化共享参数/helper 身份。 */
    @Test
    fun publicGraphKeepsHeadersAcrossHeaderFirstAndBodyFirstQueries(@TempDir root: Path) {
        for (headerFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(headerFirst.toString()), headerMetadata())
            val function = fixture.function("publicHeader")
            val entry = fixture.function("entry")
            val before = if (headerFirst) assertNotNull(fixture.source.sourceHeaderOf(function)) else null
            val selected = assertNotNull(fixture.source.sourceGraphOf(entry))
            assertEquals(setOf(entry), selected.functionBodies.keys)
            val selectedBody = selected.functionBodies.getValue(entry)
            val selectedRecord = selected.expression(selectedBody)
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(entry))
            assertSame(selectedBody, graph.functionBodies.getValue(entry))
            assertSame(selectedRecord, graph.expression(selectedBody))
            val header = graph.functionHeaders.getValue(function)
            assertGroupedHeader(function, header)
            assertFalse(header.bodyAvailable)
            assertFalse(graph.functionBodies.containsKey(function))
            if (before != null) assertEquals(before, header)
            assertEquals(header, fixture.source.sourceHeaderOf(function))

            val helper = assertNotNull(header.desugaredParameterLists[1].single())
            assertEquals(setOf(function, helper, entry), graph.functionHeaders.keys)
            for (owner in listOf(entry, helper)) {
                assertTrue(graph.functionHeaders.getValue(owner).bodyAvailable)
                assertEquals(graph.functionHeaders.getValue(owner), fixture.source.sourceHeaderOf(owner))
                val ownerGraph = assertNotNull(fixture.source.sourceGraphOf(owner))
                assertSame(graph.functionBodies.getValue(owner), ownerGraph.functionBodies.getValue(owner))
                val body = graph.functionBodies.getValue(owner)
                val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(body).payload)
                val returnIdentity = assertIs<CfirSourceSlot.Expression>(block.statements.single()).identity
                val returned = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(returnIdentity).payload)
                assertSame(owner, returned.function)
                assertEquals(ConePrimitiveType.NOTHING, graph.expression(returnIdentity).type)
                assertEquals(ConePrimitiveType.INT64, graph.expression(returned.value).type)
                assertNull(owner.body, "SOURCE 能力不能伪造正常 CFIR body 或 BODY_RESOLVE 资格")
            }
            assertSame(graph, fixture.source.packageSourceGraphOf(fixture.function("entry")))
            assertSame(selected, fixture.source.sourceGraphOf(entry))
            assertSame(entry, fixture.function("entry"))
        }
    }

    /** 损坏的是 body e 引用；表内所有 statement 仍是真实 Block/Return/literal 结构。 */
    @Test
    fun headerOnlyQueriesNeverDecodeAnAvailableButInvalidBody(@TempDir root: Path) {
        val input = headerMetadata()
        val invalidBody = (input.expressions.size + 1).toUInt()
        val declarations = input.declarations.toMutableList()
        val info = assertIs<CjoFunctionInfo>(declarations[0].info)
        declarations[0] = declarations[0].copy(info = info.copy(body = info.body.copy(body = 1u)))
        val fixture = fixture(root, input.copy(declarations = declarations), corruptedBody = invalidBody)
        val function = fixture.function("publicHeader")
        val header = assertNotNull(fixture.source.sourceHeaderOf(function))
        assertGroupedHeader(function, header)
        assertTrue(header.bodyAvailable, "availability 来自非零保存槽，不代表坏正文已经完成")
        repeat(3) {
            val repeated = assertNotNull(fixture.source.sourceHeaderOf(fixture.function("publicHeader")))
            assertEquals(header, repeated)
            assertGroupedHeader(function, repeated)
        }
        val error = assertFailsWith<IllegalArgumentException> { fixture.source.sourceGraphOf(function) }
        assertTrue(assertNotNull(error.message).contains("Invalid SOURCE e reference $invalidBody"))
        // 正文事务失败不损坏此前完成的签名/header；重复 body 请求仍报告原损坏。
        assertSame(function, fixture.function("publicHeader"))
        assertEquals(header, fixture.source.sourceHeaderOf(function))
        assertFailsWith<IllegalArgumentException> { fixture.source.sourceGraphOf(function) }
        assertNull(function.body)
    }

    /** native core缺JString的真实错误恢复仍保留四形态；另外三种使用官方String类型。 */
    @Test
    fun fourStringKindsSurviveThePublicGraph(@TempDir root: Path) {
        val core = officialCore()
        val fixture = fixture(root, stringMetadata(core), core)
        val function = fixture.function("strings")
        val graph = assertNotNull(fixture.source.sourceGraphOf(function))
        val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(function)).payload)
        val identities = block.statements.take(4).map { assertIs<CfirSourceSlot.Expression>(it).identity }
        val expected = listOf(CfirSourceStringKind.NORMAL, CfirSourceStringKind.JSTRING,
            CfirSourceStringKind.MULTILINE, CfirSourceStringKind.MULTILINE_RAW)
        identities.forEachIndexed { index, identity ->
            val record = graph.expression(identity)
            val literal = assertIs<CfirSourceExpressionPayload.Literal>(record.payload)
            assertEquals(expected[index], literal.stringKind)
            assertEquals(index == 1, literal.isJavaString)
            assertEquals(if (index == 1) CfirLiteralKind.JSTRING else CfirLiteralKind.STRING, literal.kind)
            assertEquals("same text", literal.text)
            if (index == 1) {
                assertIs<ConeErrorType>(record.type, "固定 native 无JString引用时实际语义类型是Invalid，不能伪造String")
            } else {
                val classifier = assertIs<ConeClassifierType>(record.type)
                assertEquals("std.core.String", assertIs<ConeClassLikeLookupTag>(classifier.lookupTag).classId.asFqNameString())
            }
            assertSame(function, record.context)
            assertSame(record, graph.expression(identity))
            identities.drop(index + 1).forEach { assertNotSame(identity, it) }
        }
        val wire = fixture.wire
        assertEquals(listOf(StringKind.Normal, StringKind.JString, StringKind.MultiLine, StringKind.MultiLineRaw),
            (1..4).map { (assertNotNull(wire.allExprs(it)).info(LitConstInfo()) as LitConstInfo).strKind })
        assertSame(graph, fixture.source.sourceGraphOf(function))
        assertEquals(graph.functionHeaders.getValue(function), fixture.source.sourceHeaderOf(function))
    }

    /** 两个同型 Match 各自恢复 pattern 的 selector 回边，不按相同文本/结构合并身份。 */
    @Test
    fun eachMatchPatternBorrowsItsOwnSelectorAndKeepsItsPayload(@TempDir root: Path) {
        val fixture = fixture(root, matchMetadata())
        val function = fixture.function("matches")
        val graph = assertNotNull(fixture.source.sourceGraphOf(function))
        val body = graph.functionBodies.getValue(function)
        val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(body).payload)
        val returnIdentity = assertIs<CfirSourceSlot.Expression>(block.statements.single()).identity
        val returned = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(returnIdentity).payload)
        assertSame(function, returned.function)
        val tuple = assertIs<CfirSourceExpressionPayload.Tuple>(graph.expression(returned.value).payload)
        val matches = tuple.elements.map { assertIs<CfirSourceExpressionPayload.Match>(graph.expression(it).payload) }
        assertEquals(2, matches.size)
        val selectors = matches.map { assertNotNull(it.selector) }
        assertNotSame(selectors[0], selectors[1])
        assertEquals(graph.expression(selectors[0]).payload, graph.expression(selectors[1]).payload)

        matches.forEachIndexed { matchIndex, match ->
            val selector = selectors[matchIndex]
            assertEquals(2, match.cases.size)
            assertEquals(1, graph.expressions.values.sumOf { it.payload.expressionChildren().count { child -> child === selector } },
                "selector 仅由 Match 拥有；pattern.ctx 不得进入 expressionChildren")
            match.cases.forEachIndexed { caseIndex, caseIdentity ->
                val caseRecord = graph.expression(caseIdentity)
                val case = assertIs<CfirSourceExpressionPayload.MatchCase>(caseRecord.payload)
                val pattern = case.patterns.single()
                assertSame(selector, pattern.contextExpression)
                assertNotSame(selectors[1 - matchIndex], pattern.contextExpression)
                assertFalse(pattern.expressionChildren().any { it === selector })
                assertEquals(listOf(ConePrimitiveType.INT64), pattern.types)
                assertEquals(ConePrimitiveType.INT64, caseRecord.type)
                assertFalse(pattern.needRuntimeTypeCheck)
                assertFalse(pattern.matchBeforeRuntime)
                assertEquals(CfirSourceImportedPosition(CjSourceLocation("source-public.cj", 11, 5),
                    CjSourceLocation("source-public.cj", 11, 6)), pattern.importedPosition)
                assertIs<CfirSourceExpressionPayload.Block>(graph.expression(case.body).payload)
                if (caseIndex == 0) {
                    assertEquals(CfirSourcePatternKind.CONSTANT, pattern.kind)
                    assertEquals(ConePrimitiveType.BOOLEAN, graph.expression(assertNotNull(case.guard)).type)
                    assertTrue(pattern.constantValues.isEmpty(), "固定 native SOURCE ConstPattern 不保存 values 缓存")
                    val slot = assertIs<CfirSourceSlot.Expression>(pattern.slots.single())
                    assertNotSame(selector, slot.identity)
                    assertEquals(graph.expression(selector).payload, graph.expression(slot.identity).payload)
                } else {
                    assertEquals(CfirSourcePatternKind.WILDCARD, pattern.kind)
                    assertNull(case.guard)
                    assertTrue(pattern.slots.isEmpty())
                    assertTrue(pattern.constantValues.isEmpty())
                }
                assertSame(caseRecord, graph.expression(caseIdentity))
                assertSame(pattern, assertIs<CfirSourceExpressionPayload.MatchCase>(graph.expression(caseIdentity).payload).patterns.single())
            }
        }
        assertSame(graph, fixture.source.sourceGraphOf(function))
    }

    /** 一个 case e identity 不能同时归属两个不同 selector；不复制 case 来接纳冲突。 */
    @Test
    fun aRepeatedCaseIdentityWithDifferentSelectorsIsRejected(@TempDir root: Path) {
        val input = matchMetadata()
        val expressions = input.expressions.toMutableList()
        expressions[4] = expressions[4].copy(operands = listOf(7u, 8u, 11u))
        val fixture = fixture(root, input.copy(expressions = expressions))
        val function = fixture.function("matches")
        val header = assertNotNull(fixture.source.sourceHeaderOf(function))
        val error = assertFailsWith<IllegalStateException> { fixture.source.sourceGraphOf(function) }
        assertTrue(assertNotNull(error.message).contains("One SOURCE MatchCase cannot bind different selectors"))
        assertEquals(header, fixture.source.sourceHeaderOf(function))
        assertSame(function, fixture.function("matches"))
    }

    /** 同一 Match 的不同槽可共享 case record；selector 绑定按 record 唯一而 occurrence 保留。 */
    @Test
    fun aRepeatedCaseIdentityWithTheSameSelectorKeepsOneRecord(@TempDir root: Path) {
        val input = matchMetadata()
        val expressions = input.expressions.toMutableList()
        expressions[3] = expressions[3].copy(operands = listOf(6u, 8u, 8u, 10u))
        val fixture = fixture(root, input.copy(expressions = expressions))
        val function = fixture.function("matches")
        val graph = assertNotNull(fixture.source.sourceGraphOf(function))
        val matches = graph.expressions.values.mapNotNull { it.payload as? CfirSourceExpressionPayload.Match }
        val repeated = matches.single { it.cases.size == 3 }
        assertSame(repeated.cases[0], repeated.cases[1])
        val case = assertIs<CfirSourceExpressionPayload.MatchCase>(graph.expression(repeated.cases[0]).payload)
        assertSame(repeated.selector, case.patterns.single().contextExpression)
        assertSame(graph.expression(repeated.cases[0]), graph.expression(repeated.cases[1]))
        assertSame(graph, fixture.source.sourceGraphOf(function))
    }

    /**
     * N14276740 的 raw identifier→普通 Lookup→原目标/类型→ReferenceInfo 合同。
     * metadata 由现有 Writer 编码，未声称这些 seed 经官方 cjc 编译；函数类型 extend 不作为正例。
     * 参数/变量保留原 identity，同时检查 canonical SOURCE 与正式普通 BODY。
     */
    @Test
    fun rawFunctionValuesKeepTheirTargetsAndReadableBodies(@TempDir root: Path) {
        for (spelling in listOf("this", "super")) {
            for (kind in listOf(FunctionValueTarget.PARAMETER, FunctionValueTarget.VARIABLE, FunctionValueTarget.ALIASED_VARIABLE)) {
                val fixture = fixture(root.resolve("$spelling-$kind"), functionValueMetadata(spelling, kind))
                val function = fixture.function("metadataRootF")
                val header = assertRootFunctionHeader(fixture, function)
                val parameter = function.valueParameters.single()
                val target: CfirVariable = if (kind == FunctionValueTarget.PARAMETER) parameter
                    else fixture.variableBinding(2)
                assertEquals(spelling, target.symbol.name.asString())
                assertSame(target, target.symbol.cfir)
                val declaredValueType = target.returnTypeRef.coneType
                if (kind == FunctionValueTarget.ALIASED_VARIABLE) {
                    // 真实 Relay=Handler，Handler=()->Int64；原 VarDecl.type 仍指 Relay，RHS 来自 AliasInfo。
                    val handler = assertIs<CfirTypeAlias>(fixture.declaration(5))
                    val relay = assertIs<CfirTypeAlias>(fixture.declaration(6))
                    val rhs = assertIs<ConeFunctionType>(handler.expandedTypeRef.coneType)
                    assertTrue(rhs.parameterTypes.isEmpty())
                    assertEquals(ConePrimitiveType.INT64, rhs.returnType)
                    assertFalse(target.returnTypeRef.coneType is ConeFunctionType,
                        "正例必须保留 alias 值类型，不能预先将目标 VarDecl 改写为 Func")
                    assertFalse(relay.expandedTypeRef.coneType is ConeFunctionType,
                        "两层 alias 必须消费已完成 RHS，不以一层类型种类替代完整展开")
                    assertSame(handler, fixture.declaration(5))
                    assertSame(relay, fixture.declaration(6))
                } else {
                    assertIs<ConeFunctionType>(target.returnTypeRef.coneType)
                }
                val graph = assertNotNull(fixture.source.sourceGraphOf(function))
                val returned = rootReturn(graph, function)
                val record = graph.expression(returned.value)
                val reference = assertIs<CfirSourceExpressionPayload.Reference>(record.payload)
                assertEquals(CfirSourceReferenceRole.ORDINARY, reference.role)
                assertEquals(Name.identifier(spelling), reference.name)
                assertSame(target, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
                assertIs<ConeFunctionType>(record.type)
                assertSame(function, record.context)
                assertSame(header, graph.functionHeaders.getValue(function))
                assertNull(function.body)

                val body = assertNotNull(fixture.source.sourceBodyOf(function))
                val ordinaryReturn = assertIs<CfirReturnExpression>(body.statements.single())
                assertTrue(ordinaryReturn.target.isBound)
                assertSame(function, ordinaryReturn.target.labeledElement)
                val ordinaryValue = assertIs<CfirNamedAccessExpression>(ordinaryReturn.result)
                val resolved = assertIs<CfirResolvedNamedReference>(ordinaryValue.calleeReference)
                assertSame(target.symbol, resolved.resolvedSymbol)
                assertSame(target, resolved.resolvedSymbol.cfir)
                assertSame(record.type, ordinaryValue.coneTypeOrNull)
                assertSame(body, function.body)
                val completed = assertNotNull(fixture.source.sourceGraphOf(function))
                val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(function)).payload)
                val returnIdentity = assertIs<CfirSourceSlot.Expression>(block.statements.single()).identity
                assertSame(returnIdentity, completed.expressionOf(ordinaryReturn))
                assertSame(returned.value, completed.expressionOf(ordinaryValue))
                assertSame(record, completed.expression(returned.value))
                assertSame(ConePrimitiveType.NOTHING, ordinaryReturn.coneTypeOrNull)
                assertSame(body, fixture.source.sourceBodyOf(function))
                assertSame(header, fixture.source.sourceHeaderOf(function))
                assertSame(parameter, function.valueParameters.single())
                assertSame(function, fixture.function("metadataRootF"))
                assertSame(declaredValueType, target.returnTypeRef.coneType,
                    "ORDINARY 资格不得改写原参数/变量的完成值类型或 alias abbreviation")
                if (kind != FunctionValueTarget.PARAMETER) {
                    val binding = assertIs<CfirPatternBindingVariable>(target)
                    val root = assertIs<CfirPatternVariable>(fixture.declaration(2))
                    assertSame(root, binding.patternTreeOwner)
                    // 变量具有真实 initializer 保存槽，不用 initializer=0 冒充函数值来源。
                    val implementation = fixture.function("implementation")
                    val initializer = assertIs<CfirNamedAccessExpression>(assertNotNull(fixture.source.sourceInitializerOf(binding)))
                    assertSame(initializer, root.initializer)
                    assertSame(initializer, binding.initializer)
                    assertSame(implementation.symbol, assertIs<CfirResolvedNamedReference>(initializer.calleeReference).resolvedSymbol)
                    val implementationBody = assertNotNull(fixture.source.sourceBodyOf(implementation))
                    val implementationReturn = assertIs<CfirReturnExpression>(implementationBody.statements.single())
                    assertSame(implementation, implementationReturn.target.labeledElement)
                    assertSame(implementationBody, implementation.body)
                }
            }
        }
    }

    /**
     * R34-alias-cycle：Identity<T>=T 的同 symbol 有限嵌套不是定义依赖环。
     * docs 类型别名规则及 N14276740 PreCheck.cpp 的定义路径检查为语义依据；
     * K3c7a504 TypeExpansionUtils.kt 的每次实参替换只作为框架参照。
     * 固定 N=14276740ffec1262b81b6b5236b1d840af270b37，K=3c7a5041f238102285764d370665f56f587f9dc4。
     * 复用 CjoTypeGraphDeserializationTest 的 TP.type/GenericTyInfo、空约束与 alias self 模板。
     * 这些是 Writer metadata seed，不是官方 cjc 产物；执行与合入由 main 验证。
     * 静态限制：本候选未编译/运行，不授 native CJO 或 provider/lazy/infer 调用次数验证结论；
     * 泛型嵌套覆盖函数值变量，原直接 Func 参数和 Handler/Relay 正例的期望保持原样。
     */
    @Test
    fun finiteIdentityFunctionValuesKeepOriginalTypesSymbolsAndBodies(@TempDir root: Path) {
        for (spelling in listOf("this", "super")) {
            for (depth in listOf(1, 2, 3)) {
                for (aliasFirst in listOf(false, true)) {
                    val input = genericFunctionValueMetadata(spelling, depth = depth)
                    assertAliasTypeTableIsStructurallyAcyclic(input)
                    val fixture = fixture(root.resolve("$spelling-$depth-$aliasFirst"), input)
                    assertGenericFunctionAliasWire(input, fixture.wire)
                    if (aliasFirst) assertCompletedGenericFunctionAlias(fixture, 7, 8)
                    val function = fixture.function("metadataRootF")
                    val header = assertRootFunctionHeader(fixture, function)
                    val parameter = function.valueParameters.single()
                    val identity = assertCompletedGenericFunctionAlias(fixture, 7, 8)
                    val own = assertIs<CfirTypeParameter>(identity.typeParameters.single())
                    val rhsRef = identity.expandedTypeRef
                    val rhs = assertIs<ConeTypeParameterType>(rhsRef.coneType)
                    assertSame(own.symbol, rhs.lookupTag.typeParameterSymbol)
                    val target = fixture.variableBinding(2)
                    val valueTypeRef = target.returnTypeRef
                    val valueType = valueTypeRef.coneType
                    val returnTypeRef = function.returnTypeRef
                    val parameterTypeRef = parameter.returnTypeRef
                    assertEquals(spelling, target.name.asString())
                    var application = assertIs<ConeClassLikeType>(valueType)
                    repeat(depth) { level ->
                        assertAliasApplication(application, identity)
                        val argument = application.typeArguments.single().type
                        if (level + 1 == depth) {
                            val leaf = assertIs<ConeFunctionType>(argument)
                            assertSame(header.returnType, leaf)
                            assertTrue(leaf.parameterTypes.isEmpty())
                            assertEquals(ConePrimitiveType.INT64, leaf.returnType)
                        } else {
                            application = assertIs<ConeClassLikeType>(argument)
                        }
                    }
                    val graph = assertNotNull(fixture.source.sourceGraphOf(function))
                    val returned = rootReturn(graph, function)
                    val record = graph.expression(returned.value)
                    val reference = assertIs<CfirSourceExpressionPayload.Reference>(record.payload)
                    assertEquals(CfirSourceReferenceRole.ORDINARY, reference.role)
                    assertEquals(Name.identifier(spelling), reference.name)
                    assertSame(target, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
                    assertSame(header.returnType, record.type)
                    assertSame(function, record.context)
                    assertSame(header, graph.functionHeaders.getValue(function))
                    assertNull(function.body)

                    val body = assertNotNull(fixture.source.sourceBodyOf(function))
                    val ordinaryReturn = assertIs<CfirReturnExpression>(body.statements.single())
                    assertTrue(ordinaryReturn.target.isBound)
                    assertSame(function, ordinaryReturn.target.labeledElement)
                    val ordinaryValue = assertIs<CfirNamedAccessExpression>(ordinaryReturn.result)
                    val resolved = assertIs<CfirResolvedNamedReference>(ordinaryValue.calleeReference)
                    assertSame(target.symbol, resolved.resolvedSymbol)
                    assertSame(target, resolved.resolvedSymbol.cfir)
                    assertSame(record.type, ordinaryValue.coneTypeOrNull)
                    assertSame(ConePrimitiveType.NOTHING, ordinaryReturn.coneTypeOrNull)
                    val completed = assertNotNull(fixture.source.sourceGraphOf(function))
                    val block = assertIs<CfirSourceExpressionPayload.Block>(
                        graph.expression(graph.functionBodies.getValue(function)).payload)
                    val returnIdentity = assertIs<CfirSourceSlot.Expression>(block.statements.single()).identity
                    assertSame(returnIdentity, completed.expressionOf(ordinaryReturn))
                    assertSame(returned.value, completed.expressionOf(ordinaryValue))
                    assertSame(record, completed.expression(returned.value))
                    assertSame(body, function.body)
                    assertSame(body, fixture.source.sourceBodyOf(function))
                    assertSame(header, fixture.source.sourceHeaderOf(function))
                    assertSame(function, fixture.function("metadataRootF"))
                    assertSame(returnTypeRef, function.returnTypeRef)
                    assertSame(parameter, function.valueParameters.single())
                    assertSame(parameter, header.parameterLists.single().single())
                    assertSame(parameterTypeRef, parameter.returnTypeRef)
                    assertSame(target, fixture.declaration(2))
                    assertSame(valueTypeRef, target.returnTypeRef)
                    assertSame(valueType, target.returnTypeRef.coneType)
                    assertSame(identity, assertCompletedGenericFunctionAlias(fixture, 7, 8))
                    assertSame(rhsRef, identity.expandedTypeRef)
                    assertSame(rhs, identity.expandedTypeRef.coneType)
                    assertSame(own, identity.typeParameters.single())
                    // 保留原 d3 initializer=e7 与 d4 的完整 FuncBody，不能靠缺初始化器制造函数值。
                    val implementation = fixture.function("implementation")
                    val initializer = assertIs<CfirNamedAccessExpression>(
                        assertNotNull(fixture.source.sourceInitializerOf(target)))
                    assertSame(implementation.symbol,
                        assertIs<CfirResolvedNamedReference>(initializer.calleeReference).resolvedSymbol)
                    assertSame(record.type, initializer.coneTypeOrNull)
                    val implementationBody = assertNotNull(fixture.source.sourceBodyOf(implementation))
                    val implementationReturn = assertIs<CfirReturnExpression>(implementationBody.statements.single())
                    assertSame(implementation, implementationReturn.target.labeledElement)
                    val literal = assertIs<CfirLiteralExpression>(implementationReturn.result)
                    assertEquals("7", literal.value)
                    assertEquals(CfirLiteralKind.INT, literal.kind)
                    assertSame(ConePrimitiveType.INT64, literal.coneTypeOrNull)
                    assertSame(implementationBody, implementation.body)
                    assertSame(implementationBody, fixture.source.sourceBodyOf(implementation))
                }
            }
        }
    }

    /**
     * 定义环必须在 SOURCE 资格边界明确拒绝，包括实参不断增长和 Func/tuple 中的依赖。
     * allTypes 的类型结构引用始终为 DAG；先取得全部 alias 完成 RHS/TP 与根 header。
     * 更早的 TypeDeserializer 结构环、未完成 header 或 arity 错误均不能算本测试成功。
     */
    @Test
    fun actualGenericAliasDefinitionCyclesRejectAfterHeadersComplete(@TempDir root: Path) {
        for (spelling in listOf("this", "super")) {
            for (definition in GenericFunctionAliasDefinition.entries.filter { it != GenericFunctionAliasDefinition.IDENTITY }) {
                val input = genericFunctionValueMetadata(spelling, definition)
                assertAliasTypeTableIsStructurallyAcyclic(input)
                val fixture = fixture(root.resolve("$spelling-$definition"), input)
                assertGenericFunctionAliasWire(input, fixture.wire)
                // 这些调用在 assertFailsWith 之外：必须已成功发表真实完整声明，才能检查定义循环。
                val identity = assertCompletedGenericFunctionAlias(fixture, 7, 8)
                val own = assertIs<CfirTypeParameter>(identity.typeParameters.single())
                val rhsRef = identity.expandedTypeRef
                val rhs = rhsRef.coneType
                val pair = if (definition == GenericFunctionAliasDefinition.MUTUAL_CYCLE)
                    assertCompletedGenericFunctionAlias(fixture, 9, 10) else null
                val pairRhsRef = pair?.expandedTypeRef
                val dependency = when (definition) {
                    GenericFunctionAliasDefinition.FUNCTION_PARAMETER_CYCLE ->
                        assertIs<ConeClassLikeType>(assertIs<ConeFunctionType>(rhs).parameterTypes.single())
                    GenericFunctionAliasDefinition.FUNCTION_RESULT_CYCLE ->
                        assertIs<ConeClassLikeType>(assertIs<ConeFunctionType>(rhs).returnType)
                    GenericFunctionAliasDefinition.TUPLE_CYCLE ->
                        assertIs<ConeClassLikeType>(assertIs<ConeTupleType>(rhs).elementTypes[1])
                    else -> assertIs<ConeClassLikeType>(rhs)
                }
                assertAliasApplication(dependency, pair ?: identity)
                if (definition == GenericFunctionAliasDefinition.GROWING_CYCLE) {
                    // Identity<T>=Identity<(T,T)>：展开参数增长，但原 RHS 类型对象本身不成结构环。
                    val growth = assertIs<ConeTupleType>(dependency.typeArguments.single().type)
                    assertEquals(2, growth.elementTypes.size)
                    growth.elementTypes.forEach {
                        assertSame(own.symbol, assertIs<ConeTypeParameterType>(it).lookupTag.typeParameterSymbol)
                    }
                } else {
                    assertSame(own.symbol,
                        assertIs<ConeTypeParameterType>(dependency.typeArguments.single().type).lookupTag.typeParameterSymbol)
                }
                if (pair != null) {
                    val reverse = assertIs<ConeClassLikeType>(pair.expandedTypeRef.coneType)
                    assertAliasApplication(reverse, identity)
                    assertSame(pair.typeParameters.single().symbol,
                        assertIs<ConeTypeParameterType>(reverse.typeArguments.single().type).lookupTag.typeParameterSymbol)
                    assertNotSame(own.symbol, pair.typeParameters.single().symbol)
                }
                val function = fixture.function("metadataRootF")
                val header = assertRootFunctionHeader(fixture, function)
                val parameter = function.valueParameters.single()
                val target = fixture.variableBinding(2)
                val valueTypeRef = target.returnTypeRef
                val valueType = assertIs<ConeClassLikeType>(valueTypeRef.coneType)
                assertAliasApplication(valueType, identity)
                assertIs<ConeFunctionType>(valueType.typeArguments.single().type)
                val returnTypeRef = function.returnTypeRef
                val parameterTypeRef = parameter.returnTypeRef
                val message = "A SOURCE function value alias requires a bound, acyclic header"
                repeat(2) {
                    val graphError = assertFailsWith<IllegalStateException> { fixture.source.sourceGraphOf(function) }
                    assertEquals(message, graphError.message)
                    val bodyError = assertFailsWith<IllegalStateException> { fixture.source.sourceBodyOf(function) }
                    assertEquals(message, bodyError.message)
                    assertNull(function.body)
                    assertSame(function, fixture.function("metadataRootF"))
                    assertSame(header, fixture.source.sourceHeaderOf(function))
                    assertSame(returnTypeRef, function.returnTypeRef)
                    assertSame(parameter, function.valueParameters.single())
                    assertSame(parameter, header.parameterLists.single().single())
                    assertSame(parameterTypeRef, parameter.returnTypeRef)
                    assertSame(target, fixture.declaration(2))
                    assertSame(valueTypeRef, target.returnTypeRef)
                    assertSame(valueType, target.returnTypeRef.coneType)
                    assertSame(identity, assertCompletedGenericFunctionAlias(fixture, 7, 8))
                    assertSame(rhsRef, identity.expandedTypeRef)
                    assertSame(rhs, identity.expandedTypeRef.coneType)
                    assertSame(own, identity.typeParameters.single())
                    if (pair != null) {
                        assertSame(pair, assertCompletedGenericFunctionAlias(fixture, 9, 10))
                        assertSame(pairRhsRef, pair.expandedTypeRef)
                    }
                }
            }
        }
    }

    /** 坏 Func 组合在 role 创建边界失败；失败不替换先前发表的函数、header 或参数。 */
    @Test
    fun specialFunctionReferencesRejectIncompatibleTargetsWithoutReplacingHeaders(@TempDir root: Path) {
        for (spelling in listOf("this", "super")) {
            for (kind in BadFunctionTarget.entries) {
                val input = functionValueMetadata(spelling, FunctionValueTarget.PARAMETER)
                val target = when (kind) {
                    BadFunctionTarget.PACKAGE -> CjoExpressionTargetMetadata(pkgId = PackageIndex.PACKAGE_REFERENCE.value, decl = input.fullPackageName)
                    BadFunctionTarget.MISSING -> null
                    BadFunctionTarget.INVALID -> CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value, decl = "")
                    BadFunctionTarget.CLASS_LIKE -> declarationTarget(5u)
                    BadFunctionTarget.NON_FUNCTION_PARAMETER -> declarationTarget(2u)
                }
                val expressions = input.expressions.toMutableList()
                expressions[2] = expressions[2].copy(reference = CjoReferenceExpressionMetadata(spelling, target))
                val declarations = input.declarations.toMutableList()
                val types = input.types.toMutableList()
                if (kind == BadFunctionTarget.NON_FUNCTION_PARAMETER) {
                    declarations[1] = declarations[1].copy(type = 1u)
                    types[3] = functionType(listOf(1u), 3u)
                }
                val fixture = fixture(root.resolve("$spelling-$kind"), input.copy(
                    declarations = declarations, types = types, expressions = expressions))
                val function = fixture.function("metadataRootF")
                val header = assertRootFunctionHeader(fixture, function)
                val parameter = function.valueParameters.single()
                if (kind == BadFunctionTarget.NON_FUNCTION_PARAMETER) assertEquals(ConePrimitiveType.INT64, parameter.returnTypeRef.coneType)
                val message = "A function-valued SOURCE $spelling reference requires its completed callable declaration target"
                repeat(2) {
                    val graphError = assertFailsWith<IllegalStateException> { fixture.source.sourceGraphOf(function) }
                    assertEquals(message, graphError.message)
                    assertSame(function, fixture.function("metadataRootF"))
                    assertSame(header, fixture.source.sourceHeaderOf(function))
                    assertSame(parameter, function.valueParameters.single())
                    assertSame(parameter, header.parameterLists.single().single())
                    assertNull(function.body)
                    val bodyError = assertFailsWith<IllegalStateException> { fixture.source.sourceBodyOf(function) }
                    assertEquals(message, bodyError.message)
                    assertSame(header, fixture.source.sourceHeaderOf(function))
                    assertSame(function, fixture.function("metadataRootF"))
                    assertSame(parameter, function.valueParameters.single())
                    assertNull(function.body)
                }
            }
        }
    }

    /** 普通 name 的 Func+Absent 保留两种原原因与真实 error reference，不套用关键词禁令。 */
    @Test
    fun ordinaryFunctionAbsenceKeepsTheOriginalErrorReference(@TempDir root: Path) {
        for (reason in listOf(CfirSourceReferenceAbsence.FIELD_MISSING, CfirSourceReferenceAbsence.INVALID_SENTINEL)) {
            val input = functionValueMetadata("handler", FunctionValueTarget.PARAMETER)
            val target = if (reason == CfirSourceReferenceAbsence.FIELD_MISSING) null
                else CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value, decl = "")
            val expressions = input.expressions.toMutableList()
            expressions[2] = expressions[2].copy(reference = CjoReferenceExpressionMetadata("handler", target))
            val fixture = fixture(root.resolve(reason.toString()), input.copy(expressions = expressions))
            val function = fixture.function("metadataRootF")
            val header = assertRootFunctionHeader(fixture, function)
            val graph = assertNotNull(fixture.source.sourceGraphOf(function))
            val returned = rootReturn(graph, function)
            val record = graph.expression(returned.value)
            val reference = assertIs<CfirSourceExpressionPayload.Reference>(record.payload)
            assertEquals(CfirSourceReferenceRole.ORDINARY, reference.role)
            assertEquals(reason, assertIs<CfirSourceReferenceTarget.Absent>(reference.target).reason)
            assertIs<ConeFunctionType>(record.type)
            val body = assertNotNull(fixture.source.sourceBodyOf(function))
            val ordinaryReturn = assertIs<CfirReturnExpression>(body.statements.single())
            assertSame(function, ordinaryReturn.target.labeledElement)
            val access = assertIs<CfirNamedAccessExpression>(ordinaryReturn.result)
            val error = assertIs<CfirErrorNamedReference>(access.calleeReference)
            assertEquals(Name.identifier("handler"), error.name)
            val diagnostic = assertIs<ConeSimpleDiagnostic>(error.diagnostic)
            assertEquals(DiagnosticKind.DeserializationError, diagnostic.kind)
            assertEquals("Serialized reference has no declaration target: $reason", diagnostic.reason)
            assertSame(record.type, access.coneTypeOrNull)
            val completed = assertNotNull(fixture.source.sourceGraphOf(function))
            assertSame(returned.value, completed.expressionOf(access))
            assertSame(record, completed.expression(returned.value))
            assertSame(body, fixture.source.sourceBodyOf(function))
            assertSame(header, fixture.source.sourceHeaderOf(function))
        }
    }

    /** builtin extend 的非Func this+Absent 仍拥有真实 Extend receiver；原 ReferenceForms 两例保持。 */
    @Test
    fun builtinExtendThisRetainsItsAbsentTargetAndReadablePrimitiveBody(@TempDir root: Path) {
        for (reason in listOf(CfirSourceReferenceAbsence.FIELD_MISSING, CfirSourceReferenceAbsence.INVALID_SENTINEL)) {
            val target = if (reason == CfirSourceReferenceAbsence.FIELD_MISSING) null
                else CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value, decl = "")
            val input = metadata("r34builtinthis", listOf(
                CjoPackageDeclaration("", DeclKind.ExtendDecl, type = 1u, info = CjoExtendInfo(body = listOf(2u))),
                CjoPackageDeclaration("metadataRootF", type = 3u, isTopLevel = false, attributes = flags(Attribute.PUBLIC, Attribute.IN_EXTEND),
                    info = CjoFunctionInfo(body = CjoFunctionBodyInfo(parameterLists = listOf(emptyList()),
                        desugaredParameterLists = listOf(emptyList()), returnType = 1u, body = 1u))),
            ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing), functionType(emptyList(), 1u)), listOf(
                block(1u, listOf(2u)),
                CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(3u)),
                CjoExpressionMetadata(ExprKind.RefExpr, type = 1u, reference = CjoReferenceExpressionMetadata("this", target)),
            ))
            val fixture = fixture(root.resolve(reason.toString()), input)
            val owner = assertIs<CfirExtend>(fixture.declaration(0))
            val function = assertIs<CfirNamedFunction>(fixture.declaration(1))
            assertSame(function, owner.declarations.single())
            val header = assertNotNull(fixture.source.sourceHeaderOf(function))
            assertTrue(header.bodyAvailable)
            assertEquals(ConePrimitiveType.INT64, header.returnType)
            assertTrue(header.parameterLists.single().isEmpty())
            assertTrue(header.desugaredParameterLists.single().isEmpty())
            val graph = assertNotNull(fixture.source.sourceGraphOf(function))
            val returned = rootReturn(graph, function)
            val record = graph.expression(returned.value)
            val reference = assertIs<CfirSourceExpressionPayload.Reference>(record.payload)
            assertEquals(CfirSourceReferenceRole.THIS, reference.role)
            assertEquals(reason, assertIs<CfirSourceReferenceTarget.Absent>(reference.target).reason)
            assertEquals(ConePrimitiveType.INT64, record.type)
            assertSame(owner, graph.declarationOwners.getValue(function))
            val body = assertNotNull(fixture.source.sourceBodyOf(function))
            val ordinaryReturn = assertIs<CfirReturnExpression>(body.statements.single())
            assertSame(function, ordinaryReturn.target.labeledElement)
            val receiver = assertIs<CfirThisReceiverExpression>(ordinaryReturn.result)
            val thisReference = assertIs<CfirThisReference>(receiver.calleeReference)
            assertSame(owner.symbol, thisReference.boundSymbol)
            assertFalse(thisReference.isImplicit)
            assertEquals(ConePrimitiveType.INT64, receiver.coneTypeOrNull)
            assertSame(ConePrimitiveType.NOTHING, ordinaryReturn.coneTypeOrNull)
            val completed = assertNotNull(fixture.source.sourceGraphOf(function))
            assertSame(returned.value, completed.expressionOf(receiver))
            assertSame(record, completed.expression(returned.value))
            assertSame(body, fixture.source.sourceBodyOf(function))
            assertSame(header, fixture.source.sourceHeaderOf(function))
        }
    }

    private enum class FunctionValueTarget { PARAMETER, VARIABLE, ALIASED_VARIABLE }
    private enum class BadFunctionTarget { PACKAGE, MISSING, INVALID, CLASS_LIKE, NON_FUNCTION_PARAMETER }

    /** d1 根 F；d2 参数；d3 函数值变量；d4 initializer 引用的真实具名函数；d5 class；d6/d7 两层 alias。 */
    private fun functionValueMetadata(spelling: String, kind: FunctionValueTarget): CjoPackageMetadata {
        val parameterName = if (kind == FunctionValueTarget.PARAMETER) spelling else "input"
        val variableName = if (kind == FunctionValueTarget.PARAMETER) "storedHandler" else spelling
        return metadata("r34functionvalues", listOf(
            CjoPackageDeclaration("metadataRootF", type = 4u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(parameterLists = listOf(listOf(2u)),
                    desugaredParameterLists = listOf(listOf(0u)), returnType = 3u, body = 1u))),
            CjoPackageDeclaration(parameterName, DeclKind.FuncParam, isTopLevel = false, type = 3u,
                info = CjoParameterInfo(isNamed = false, isMember = false)),
            CjoPackageDeclaration(variableName, DeclKind.VarDecl, type = if (kind == FunctionValueTarget.ALIASED_VARIABLE) 7u else 3u,
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoVariableInfo(isVar = false, initializer = 7u)),
            CjoPackageDeclaration("implementation", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(parameterLists = listOf(emptyList()),
                    desugaredParameterLists = listOf(emptyList()), returnType = 1u, body = 4u))),
            CjoPackageDeclaration("Owner", DeclKind.ClassDecl, type = 5u, attributes = flags(Attribute.PUBLIC), info = CjoClassInfo()),
            CjoPackageDeclaration("Handler", DeclKind.TypeAliasDecl, type = 6u, attributes = flags(Attribute.PUBLIC), info = CjoAliasInfo(3u)),
            CjoPackageDeclaration("Relay", DeclKind.TypeAliasDecl, type = 7u, attributes = flags(Attribute.PUBLIC), info = CjoAliasInfo(6u)),
        ), listOf(
            CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing),
            functionType(emptyList(), 1u), functionType(listOf(3u), 3u),
            CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(5u)),
            CjoTypeMetadata(TypeKind.Type, semanticInfo = CjoCompositeTypeInfoMetadata(6u)),
            CjoTypeMetadata(TypeKind.Type, semanticInfo = CjoCompositeTypeInfoMetadata(7u)),
        ), listOf(
            block(3u, listOf(2u)),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(3u)),
            CjoExpressionMetadata(ExprKind.RefExpr, type = 3u, reference = CjoReferenceExpressionMetadata(spelling,
                declarationTarget(if (kind == FunctionValueTarget.PARAMETER) 2u else 3u))),
            block(1u, listOf(5u)),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(6u)),
            integer(1u, "7"),
            CjoExpressionMetadata(ExprKind.RefExpr, type = 3u, reference = CjoReferenceExpressionMetadata("implementation", declarationTarget(4u))),
        ))
    }

    private enum class GenericFunctionAliasDefinition {
        IDENTITY, DIRECT_CYCLE, MUTUAL_CYCLE, GROWING_CYCLE,
        FUNCTION_PARAMETER_CYCLE, FUNCTION_RESULT_CYCLE, TUPLE_CYCLE,
    }

    /**
     * 保留 functionValueMetadata 的 d1/F、d2 参数、d3 变量、d4 initializer 函数及 e1..e7。
     * 新增 d8 Identity<T>/d9 T；t8=T、t9=Identity<T> self。声明 type 与 AliasInfo RHS 独立。
     * 互环另有 d10 Pair<U>/d11 U；所有 GenericParamDecl.type 指向自己的 GenericTyInfo。
     * 泛型别名不允许 where，故 constraints 和最终 upperBounds 均明确 present-empty。
     */
    private fun genericFunctionValueMetadata(
        spelling: String,
        definition: GenericFunctionAliasDefinition = GenericFunctionAliasDefinition.IDENTITY,
        depth: Int = 2,
    ): CjoPackageMetadata {
        require(depth > 0)
        val input = functionValueMetadata(spelling, FunctionValueTarget.VARIABLE)
        val declarations = input.declarations.toMutableList()
        val types = input.types.toMutableList()
        fun append(type: CjoTypeMetadata): UInt {
            types += type
            return types.size.toUInt()
        }
        fun aliasType(declaration: UInt, argument: UInt): CjoTypeMetadata =
            CjoTypeMetadata(TypeKind.Type, listOf(argument), semanticInfo = CjoCompositeTypeInfoMetadata(declaration))
        val ownType = append(CjoTypeMetadata(TypeKind.Generic,
            semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = 9u, upperBounds = emptyList())))
        val selfType = append(aliasType(8u, ownType))
        val rhsType = when (definition) {
            GenericFunctionAliasDefinition.IDENTITY -> ownType
            GenericFunctionAliasDefinition.DIRECT_CYCLE -> append(aliasType(8u, ownType))
            GenericFunctionAliasDefinition.GROWING_CYCLE -> {
                val growth = append(CjoTypeMetadata(TypeKind.Tuple, listOf(ownType, ownType)))
                append(aliasType(8u, growth))
            }
            GenericFunctionAliasDefinition.FUNCTION_PARAMETER_CYCLE -> {
                val dependency = append(aliasType(8u, ownType))
                append(functionType(listOf(dependency), 1u))
            }
            GenericFunctionAliasDefinition.FUNCTION_RESULT_CYCLE -> {
                val dependency = append(aliasType(8u, ownType))
                append(functionType(emptyList(), dependency))
            }
            GenericFunctionAliasDefinition.TUPLE_CYCLE -> {
                val dependency = append(aliasType(8u, ownType))
                append(CjoTypeMetadata(TypeKind.Tuple, listOf(1u, dependency)))
            }
            GenericFunctionAliasDefinition.MUTUAL_CYCLE -> append(aliasType(10u, ownType))
        }
        declarations += CjoPackageDeclaration("Identity", DeclKind.TypeAliasDecl, type = selfType,
            attributes = flags(Attribute.PUBLIC), exportId = input.fullPackageName + "::Identity",
            generic = CjoGenericMetadata(typeParameters = listOf(9u), constraints = emptyList()),
            info = CjoAliasInfo(rhsType))
        declarations += CjoPackageDeclaration("T", DeclKind.GenericParamDecl, isTopLevel = false, type = ownType,
            exportId = input.fullPackageName + "::Identity.T")
        if (definition == GenericFunctionAliasDefinition.MUTUAL_CYCLE) {
            val otherType = append(CjoTypeMetadata(TypeKind.Generic,
                semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = 11u, upperBounds = emptyList())))
            val otherSelf = append(aliasType(10u, otherType))
            val reverseRhs = append(aliasType(8u, otherType))
            declarations += CjoPackageDeclaration("Pair", DeclKind.TypeAliasDecl, type = otherSelf,
                attributes = flags(Attribute.PUBLIC), exportId = input.fullPackageName + "::Pair",
                generic = CjoGenericMetadata(typeParameters = listOf(11u), constraints = emptyList()),
                info = CjoAliasInfo(reverseRhs))
            declarations += CjoPackageDeclaration("U", DeclKind.GenericParamDecl, isTopLevel = false, type = otherType,
                exportId = input.fullPackageName + "::Pair.U")
        }
        var useType = 3u // 原 ()->Int64；每层 Identity 都保留自己的实际应用类型。
        repeat(if (definition == GenericFunctionAliasDefinition.IDENTITY) depth else 1) {
            useType = append(aliasType(8u, useType))
        }
        declarations[2] = declarations[2].copy(type = useType)
        return input.copy(declarations = declarations, types = types)
    }

    /** 只沿 allTypes 的结构边检查；FullId 的声明/RHS 依赖故意不在此结构 DAG 内。 */
    private fun assertAliasTypeTableIsStructurallyAcyclic(input: CjoPackageMetadata) {
        val visiting = HashSet<UInt>()
        val completed = HashSet<UInt>()
        fun visit(index: UInt) {
            assertTrue(index > 0u && index <= input.types.size.toUInt())
            if (index in completed) return
            assertTrue(visiting.add(index), "候选不能以 allTypes 结构环替代 alias 定义环：t$index")
            val type = input.types[index.toInt() - 1]
            type.typeArguments.forEach { visit(it) }
            when (val info = type.semanticInfo) {
                is CjoFunctionTypeInfoMetadata -> visit(info.returnType)
                is CjoGenericTypeInfoMetadata -> info.upperBounds.forEach { visit(it) }
                else -> Unit
            }
            assertTrue(visiting.remove(index))
            completed.add(index)
        }
        input.types.indices.forEach { visit((it + 1).toUInt()) }
    }

    /** 在 consumer 前核 TP.type、Generic/Composite/Func union、FullId 和 alias self/RHS 保存槽。 */
    private fun assertGenericFunctionAliasWire(input: CjoPackageMetadata, wire: Package) {
        input.types.forEachIndexed { index, expected ->
            val actual = assertNotNull(wire.allTypes(index))
            assertEquals(expected.kind, actual.kind)
            assertEquals(expected.typeArguments, (0 until actual.typeArgsLength).map { actual.typeArgs(it) })
            when (val info = expected.semanticInfo) {
                is CjoGenericTypeInfoMetadata -> {
                    assertEquals(PackageFormat.SemaTyInfo.GenericTyInfo, actual.infoType)
                    val generic = assertIs<PackageFormat.GenericTyInfo>(actual.info(PackageFormat.GenericTyInfo()))
                    val fullId = assertNotNull(generic.declPtr)
                    assertEquals(info.packageId, fullId.pkgId)
                    assertEquals(info.declarationIndex, fullId.index)
                    assertEquals(info.upperBounds, (0 until generic.upperBoundsLength).map { generic.upperBounds(it) })
                    val parameter = assertNotNull(wire.allDecls(fullId.index.toInt() - 1))
                    assertEquals(DeclKind.GenericParamDecl, parameter.kind)
                    assertEquals((index + 1).toUInt(), parameter.type)
                    assertFalse(parameter.isTopLevel)
                }
                is CjoCompositeTypeInfoMetadata -> {
                    assertEquals(PackageFormat.SemaTyInfo.CompositeTyInfo, actual.infoType)
                    val composite = assertIs<PackageFormat.CompositeTyInfo>(actual.info(PackageFormat.CompositeTyInfo()))
                    val fullId = assertNotNull(composite.declPtr)
                    assertEquals(info.packageId, fullId.pkgId)
                    assertEquals(info.declarationIndex, fullId.index)
                    assertEquals(info.isThisType, composite.isThisTy)
                }
                is CjoFunctionTypeInfoMetadata -> {
                    assertEquals(PackageFormat.SemaTyInfo.FuncTyInfo, actual.infoType)
                    val function = assertIs<PackageFormat.FuncTyInfo>(actual.info(PackageFormat.FuncTyInfo()))
                    assertEquals(info.returnType, function.retType)
                    assertEquals(info.isC, function.isC)
                    assertEquals(info.hasVariableLenArg, function.hasVariableLenArg)
                }
                else -> Unit
            }
        }
        input.declarations.forEachIndexed { index, expected ->
            val genericInput = expected.generic ?: return@forEachIndexed
            val declaration = assertNotNull(wire.allDecls(index))
            assertEquals(DeclKind.TypeAliasDecl, declaration.kind)
            assertEquals(expected.type, declaration.type)
            val generic = assertNotNull(declaration.generic)
            assertEquals(genericInput.typeParameters,
                (0 until generic.typeParametersLength).map { generic.typeParameters(it) })
            assertTrue(genericInput.constraints.isEmpty())
            assertEquals(0, generic.constraintsLength)
            val self = assertNotNull(wire.allTypes(declaration.type.toInt() - 1))
            assertEquals(TypeKind.Type, self.kind)
            val selfInfo = assertIs<PackageFormat.CompositeTyInfo>(self.info(PackageFormat.CompositeTyInfo()))
            assertEquals((index + 1).toUInt(), assertNotNull(selfInfo.declPtr).index)
            assertEquals(genericInput.typeParameters.map {
                assertNotNull(wire.allDecls(it.toInt() - 1)).type
            }, (0 until self.typeArgsLength).map { self.typeArgs(it) })
            val alias = assertIs<PackageFormat.AliasInfo>(declaration.info(PackageFormat.AliasInfo()))
            assertEquals(assertIs<CjoAliasInfo>(expected.info).aliasedType, alias.aliasedTy)
            assertTrue(alias.aliasedTy > 0u && alias.aliasedTy <= wire.allTypesLength.toUInt())
        }
    }

    private fun assertCompletedGenericFunctionAlias(fixture: Fixture, aliasIndex: Int, parameterIndex: Int): CfirTypeAlias {
        val alias = assertIs<CfirTypeAlias>(fixture.declaration(aliasIndex))
        assertTrue(alias.symbol.isBound)
        assertSame(alias, alias.symbol.cfir)
        val parameter = assertIs<CfirTypeParameter>(alias.typeParameters.single())
        assertSame(parameter, fixture.declaration(parameterIndex))
        assertSame(parameter, parameter.symbol.cfir)
        assertSame(alias.symbol, parameter.containingDeclarationSymbol)
        assertTrue(parameter.bounds.isEmpty(), "present-empty 最终上界不能改写成默认 Any")
        assertFalse(alias.expandedTypeRef.coneType is ConeErrorType)
        return alias
    }

    private fun assertAliasApplication(type: ConeClassLikeType, alias: CfirTypeAlias) {
        val tag = assertIs<ConeClassLikeLookupTagWithFixedSymbol>(type.lookupTag)
        assertSame(alias.symbol, tag.symbol)
        assertEquals(alias.symbol.classId, tag.classId)
        assertEquals(alias.typeParameters.size, type.typeArguments.size)
        assertEquals(1, type.typeArguments.size)
    }

    private fun declarationTarget(index: UInt): CjoExpressionTargetMetadata =
        CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = index)

    private fun assertRootFunctionHeader(fixture: Fixture, function: CfirNamedFunction): CfirSourceImportedFunctionHeader {
        val header = assertNotNull(fixture.source.sourceHeaderOf(function))
        assertTrue(header.bodyAvailable)
        assertEquals(function.returnTypeRef.coneType, header.returnType)
        assertEquals(1, header.parameterLists.size)
        assertSame(function.valueParameters.single(), header.parameterLists.single().single())
        assertSame(function.symbol, function.valueParameters.single().containingDeclarationSymbol)
        assertNull(header.desugaredParameterLists.single().single())
        val type = assertIs<ConeFunctionType>(header.returnType)
        assertTrue(type.parameterTypes.isEmpty())
        assertEquals(ConePrimitiveType.INT64, type.returnType)
        assertNull(function.body)
        return header
    }

    private fun rootReturn(graph: CfirSourceGraph, function: CfirNamedFunction): CfirSourceExpressionPayload.Return {
        val body = graph.functionBodies.getValue(function)
        assertSame(function, graph.expression(body).context)
        assertEquals(function.returnTypeRef.coneType, graph.expression(body).type)
        val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(body).payload)
        val identity = assertIs<CfirSourceSlot.Expression>(block.statements.single()).identity
        assertEquals(ConePrimitiveType.NOTHING, graph.expression(identity).type)
        return assertIs<CfirSourceExpressionPayload.Return>(graph.expression(identity).payload).also { assertSame(function, it.function) }
    }

    private fun assertGroupedHeader(function: CfirNamedFunction, header: CfirSourceImportedFunctionHeader) {
        assertEquals(listOf(listOf("first"), listOf("second")), header.parameterLists.map { list -> list.map { it.name.asString() } })
        assertEquals(listOf(1, 1), header.desugaredParameterLists.map { it.size })
        assertNull(header.desugaredParameterLists[0].single(), "保存的 0 helper 槽必须保留 null 和原位置")
        assertIs<CfirNamedFunction>(header.desugaredParameterLists[1].single())
        assertNull(header.returnType, "FuncBody.retType=0 不可用完整 FuncDecl.type 的返回类型填补")
        assertEquals(ConePrimitiveType.INT64, function.returnTypeRef.coneType)
        assertEquals(1, header.captureKind)
        assertTrue(header.always)
        assertEquals(2, function.valueParameters.size)
        header.parameterLists.flatten().forEachIndexed { index, parameter ->
            assertSame(function.valueParameters[index], parameter)
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
            assertEquals(ConePrimitiveType.INT64, parameter.returnTypeRef.coneType)
        }
        assertSame(function.valueParameters[1].defaultValueMetadata?.function, header.desugaredParameterLists[1].single())
    }

    /** d1 body0；d2/d3 参数；d4 是实际命名 helper；d5 提供获取完整公共 graph 的入口。 */
    private fun headerMetadata(): CjoPackageMetadata = metadata("sourceheaders", listOf(
        CjoPackageDeclaration("publicHeader", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(listOf(2u), listOf(3u)),
                listOf(listOf(0u), listOf(4u)), returnType = 0u, body = 0u, always = true, captureKind = 1u))),
        CjoPackageDeclaration("first", DeclKind.FuncParam, isTopLevel = false, type = 1u,
            info = CjoParameterInfo(isNamed = false, isMember = false)),
        CjoPackageDeclaration("second", DeclKind.FuncParam, isTopLevel = false, type = 1u,
            info = CjoParameterInfo(isNamed = true, isMember = false)),
        CjoPackageDeclaration("default_second", isTopLevel = false, type = 4u,
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u, body = 4u))),
        CjoPackageDeclaration("entry", type = 4u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing),
        functionType(listOf(1u, 1u), 1u), functionType(emptyList(), 1u)), listOf(
        block(1u, listOf(2u)),
        CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(3u)),
        integer(1u, "42"),
        block(1u, listOf(5u)),
        CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(6u)),
        integer(1u, "7"),
    ))

    /** 从官方 core 的声明和 self SemaTy 取得实际类型 kind/export key，避免伪造 String 类型。 */
    private fun stringMetadata(core: ByteArray): CjoPackageMetadata {
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(core))
        fun stringType(name: String): CjoTypeMetadata {
            val declaration = assertNotNull((0 until pkg.allDeclsLength).map { assertNotNull(pkg.allDecls(it)) }
                .singleOrNull { it.isTopLevel && it.identifier == name },
                "官方 core CJO 必须包含真实 $name 声明，不能伪造类型或退化为 Int64/String")
            val type = assertNotNull(pkg.allTypes(declaration.type.toInt() - 1))
            assertTrue(type.kind == TypeKind.Struct || type.kind == TypeKind.Class)
            return CjoTypeMetadata(type.kind, semanticInfo = CjoCompositeTypeInfoMetadata(
                declarationIndex = 0u, packageId = 0, declarationKey = assertNotNull(declaration.exportId)))
        }
        return metadata("sourcestrings", listOf(
            CjoPackageDeclaration("strings", type = 4u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
        ), listOf(CjoTypeMetadata(TypeKind.Unit), stringType("String"), CjoTypeMetadata(TypeKind.Invalid),
            functionType(emptyList(), 1u), CjoTypeMetadata(TypeKind.Nothing)), listOf(
            block(1u, listOf(2u, 3u, 4u, 5u, 6u)),
            string(2u, LitConstKind.String, StringKind.Normal),
            string(3u, LitConstKind.JString, StringKind.JString),
            string(2u, LitConstKind.String, StringKind.MultiLine),
            string(2u, LitConstKind.String, StringKind.MultiLineRaw),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 5u, operands = listOf(7u)),
            CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
                literal = CjoLiteralExpressionMetadata("()", LitConstKind.Unit)),
        ), imports = listOf("std.core"))
    }

    /** 两个穷尽 Match 返回 (Int64,Int64)；根、case body 和 Return 都有真实类型与完整槽。 */
    private fun matchMetadata(): CjoPackageMetadata {
        fun pattern(kind: Byte, expression: UInt? = null) = CjoPatternMetadata(
            kind = kind, types = listOf(1u), exprs = listOfNotNull(expression),
            values = emptyList(),
            begin = CjoPositionMetadata(file = 1u, line = 11, column = 5),
            end = CjoPositionMetadata(file = 1u, line = 11, column = 6),
        )
        fun case(guard: UInt, body: UInt, pattern: CjoPatternMetadata) = CjoExpressionMetadata(
            ExprKind.MatchCase, type = 1u, operands = listOf(guard, body),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(listOf(pattern)))
        return metadata("sourcematches", listOf(
            CjoPackageDeclaration("matches", type = 6u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 5u, body = 1u))),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Bool), CjoTypeMetadata(TypeKind.Unit),
            CjoTypeMetadata(TypeKind.Nothing), CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 1u)),
            functionType(emptyList(), 5u)), listOf(
            block(5u, listOf(2u)), // e1
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 4u, operands = listOf(3u)),
            CjoExpressionMetadata(ExprKind.TupleLit, type = 5u, operands = listOf(4u, 5u)),
            CjoExpressionMetadata(ExprKind.MatchExpr, type = 1u, operands = listOf(6u, 8u, 10u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(true)),
            CjoExpressionMetadata(ExprKind.MatchExpr, type = 1u, operands = listOf(7u, 9u, 11u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(true)),
            integer(1u, "7"), integer(1u, "7"), // e6/e7：文本相同，拥有者不同
            case(12u, 16u, pattern(PatternKind.ConstPattern, 14u)),
            case(13u, 17u, pattern(PatternKind.ConstPattern, 15u)),
            case(0u, 18u, pattern(PatternKind.WildcardPattern)),
            case(0u, 19u, pattern(PatternKind.WildcardPattern)),
            CjoExpressionMetadata(ExprKind.LitConstExpr, type = 2u,
                literal = CjoLiteralExpressionMetadata("true", LitConstKind.Bool)),
            CjoExpressionMetadata(ExprKind.LitConstExpr, type = 2u,
                literal = CjoLiteralExpressionMetadata("true", LitConstKind.Bool)),
            integer(1u, "7"), integer(1u, "7"),
            block(1u, listOf(20u)), block(1u, listOf(21u)),
            block(1u, listOf(22u)), block(1u, listOf(23u)),
            integer(1u, "9"), integer(1u, "9"), integer(1u, "10"), integer(1u, "10"),
        ))
    }

    /** Block.isExpr 的末槽是真实 UNSAFE 标志，不是额外 statement。 */
    private fun block(type: UInt, statements: List<UInt>): CjoExpressionMetadata = CjoExpressionMetadata(
        ExprKind.Block, type = type, operands = statements,
        additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(List(statements.size) { true } + false))

    private fun integer(type: UInt, value: String): CjoExpressionMetadata = CjoExpressionMetadata(
        ExprKind.LitConstExpr, type = type, literal = CjoLiteralExpressionMetadata(value, LitConstKind.Integer))

    private fun string(type: UInt, kind: UByte, stringKind: UByte): CjoExpressionMetadata = CjoExpressionMetadata(
        ExprKind.LitConstExpr, type = type, literal = CjoLiteralExpressionMetadata("same text", kind, stringKind))

    private fun functionType(parameters: List<UInt>, result: UInt): CjoTypeMetadata =
        CjoTypeMetadata(TypeKind.Func, parameters, semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))

    private fun metadata(
        name: String,
        declarations: List<CjoPackageDeclaration>,
        types: List<CjoTypeMetadata>,
        expressions: List<CjoExpressionMetadata>,
        imports: List<String> = emptyList(),
    ): CjoPackageMetadata = CjoPackageMetadata(
        fullPackageName = name, moduleName = name, schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
        cjoVersion = CjoFormatVersion(0u, 1u, 0u), allFiles = listOf("source-public.cj"),
        declarations = declarations, types = types, expressions = expressions, imports = imports,
    )

    private data class Fixture(
        val packageName: String,
        val module: Module,
        val manager: CjoManager,
        val provider: CfirDeserializedSymbolProvider,
        val wire: Package,
    ) {
        val source: CfirImportedSourceGraphProvider get() = provider

        fun function(name: String): CfirNamedFunction = assertIs<CfirNamedFunction>(
            provider.getTopLevelCallableSymbols(FqName(packageName), Name.identifier(name)).single().cfir)

        fun declaration(index: Int): CfirDeclaration = assertNotNull(provider.deserializeCjoDeclaration(
            assertNotNull(manager.loadPackageSnapshot(packageName)).sourcePath, packageName, index))

        fun variableBinding(index: Int): CfirPatternBindingVariable {
            val root = assertIs<CfirPatternVariable>(declaration(index))
            val binding = root.pattern.bindingVariables().single()
            assertSame(root, binding.patternTreeOwner)
            assertSame(binding, provider.packageVariableBinding(packageName, binding.name.asString()))
            return binding
        }
    }

    /** 与 CjoTypeGraphDeserializationTest 相同的真实 session/provider/Manager/Writer 装配。 */
    private fun fixture(root: Path, input: CjoPackageMetadata, officialCore: ByteArray? = null, corruptedBody: UInt? = null): Fixture {
        Files.createDirectories(root)
        if (officialCore != null) Files.write(root.resolve("std.core.cjo"), officialCore)
        val encoded = CjoPackageWriter.toByteArray(input)
        if (corruptedBody != null) {
            // 先由正式writer生成合法文件，再用生成accessor注入单一损坏保存槽，验证读取能力边界。
            val packageData = Package.getRootAsPackage(ByteBuffer.wrap(encoded))
            val body = assertNotNull((assertNotNull(packageData.allDecls(0)).info(FuncInfo()) as FuncInfo).funcBody)
            assertTrue(body.mutateBody(corruptedBody))
        }
        Files.write(root.resolve(input.fullPackageName + ".cjo"), encoded)
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(root)),
        )
        module.session.register(CfirSymbolProvider::class, provider)
        val wire = assertNotNull(manager.loadPackage(input.fullPackageName))
        assertWireStructure(input, wire)
        return Fixture(input.fullPackageName, module, manager, provider, wire)
    }

    /** 在 consumer 之前核对真实表和 union；正文负例也不使用假 Block 或缺类型的 statement。 */
    private fun assertWireStructure(input: CjoPackageMetadata, wire: Package) {
        assertEquals(input.declarations.size, wire.allDeclsLength)
        assertEquals(input.types.size, wire.allTypesLength)
        assertEquals(input.expressions.size, wire.allExprsLength)
        input.expressions.forEachIndexed { index, expected ->
            val expression = assertNotNull(wire.allExprs(index))
            assertEquals(expected.kind, expression.kind)
            assertTrue(expression.type != 0u && expression.type <= wire.allTypesLength.toUInt())
            assertEquals(expected.operands, (0 until expression.operandsLength).map { expression.operands(it) })
            when (expected.kind) {
                ExprKind.Block -> {
                    assertEquals(ExprInfo.BlockInfo, expression.infoType)
                    val info = expression.info(PackageFormat.BlockInfo()) as PackageFormat.BlockInfo
                    assertEquals(expression.operandsLength + 1, info.isExprLength)
                    assertFalse(info.isExpr(expression.operandsLength))
                }
                ExprKind.ReturnExpr -> {
                    assertEquals(ExprInfo.NONE, expression.infoType)
                    assertEquals(1, expression.operandsLength)
                }
                ExprKind.MatchCase -> {
                    assertEquals(ExprInfo.MatchCaseInfo, expression.infoType)
                    assertEquals(2, expression.operandsLength)
                    assertEquals(1, (expression.info(MatchCaseInfo()) as MatchCaseInfo).patternsLength)
                }
                ExprKind.LitConstExpr -> assertEquals(ExprInfo.LitConstInfo, expression.infoType)
                ExprKind.RefExpr -> {
                    assertEquals(ExprInfo.ReferenceInfo, expression.infoType)
                    val actual = expression.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
                    val reference = assertNotNull(expected.reference)
                    assertEquals(reference.reference, actual.reference)
                    val target = reference.target
                    if (target == null) assertNull(actual.target) else {
                        val actualTarget = assertNotNull(actual.target)
                        assertEquals(target.pkgId, actualTarget.pkgId)
                        assertEquals(target.index, actualTarget.index)
                        assertEquals(target.decl, actualTarget.decl)
                    }
                }
            }
        }
        if (input.fullPackageName.startsWith("r34")) {
            input.declarations.forEachIndexed { index, expected ->
                val info = expected.info as? CjoFunctionInfo ?: return@forEachIndexed
                val actual = assertNotNull((assertNotNull(wire.allDecls(index)).info(FuncInfo()) as FuncInfo).funcBody)
                assertTrue(info.body.body != 0u)
                assertEquals(info.body.body, actual.body)
                assertEquals(ExprKind.Block, assertNotNull(wire.allExprs(actual.body.toInt() - 1)).kind)
                assertEquals(info.body.returnType, actual.retType)
                assertTrue(actual.retType != 0u)
                assertEquals(info.body.parameterLists.size, actual.paramListsLength)
                info.body.parameterLists.indices.forEach { listIndex ->
                    val list = assertNotNull(actual.paramLists(listIndex))
                    assertEquals(info.body.parameterLists[listIndex], (0 until list.paramsLength).map { list.params(it) })
                    assertEquals(info.body.desugaredParameterLists[listIndex], (0 until list.desugarsLength).map { list.desugars(it) })
                    (0 until list.paramsLength).forEach { assertEquals(DeclKind.FuncParam, assertNotNull(wire.allDecls(list.params(it).toInt() - 1)).kind) }
                }
            }
        }
        if (input.fullPackageName == "sourceheaders") {
            val body = assertNotNull((assertNotNull(wire.allDecls(0)).info(FuncInfo()) as FuncInfo).funcBody)
            assertEquals(2, body.paramListsLength)
            assertEquals(0u, body.retType)
            assertEquals(0u, assertNotNull(body.paramLists(0)).desugars(0))
            assertEquals(4u, assertNotNull(body.paramLists(1)).desugars(0))
            assertTrue(assertNotNull(wire.allDecls(0)).type != 0u)
            assertEquals(DeclKind.FuncDecl, assertNotNull(wire.allDecls(3)).kind)
        }
    }

    private fun officialCore(): ByteArray = assertNotNull(javaClass.classLoader.getResourceAsStream(
        "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"), "已有官方 std.core CJO 测试资源必须存在").use { it.readBytes() }

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }

    /** 库声明由正式 provider 构建，参数/helper 使用同一模块和 session 的 canonical symbol。 */
    private class Module : CfirModuleData() {
        override val name: Name = Name.identifier("cjo-source-public-payload-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = "cjo-source-public-payload-test"
        override val session: CfirSession = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            // alias RHS 的正式参数替换器使用当前 session 的 typeContext，复用既有 TypeComponents 装配。
            it.register(TypeComponents::class, TypeComponents(it))
        }
        init { bindSession(session) }
    }
}
