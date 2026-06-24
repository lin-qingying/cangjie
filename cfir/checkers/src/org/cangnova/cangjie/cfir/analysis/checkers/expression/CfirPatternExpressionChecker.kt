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
import org.cangnova.cangjie.cfir.patterns.bindingOccurrences
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
    /**
     * 检查 match 表达式中所有 pattern 的 or-pattern 附加约束。
     */
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
                val firstBinding = pattern.bindingOccurrences().firstOrNull()
                if (firstBinding != null) {
                    reporter.reportOn(
                        source = firstBinding.source ?: pattern.source,
                        factory = CfirErrors.VAR_IN_OR_PATTERN,
                    )
                } else {
                    // 官方 ChkMatchCasePatterns 中变量绑定检查失败后会短路，不继续检查 kind 一致性。
                    checkOrPatternKindConsistency(pattern)
                }
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

        val firstKind = patternKindKey(alternatives.first())
        for (i in 1 until alternatives.size) {
            val altKind = patternKindKey(alternatives[i])
            if (altKind != firstKind) {
                reporter.reportOn(
                    source = alternatives[i].source,
                    factory = CfirErrors.DIFFERENT_OR_PATTERN,
                    a = "expected '${patternKindName(alternatives.first())}' but found '${patternKindName(alternatives[i])}'",
                )
            }
        }
    }

    /**
     * 取得 pattern 用于 or-pattern 同类比较的类别键。
     */
    private fun patternKindKey(pattern: CfirPattern): String = when (pattern) {
        is CfirEnumPattern,
        is CfirVarOrEnumPattern,
        -> "enum-or-variable"
        else -> patternKindName(pattern)
    }

    /**
     * 取得 pattern 类别的诊断展示名称。
     */
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
}

/**
 * 常量模式字符串插值检查器
 *
 * 对齐 C++ DiagKind::sema_interpolation_in_const_pattern:
 * match 的常量模式中不能使用字符串插值。
 */
//object CfirConstPatternInterpolationChecker : CfirMatchExpressionChecker() {
//    context(context: CheckerContext, reporter: DiagnosticReporter)
//    override fun check(expression: CfirMatchExpression) {
//        for (branch in expression.branches) {
//            checkConstPatternInterpolation(branch.pattern)
//        }
//    }
//
//    context(context: CheckerContext, reporter: DiagnosticReporter)
//    private fun checkConstPatternInterpolation(pattern: CfirPattern) {
//        when (pattern) {
//            is CfirConstPattern -> {
//                val expr = pattern.expression
//                if (expr is CfirLiteralExpression && expr.kind == CfirLiteralKind.StringInterpolation) {
//                    reporter.reportOn(
//                        source = pattern.source,
//                        factory = CfirErrors.INTERPOLATION_IN_CONST_PATTERN,
//                    )
//                }
//            }
//            is CfirOrPattern -> {
//                for (alt in pattern.alternatives) {
//                    checkConstPatternInterpolation(alt)
//                }
//            }
//            is CfirTuplePattern -> {
//                for (element in pattern.elements) {
//                    checkConstPatternInterpolation(element)
//                }
//            }
//            is CfirBindingPattern -> {
//                pattern.nestedPattern?.let { checkConstPatternInterpolation(it) }
//            }
//            else -> Unit
//        }
//    }
//}
