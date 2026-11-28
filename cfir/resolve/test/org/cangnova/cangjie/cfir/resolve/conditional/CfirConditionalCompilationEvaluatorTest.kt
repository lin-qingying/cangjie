package org.cangnova.cangjie.cfir.resolve.conditional

import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBinaryOp
import org.cangnova.cangjie.cfir.expressions.builder.buildComparisonExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationError
import org.cangnova.cangjie.cfir.session.ExplicitCfirConditionalCompilationSettings
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/** 官方 v1.1.3 ConditionalCompilation.cpp:242–249 对 AND/OR 两侧都求值，再执行 AllOf/AnyOf。 */
class CfirConditionalCompilationEvaluatorTest {
    /** 四组显式 debug/test 设置分别经过 AND、OR，共八项真值回归。 */
    @TestFactory
    fun logicalOperatorsCombineBothConfiguredConditions(): List<DynamicTest> {
        val cases = listOf(
            TruthCase(false, false, CfirConditionalCompilationResult.Disabled, CfirConditionalCompilationResult.Disabled),
            TruthCase(false, true, CfirConditionalCompilationResult.Disabled, CfirConditionalCompilationResult.Enabled),
            TruthCase(true, false, CfirConditionalCompilationResult.Disabled, CfirConditionalCompilationResult.Enabled),
            TruthCase(true, true, CfirConditionalCompilationResult.Enabled, CfirConditionalCompilationResult.Enabled),
        )
        return cases.flatMap { case ->
            listOf(CfirBinaryOpKind.AND to case.andResult, CfirBinaryOpKind.OR to case.orResult).map { (operation, expected) ->
                dynamicTest("debug=${case.debug} $operation test=${case.test}") {
                    val evaluator = CfirConditionalCompilationEvaluator(settings(case.debug, case.test))
                    assertEquals(expected, evaluator.evaluate(binary(operation, condition("debug"), condition("test"))))
                }
            }
        }
    }

    /** false AND 与 true OR 的决定性操作数都不能隐藏另一侧未知条件，左右位置分别覆盖。 */
    @TestFactory
    fun unknownConditionsRemainInvalidOnEitherSideOfDecisiveOperands(): List<DynamicTest> =
        listOf(CfirBinaryOpKind.AND, CfirBinaryOpKind.OR).flatMap { operation ->
            listOf(false, true).map { unknownOnLeft ->
                dynamicTest("$operation unknown condition on ${if (unknownOnLeft) "left" else "right"}") {
                    val evaluator = CfirConditionalCompilationEvaluator(settings(debug = false, test = true))
                    val decisive = condition(if (operation == CfirBinaryOpKind.AND) "debug" else "test")
                    val unknown = buildComparisonExpression {
                        this.operation = CfirComparisonOp.EQ
                        left = condition("not_configured")
                        right = buildLiteralExpression {
                            kind = CfirLiteralKind.STRING
                            value = "value"
                        }
                    }
                    val expression = if (unknownOnLeft) binary(operation, unknown, decisive) else binary(operation, decisive, unknown)
                    val result = assertInstanceOf(CfirConditionalCompilationResult.Invalid::class.java, evaluator.evaluate(expression))
                    assertEquals(CfirConditionalCompilationError.UNKNOWN_CONDITION, result.reason)
                }
            }
        }

    /** raw 条件由真实 qualified-access 节点表示，不把任意 Bool literal 当成 @When 条件。 */
    private fun condition(conditionName: String): CfirQualifiedAccessExpression = buildNamedAccessExpression {
        calleeReference = buildNamedReference { name = Name.identifier(conditionName) }
    }

    private fun binary(operation: CfirBinaryOpKind, first: CfirExpression, second: CfirExpression): CfirExpression = buildBinaryOp {
        kind = operation
        left = first
        right = second
    }

    private fun settings(debug: Boolean, test: Boolean): ExplicitCfirConditionalCompilationSettings =
        ExplicitCfirConditionalCompilationSettings(
            backend = "cjnative", arch = "x86_64", os = "Windows", cjcVersion = "1.1.3", debug = debug, test = test,
        )

    private data class TruthCase(
        val debug: Boolean,
        val test: Boolean,
        val andResult: CfirConditionalCompilationResult,
        val orResult: CfirConditionalCompilationResult,
    )
}
