package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestSpecBase
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.*
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.IdentityHashMap

/** PSI 的真实 SOURCE/FRONTEND 必须发布可读取的 primitive 运算绑定。 */
class PrimitiveOperatorBindingTest : AbstractPrimitiveOperatorBindingTest(CfirParser.Psi)

/** LightTree 逐字复用同一官方已编译 fixture 和完整绑定契约。 */
class PrimitiveOperatorBindingLightTreeTest : AbstractPrimitiveOperatorBindingTest(CfirParser.LightTree)

/**
 * 官方 Windows cjc 1.1.3 对 primitiveOperatorBindings.cj 的 staticlib 编译退出码为 0、无诊断。
 * 语义表来自官方 src/Sema/BuiltInOperatorUtil.cpp 和操作符重载文档；不调用 CFIR 匹配器生成期望。
 */
abstract class AbstractPrimitiveOperatorBindingTest(parser: CfirParser) : AbstractCfirDiagnosticTestSpecBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> PrimitiveOperatorBindingHandler(services) })
        }
    }

    @Test
    fun allPrimitiveFamiliesRetainTheirResolvedBindings() {
        runTest("cfir/analysis-tests/testData/diagnostics2/operator/primitiveOperatorBindings.cj")
    }
}

/**
 * 从正式 FRONTEND 的完整声明树读取引用，包括嵌套链、比较专有节点和匿名函数。
 * 签名按官方操作数/返回类型逐项验收，不能以“没有诊断”代替绑定完成。
 */
