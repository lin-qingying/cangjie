package org.cangnova.cangjie.cfir.resolve.conditional

import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationSettings
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationCatalog
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationError
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/** 条件表达式求值结果；非法条件不能静默降级为 false。 */
public sealed interface CfirConditionalCompilationResult {
    /** 条件成立，保留对应 AST 节点。 */
    public data object Enabled : CfirConditionalCompilationResult

    /** 条件不成立，在 raw-CFIR 裁剪阶段移除对应节点。 */
    public data object Disabled : CfirConditionalCompilationResult

    /** 条件表达式违反官方语法/运算符/值规则。 */
    public data class Invalid(
        public val reason: CfirConditionalCompilationError,
        public val conditionName: String? = null,
        public val rightValue: String? = null,
        public val operator: CfirConditionalCompilationOperator? = null,
    ) : CfirConditionalCompilationResult
}

/**
 * 后端中立的 `@When` 条件 evaluator。
 *
 * 它只读取显式注入的 [CfirConditionalCompilationSettings] 和 raw CFIR 表达式，
 * 不读取 PSI、普通符号解析结果或 JVM 宿主环境。节点裁剪和诊断上报由 pipeline
 * owner 负责，evaluator 只发布三态结果。
 */
public class CfirConditionalCompilationEvaluator(
    private val settings: CfirConditionalCompilationSettings,
) {
    private sealed interface Value {
        data class ConditionName(val name: String, val value: String) : Value
        data class StringValue(val value: String) : Value
        data class BooleanValue(val value: Boolean) : Value
    }

    /** 求值一个 `@When[...]` 条件表达式。 */
    public fun evaluate(expression: CfirExpression?): CfirConditionalCompilationResult {
        if (expression == null) return CfirConditionalCompilationResult.Invalid(CfirConditionalCompilationError.NO_CONDITION)
        return evaluateBoolean(expression)
    }

    private fun evaluateBoolean(expression: CfirExpression): CfirConditionalCompilationResult = when (expression) {
        is CfirNamedArgumentExpression -> evaluateBoolean(expression.expression)
        is CfirBinaryOp -> when (expression.kind) {
            CfirBinaryOpKind.AND -> combine(expression.left, expression.right, and = true)
            CfirBinaryOpKind.OR -> combine(expression.left, expression.right, and = false)
            else -> invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
        }
        is CfirComparisonExpression -> compare(
            expression.operation,
            expression.left,
            expression.right,
        )
        is CfirFunctionCall -> evaluateFunctionCall(expression)
        is CfirQualifiedAccessExpression -> evaluateBareCondition(expression)
        else -> invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
    }

    private fun combine(
        left: CfirExpression,
        right: CfirExpression,
        and: Boolean,
    ): CfirConditionalCompilationResult {
        val first = evaluateBoolean(left)
        val second = evaluateBoolean(right)
        // Official ConditionalCompilation evaluates both operands through
        // Utils::AllOf/AnyOf; a decisive left operand must not hide a malformed
        // right operand and its structured diagnostic.
        if (first is CfirConditionalCompilationResult.Invalid) return first
        if (second is CfirConditionalCompilationResult.Invalid) return second
        return if (and) {
            if (second === CfirConditionalCompilationResult.Enabled) {
                CfirConditionalCompilationResult.Enabled
            } else {
                CfirConditionalCompilationResult.Disabled
            }
        } else if (second === CfirConditionalCompilationResult.Enabled) {
            CfirConditionalCompilationResult.Enabled
        } else {
            CfirConditionalCompilationResult.Disabled
        }
    }

    private fun evaluateFunctionCall(call: CfirFunctionCall): CfirConditionalCompilationResult {
        val operator = (call.calleeReference as? CfirNamedReference)?.name ?: return invalid()
        if (operator == OperatorNameConventions.NOT) {
            val operand = call.explicitReceiver ?: call.argumentList.arguments.singleOrNull()
                ?: return invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
            val operandName = (operand.unwrapNamedArgument() as? CfirQualifiedAccessExpression)
                ?.let { it.calleeReference as? CfirNamedReference }
                ?.name
                ?.asString()
            if (operandName !in setOf("debug", "test")) {
                return invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
            }
            return when (val result = evaluateBoolean(operand)) {
                CfirConditionalCompilationResult.Enabled -> CfirConditionalCompilationResult.Disabled
                CfirConditionalCompilationResult.Disabled -> CfirConditionalCompilationResult.Enabled
                is CfirConditionalCompilationResult.Invalid -> result
            }
        }

        val operands = listOfNotNull(call.explicitReceiver) + call.argumentList.arguments
        if (operands.size != 2) return invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
        val comparison = when (operator) {
            OperatorNameConventions.EQUALS -> CfirComparisonOp.EQ
            OperatorNameConventions.NOT_EQUALS -> CfirComparisonOp.NE
            OperatorNameConventions.COMPARE_LT -> CfirComparisonOp.LT
            OperatorNameConventions.COMPARE_GT -> CfirComparisonOp.GT
            OperatorNameConventions.COMPARE_LTEQ -> CfirComparisonOp.LE
            OperatorNameConventions.COMPARE_GTEQ -> CfirComparisonOp.GE
            else -> return invalid(CfirConditionalCompilationError.UNSUPPORTED_OPERATOR)
        }
        return compare(comparison, operands[0], operands[1])
    }

    private fun evaluateBareCondition(expression: CfirQualifiedAccessExpression): CfirConditionalCompilationResult {
        val name = (expression.calleeReference as? CfirNamedReference)?.name?.asString()
            ?: return invalid()
        return when (name) {
            "debug" -> result(settings.debug)
            "test" -> result(settings.test)
            else -> invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
        }
    }

    private fun compare(
        operation: CfirComparisonOp,
        left: CfirExpression,
        right: CfirExpression,
    ): CfirConditionalCompilationResult {
        val conditionName = (left.unwrapNamedArgument() as? CfirQualifiedAccessExpression)
            ?.let { it.calleeReference as? CfirNamedReference }
            ?.name
            ?.asString()
            ?: return invalid(CfirConditionalCompilationError.INVALID_EXPRESSION)
        if (conditionName == "debug" || conditionName == "test") {
            return invalid(CfirConditionalCompilationError.UNSUPPORTED_OPERATOR)
        }

        val actual = conditionValue(conditionName)
            ?: return invalid(CfirConditionalCompilationError.UNKNOWN_CONDITION)
        val expected = (right.unwrapNamedArgument() as? CfirLiteralExpression)?.value as? String
            ?: return invalid(CfirConditionalCompilationError.INVALID_VALUE)
        if (conditionName == "cjc_version") {
            val actualVersion = parseVersion(actual) ?: return invalid(CfirConditionalCompilationError.INVALID_VERSION)
            val expectedVersion = parseVersion(expected) ?: return invalid(CfirConditionalCompilationError.INVALID_VERSION)
            return result(compareVersions(actualVersion, expectedVersion, operation))
        }
        val builtin = CfirConditionalCompilationCatalog.builtin(conditionName)
        val catalogOperator = operation.toCatalogOperator()
        if (builtin != null && catalogOperator !in builtin.operators) {
            return invalid(
                reason = CfirConditionalCompilationError.UNSUPPORTED_OPERATOR,
                conditionName = conditionName,
                operator = catalogOperator,
            )
        }
        val supportedValues = builtin?.supportedValues
        if (supportedValues != null && expected !in supportedValues) {
            return invalid(
                reason = CfirConditionalCompilationError.UNSUPPORTED_VALUE,
                conditionName = conditionName,
                rightValue = expected,
            )
        }
        return result(compareStrings(actual, expected, operation))
    }

    private fun conditionValue(name: String): String? = when (name) {
        "backend" -> settings.backend
        "arch" -> settings.arch
        "os" -> settings.os
        "cjc_version" -> settings.cjcVersion
        else -> settings.userDefined[name]
    }

    private fun parseVersion(value: String): List<Int>? {
        val parts = value.split('.')
        if (
            parts.size != 3 ||
            parts.any { part ->
                part.length !in 1..2 || part.any { ch -> !ch.isDigit() }
            }
        ) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    private fun compareVersions(left: List<Int>, right: List<Int>, operation: CfirComparisonOp): Boolean {
        val comparison = left.zip(right).firstNotNullOfOrNull { (a, b) -> (a - b).takeIf { it != 0 } } ?: 0
        return when (operation) {
            CfirComparisonOp.EQ -> comparison == 0
            CfirComparisonOp.NE -> comparison != 0
            CfirComparisonOp.LT -> comparison < 0
            CfirComparisonOp.GT -> comparison > 0
            CfirComparisonOp.LE -> comparison <= 0
            CfirComparisonOp.GE -> comparison >= 0
        }
    }

    private fun compareStrings(left: String, right: String, operation: CfirComparisonOp): Boolean = when (operation) {
        CfirComparisonOp.EQ -> left == right
        CfirComparisonOp.NE -> left != right
        CfirComparisonOp.LT -> left < right
        CfirComparisonOp.GT -> left > right
        CfirComparisonOp.LE -> left <= right
        CfirComparisonOp.GE -> left >= right
    }

    private fun result(value: Boolean): CfirConditionalCompilationResult =
        if (value) CfirConditionalCompilationResult.Enabled else CfirConditionalCompilationResult.Disabled

    private fun invalid(
        reason: CfirConditionalCompilationError = CfirConditionalCompilationError.INVALID_EXPRESSION,
        conditionName: String? = null,
        rightValue: String? = null,
        operator: CfirConditionalCompilationOperator? = null,
    ): CfirConditionalCompilationResult = CfirConditionalCompilationResult.Invalid(
        reason = reason,
        conditionName = conditionName,
        rightValue = rightValue,
        operator = operator,
    )

    private fun CfirComparisonOp.toCatalogOperator(): CfirConditionalCompilationOperator = when (this) {
        CfirComparisonOp.EQ -> CfirConditionalCompilationOperator.EQ
        CfirComparisonOp.NE -> CfirConditionalCompilationOperator.NE
        CfirComparisonOp.LT -> CfirConditionalCompilationOperator.LT
        CfirComparisonOp.GT -> CfirConditionalCompilationOperator.GT
        CfirComparisonOp.LE -> CfirConditionalCompilationOperator.LE
        CfirComparisonOp.GE -> CfirConditionalCompilationOperator.GE
    }

    private fun CfirExpression.unwrapNamedArgument(): CfirExpression =
        (this as? CfirNamedArgumentExpression)?.expression ?: this
}
