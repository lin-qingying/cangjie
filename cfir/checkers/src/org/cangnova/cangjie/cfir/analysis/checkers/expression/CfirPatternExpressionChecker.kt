package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 模式表达式补充检查器
 *
 * 对齐 C++ TypeCheckPattern.cpp 中以下语义：
 * - or-pattern 中不能引入变量绑定
 * - or-pattern 中的子模式必须是同类 pattern
 * - 常量模式中不能使用字符串插值
 */
object CfirOrPatternVariableChecker : CfirMatchExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return

        for (branch in expression.branches) {
            checkOrPatternConstraints(branch.pattern)
        }
    }

    /**
     * 递归检查 or-pattern 约束。
     *
     * 对齐 C++ DiagKind::sema_different_or_pattern:
     * or-pattern 中各子模式必须是同类（同为 enum / const / type 等）。
     *
     * 对齐 C++ DiagKind::sema_var_in_or_pattern:
     * or-pattern 中不允许引入变量绑定。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOrPatternConstraints(pattern: CfirPattern) {
        when (pattern) {
            is CfirOrPattern -> {
                // 检查变量引入
                for (alt in pattern.alternatives) {
                    if (containsBinding(alt)) {
                        reporter.reportOn(
                            source = alt.source,
                            factory = CfirErrors.VAR_IN_OR_PATTERN,
                        )
                    }
                }
                // 检查子模式类型一致性
                checkOrPatternKindConsistency(pattern)
                // 递归检查子模式
                for (alt in pattern.alternatives) {
                    checkOrPatternConstraints(alt)
                }
            }
            is CfirTuplePattern -> {
                for (element in pattern.elements) {
                    checkOrPatternConstraints(element)
                }
            }
            is CfirBindingPattern -> {
                pattern.nestedPattern?.let { checkOrPatternConstraints(it) }
            }
            else -> Unit
        }
    }

    /**
     * 检查 or-pattern 子模式类型一致性。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOrPatternKindConsistency(orPattern: CfirOrPattern) {
        val alternatives = orPattern.alternatives
        if (alternatives.size < 2) return

        val firstKind = patternKindName(alternatives.first())
        for (i in 1 until alternatives.size) {
            val altKind = patternKindName(alternatives[i])
            if (altKind != firstKind) {
                reporter.reportOn(
                    source = alternatives[i].source,
                    factory = CfirErrors.DIFFERENT_OR_PATTERN,
                    a = "expected '$firstKind' but found '$altKind'",
                )
            }
        }
    }

    private fun patternKindName(pattern: CfirPattern): String = when (pattern) {
        is CfirEnumPattern -> "enum"
        is CfirConstPattern -> "constant"
        is CfirTypePattern -> "type"
        is CfirTuplePattern -> "tuple"
        is CfirWildcardPattern -> "wildcard"
        is CfirBindingPattern -> "binding"
        is CfirExpressionPattern -> "expression"
        is CfirVarOrEnumPattern -> "variable"
        else -> "unknown"
    }

    private fun containsBinding(pattern: CfirPattern): Boolean = when (pattern) {
        is CfirBindingPattern -> true
        is CfirVarOrEnumPattern -> true
        is CfirTuplePattern -> pattern.elements.any { containsBinding(it) }
        is CfirOrPattern -> pattern.alternatives.any { containsBinding(it) }
        else -> false
    }
}

/**
 * 常量模式字符串插值检查器
 *
 * 对齐 C++ DiagKind::sema_interpolation_in_const_pattern:
 * match 的常量模式中不能使用字符串插值。
 */
object CfirConstPatternInterpolationChecker : CfirMatchExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        for (branch in expression.branches) {
            checkConstPatternInterpolation(branch.pattern)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConstPatternInterpolation(pattern: CfirPattern) {
        when (pattern) {
            is CfirConstPattern -> {
                val expr = pattern.expression
                if (expr is CfirLiteralExpression && expr.kind == CfirLiteralKind.StringInterpolation) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.INTERPOLATION_IN_CONST_PATTERN,
                    )
                }
            }
            is CfirOrPattern -> {
                for (alt in pattern.alternatives) {
                    checkConstPatternInterpolation(alt)
                }
            }
            is CfirTuplePattern -> {
                for (element in pattern.elements) {
                    checkConstPatternInterpolation(element)
                }
            }
            is CfirBindingPattern -> {
                pattern.nestedPattern?.let { checkConstPatternInterpolation(it) }
            }
            else -> Unit
        }
    }
}