private class PrimitiveOperatorBindingHandler(testServices: TestServices) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules)
        val part = info.partsForDependsOnModules.single { it.module === module }
        val file = part.firFilesByTestFile.filterKeys { !it.isAdditional }.values.single()
        assertEquals(CfirDeclarationOrigin.Source, file.origin)
        val functions = file.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
        val expected = expectedSignatures()
        assertEquals(expected.keys + setOf("applyOperator", "userPlus", "userUnaryMinus", "userNot"), functions.keys)

        for ((name, signatures) in expected) {
            val function = functions.getValue(name)
            assertEquals(CfirDeclarationOrigin.Source, function.origin)
            val references = elements(function).filterIsInstance<CfirResolvedBuiltinOperatorReference>()
            // 多重集同时检查完整 signature 和出现次数，不依赖访问者的字段遍历顺序。
            assertEquals(
                signatures.groupingBy { it }.eachCount(),
                references.map { it.signature }.groupingBy { it }.eachCount(),
                "$name 必须保留每个运算位置的完整内建绑定",
            )
            references.forEach { reference -> assertEquals(reference.signature.name, reference.name) }
        }

        assertBooleanNodes(functions.getValue("boolOperators"), listOf(CfirBinaryOpKind.AND, CfirBinaryOpKind.OR))
        assertBooleanNodes(
            functions.getValue("nestedBool"),
            listOf(CfirBinaryOpKind.AND, CfirBinaryOpKind.AND, CfirBinaryOpKind.OR),
        )
        assertUserOverloads(file, functions)
        assertResolvedAndStable(file)
        checkedModules++
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "每个 parser 必须完成一次真实 SOURCE/FRONTEND 验收")
    }

    /** 短路逻辑保留专有节点和左右 Bool 输入，不伪装成普通 operator 函数调用。 */
    private fun assertBooleanNodes(function: CfirNamedFunction, expected: List<CfirBinaryOpKind>) {
        val nodes = elements(function).filterIsInstance<CfirBinaryOp>()
        assertEquals(expected.groupingBy { it }.eachCount(), nodes.map { it.kind }.groupingBy { it }.eachCount())
        nodes.forEach { node ->
            assertEquals(BOOLEAN, primitiveKind(node.coneTypeOrNull))
            assertEquals(BOOLEAN, primitiveKind(node.left.coneTypeOrNull))
            assertEquals(BOOLEAN, primitiveKind(node.right.coneTypeOrNull))
        }
        assertFalse(elements(function).filterIsInstance<CfirFunctionCall>().any { call ->
            (call.calleeReference as? CfirNamedReference)?.name in
                setOf(OperatorNameConventions.ANDAND, OperatorNameConventions.OROR)
        })
    }

    /** 三个真实成员由 owning class、operator 标志、元数和返回类型唯一确定，不构造替代 symbol。 */
    private fun assertUserOverloads(file: CfirFile, functions: Map<String, CfirNamedFunction>) {
        val box = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "OperatorBox" }
        val operators = box.declarations.filterIsInstance<CfirNamedFunction>().filter { it.status.isOperator }
        assertEquals(3, operators.size)
        val plus = operators.single { it.valueParameters.size == 1 }
        val minus = operators.single { candidate ->
            candidate.valueParameters.isEmpty() &&
                (candidate.returnTypeRef.coneTypeOrNull as? ConeClassLikeType)?.lookupTag?.classId == box.symbol.classId
        }
        val not = operators.single { candidate ->
            candidate.valueParameters.isEmpty() &&
                (candidate.returnTypeRef.coneTypeOrNull as? ConePrimitiveType)?.kind == BOOLEAN
        }
        for ((name, target) in mapOf("userPlus" to plus, "userUnaryMinus" to minus, "userNot" to not)) {
            val call = elements(functions.getValue(name)).filterIsInstance<CfirFunctionCall>()
                .single { it.origin == CfirFunctionCallOrigin.Operator }
            val reference = call.calleeReference
            assertTrue(reference is CfirResolvedNamedReference, "$name 必须保留普通重载引用")
            assertSame(target.symbol, (reference as CfirResolvedNamedReference).resolvedSymbol)
            assertSame(target, reference.resolvedSymbol.cfir)
            assertEquals(CfirDeclarationOrigin.Source, target.origin)
            assertTrue(elements(functions.getValue(name)).filterIsInstance<CfirResolvedBuiltinOperatorReference>().isEmpty())
        }
    }

    /** 再次只读遍历不能改变 binding 身份；已成功的 SOURCE 不得留下活动未解析 named reference。 */
    private fun assertResolvedAndStable(file: CfirFile) {
        val first = elements(file)
        val named = first.filterIsInstance<CfirNamedReference>()
        named.forEach { reference ->
            assertTrue(
                reference is CfirResolvedBuiltinOperatorReference || reference is CfirResolvedNamedReference,
                "FRONTEND 留下未完成的 named reference: ${reference::class.simpleName} ${reference.name}",
            )
        }
        val builtin = first.filterIsInstance<CfirResolvedBuiltinOperatorReference>()
        val signatures = builtin.map { it.signature }
        val second = elements(file).filterIsInstance<CfirResolvedBuiltinOperatorReference>()
        assertEquals(builtin.size, second.size)
        builtin.indices.forEach { index ->
            assertSame(builtin[index], second[index])
            assertSame(signatures[index], second[index].signature)
        }
        first.filterIsInstance<CfirFunctionCall>().forEach { call ->
            val reference = call.calleeReference
            if (reference is CfirResolvedBuiltinOperatorReference) {
                assertEquals(CfirFunctionCallOrigin.Operator, call.origin)
                assertEquals(reference.signature.returnKind, primitiveKind(call.coneTypeOrNull))
                assertEquals(reference.signature.receiverKind, primitiveKind(checkNotNull(call.explicitReceiver).coneTypeOrNull))
                assertEquals(reference.signature.parameterKinds, call.argumentList.arguments.map { primitiveKind(it.coneTypeOrNull) })
            }
        }
        first.filterIsInstance<CfirComparisonExpression>().forEach { comparison ->
            assertEquals(BOOLEAN, primitiveKind(comparison.coneTypeOrNull))
            assertEquals(primitiveKind(comparison.left.coneTypeOrNull), primitiveKind(comparison.right.coneTypeOrNull))
        }
        first.filterIsInstance<CfirLiteralExpression>().forEach { literal -> primitiveKind(literal.coneTypeOrNull) }
    }

    private fun primitiveKind(type: ConeCangJieType?): PrimitiveTypeKind {
        assertTrue(type is ConePrimitiveType, "primitive 操作数和结果必须完成定型: $type")
        val kind = (type as ConePrimitiveType).kind
        assertFalse(kind == IDEAL_INT || kind == IDEAL_FLOAT, "完成后的 SOURCE 不得保留 Ideal 运算输入")
        return kind
    }

    /** 只遍历实际 CFIR 子图，按节点身份去重，保留每个运算位置自己的 reference。 */
    private fun elements(root: CfirElement): List<CfirElement> {
        val result = mutableListOf<CfirElement>()
        val visited = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        root.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!visited.add(element)) return
                result += element
                element.acceptChildren(this)
            }
        })
        return result
    }

    /**
     * 官方 GENERAL_ARITHMIC_TYPE_MAP / GENERAL_RELATION_NUMERIC_TYPE_MAP 和 SEMA_EXP_TYPES 的 SOURCE 对照。
     * 每个条目对应 fixture 的一个已由 cjc 1.1.3 编译成功的表达式，不读取 CFIR builtin 候选表。
     */
    private fun expectedSignatures(): Map<String, List<BuiltinPrimitiveOperatorSignature>> = buildMap {
        val binaryArithmetic = listOf(
            OperatorNameConventions.PLUS, OperatorNameConventions.MINUS,
            OperatorNameConventions.TIMES, OperatorNameConventions.DIV,
        )
        val comparisons = listOf(
            OperatorNameConventions.EQUALS, OperatorNameConventions.NOT_EQUALS,
            OperatorNameConventions.COMPARE_LT, OperatorNameConventions.COMPARE_LTEQ,
            OperatorNameConventions.COMPARE_GT, OperatorNameConventions.COMPARE_GTEQ,
        )
        for (kind in numericKinds) {
            put("numeric${kind.typeName}", binaryArithmetic.map { binary(it, kind) } +
                unary(OperatorNameConventions.UNARY_MINUS, kind) +
                comparisons.map { binary(it, kind, returnKind = BOOLEAN) })
        }
        for (kind in integerKinds) {
            put("bits${kind.typeName}", listOf(
                OperatorNameConventions.REM, OperatorNameConventions.AND,
                OperatorNameConventions.OR, OperatorNameConventions.XOR,
            ).map { binary(it, kind) } + unary(OperatorNameConventions.NOT, kind))
        }
        put("runeComparisons", comparisons.map { binary(it, RUNE, returnKind = BOOLEAN) })
        put("unitComparisons", comparisons.take(2).map { binary(it, UNIT, returnKind = BOOLEAN) })
        put("boolOperators", comparisons.take(2).map { binary(it, BOOLEAN) } + unary(OperatorNameConventions.NOT, BOOLEAN))
        put("nestedBool", listOf(unary(OperatorNameConventions.NOT, BOOLEAN)))
        put("heterogeneousShifts", listOf(
            binary(OperatorNameConventions.LEFT_SHIFT, INT8, UINT64),
            binary(OperatorNameConventions.RIGHT_SHIFT, UINT16, INT8),
            binary(OperatorNameConventions.LEFT_SHIFT, INT32, UINT8),
            binary(OperatorNameConventions.RIGHT_SHIFT, UINT64, INT16),
        ))
        put("powerSignatures", listOf(
            binary(OperatorNameConventions.EXPONENTIATION, INT64, UINT64),
            binary(OperatorNameConventions.EXPONENTIATION, FLOAT64, INT64),
            binary(OperatorNameConventions.EXPONENTIATION, FLOAT64, FLOAT64),
        ))
        put("idealInt8", listOf(binary(OperatorNameConventions.PLUS, INT8), binary(OperatorNameConventions.TIMES, INT8)))
        put("idealUInt32", listOf(binary(OperatorNameConventions.PLUS, UINT32), binary(OperatorNameConventions.MINUS, UINT32)))
        put("idealFloat32", listOf(binary(OperatorNameConventions.PLUS, FLOAT32), binary(OperatorNameConventions.DIV, FLOAT32)))
        put("idealPower", listOf(binary(OperatorNameConventions.EXPONENTIATION, INT64, UINT64)))
        put("idealInferred", listOf(binary(OperatorNameConventions.PLUS, INT64)))
        put("nestedChain", binaryArithmetic.map { binary(it, INT64) })
        put("pclaInt32", listOf(binary(OperatorNameConventions.PLUS, INT32), binary(OperatorNameConventions.TIMES, INT32)))
        put("pclaFloat32", listOf(
            binary(OperatorNameConventions.PLUS, FLOAT32), binary(OperatorNameConventions.MINUS, FLOAT32),
            binary(OperatorNameConventions.DIV, FLOAT32),
        ))
        put("pclaNested", listOf(binary(OperatorNameConventions.PLUS, INT16)))
    }

    private fun binary(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        parameterKind: PrimitiveTypeKind = receiverKind,
        returnKind: PrimitiveTypeKind = receiverKind,
    ): BuiltinPrimitiveOperatorSignature = BuiltinPrimitiveOperatorSignature(name, receiverKind, listOf(parameterKind), returnKind)

    private fun unary(name: Name, kind: PrimitiveTypeKind): BuiltinPrimitiveOperatorSignature =
        BuiltinPrimitiveOperatorSignature(name, kind, emptyList(), kind)

    private companion object {
        val integerKinds = listOf(INT8, INT16, INT32, INT64, INT_NATIVE, UINT8, UINT16, UINT32, UINT64, UINT_NATIVE)
        val numericKinds = integerKinds + listOf(FLOAT16, FLOAT32, FLOAT64)
    }
}
