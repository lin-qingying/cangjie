package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
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
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementOffsetStrategy
import org.cangnova.cangjie.source.fakeElement

/**
 * if/while 条件中由 `||` 连接的 let-pattern 不能引入变量。
 *
 * 官方编译器在条件层面报告 `sema_var_in_or_condition`；本项目在 if 条件中优先使用稳定的
 * binding source，while 与无法稳定定位单个 binding 的条件沿用整棵 `||` 条件 source。
 */
object CfirVarInOrConditionChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        when (expression) {
            is CfirIfExpression -> reportIllegalOrConditions(
                condition = expression.condition,
                preferBindingSource = true,
            )
            is CfirLoopExpression -> reportIllegalOrConditions(
                condition = expression.condition,
                preferBindingSource = false,
            )
            else -> return
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIllegalOrConditions(
        condition: CfirExpression,
        preferBindingSource: Boolean,
    ) {
        if (condition is CfirBinaryOp && condition.kind == CfirBinaryOpKind.OR && condition.containsPatternBinding()) {
            reporter.reportOn(
                source = condition.diagnosticSource(preferBindingSource),
                factory = CfirErrors.VAR_IN_OR_CONDITION,
            )
            return
        }
        when (condition) {
            is CfirBinaryOp -> {
                reportIllegalOrConditions(condition.left, preferBindingSource)
                reportIllegalOrConditions(condition.right, preferBindingSource)
            }
            else -> Unit
        }
    }

    private fun CfirBinaryOp.diagnosticSource(preferBindingSource: Boolean): CjSourceElement? =
        if (preferBindingSource) firstReportableBindingSource() ?: source else source

    private fun CfirExpression.containsPatternBinding(): Boolean = when (this) {
        is CfirLetPatternExpression -> pattern.containsIntroducedBinding()
        is CfirBinaryOp -> left.containsPatternBinding() || right.containsPatternBinding()
        else -> false
    }

    private fun CfirExpression.firstReportableBindingSource(): CjSourceElement? = when (this) {
        is CfirLetPatternExpression -> pattern.firstReportableBindingSource()
        is CfirBinaryOp -> left.firstReportableBindingSource() ?: right.firstReportableBindingSource()
        else -> null
    }

    private fun CfirPattern.containsIntroducedBinding(): Boolean =
        bindingOccurrences().isNotEmpty() || containsDeferredBindingName()

    private fun CfirPattern.firstReportableBindingSource(): CjSourceElement? = when (this) {
        is CfirBindingPattern -> source ?: nestedPattern?.firstReportableBindingSource()
        is CfirTypePattern -> bindingVariable?.source ?: source.takeIf { bindingName != null }
        is CfirVarOrEnumPattern -> bindingVariable?.source ?: source.takeIf {
            name.asString().firstOrNull()?.isLowerCase() == true
        }
        is CfirTuplePattern -> elements.firstNotNullOfOrNull { it.firstReportableBindingSource() }
        is CfirEnumPattern -> arguments.firstNotNullOfOrNull { it.firstReportableBindingSource() }
        is CfirOrPattern -> {
            if (alternatives.all { it.isBareDeferredBindingAlternative() }) {
                null
            } else {
                alternatives.firstNotNullOfOrNull { it.firstReportableBindingSource() }
            }
        }
        is CfirWildcardPattern,
        is CfirConstPattern,
        is CfirExpressionPattern,
        -> null
    }

    private fun CfirPattern.isBareDeferredBindingAlternative(): Boolean =
        this is CfirBindingPattern && nestedPattern == null ||
            this is CfirVarOrEnumPattern && bindingVariable != null && name.asString().firstOrNull()?.isLowerCase() == true

    private fun CfirPattern.containsDeferredBindingName(): Boolean = when (this) {
        is CfirBindingPattern -> true
        is CfirTypePattern -> bindingVariable != null || bindingName != null
        is CfirVarOrEnumPattern -> bindingVariable != null || name.asString().firstOrNull()?.isLowerCase() == true
        is CfirTuplePattern -> elements.any { it.containsIntroducedBinding() }
        is CfirEnumPattern -> arguments.any { it.containsIntroducedBinding() }
        is CfirOrPattern -> alternatives.any { it.containsIntroducedBinding() }
        is CfirWildcardPattern,
        is CfirConstPattern,
        is CfirExpressionPattern,
        -> false
    }
}

/**
 * let-condition 中的 or-pattern 约束检查。
 *
 * `||` 条件已经由 [CfirVarInOrConditionChecker] 报告变量引入错误，此处只在没有父级 `||`
 * 承载该错误时报告 `VAR_IN_OR_PATTERN`，并始终检查无变量绑定时的 pattern kind 一致性。
 */
object CfirLetConditionPatternChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirLetPatternExpression) return
        CfirOrPatternConstraintReporter.checkOrPatternConstraints(
            pattern = expression.pattern,
            reportVariableBindings = false,
            reportKindOnWholePattern = true,
        )
        val initializerType = expression.initializer.coneTypeOrNull ?: return
        if (initializerType is ConeErrorType) return
        CfirMatchPatternLegalityChecker.checkPattern(expression.pattern, initializerType)
    }
}

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
            CfirOrPatternConstraintReporter.checkOrPatternConstraints(
                pattern = branch.pattern,
                reportVariableBindings = true,
                reportKindOnWholePattern = false,
            )
        }
    }
}

private object CfirOrPatternConstraintReporter {
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
    fun checkOrPatternConstraints(
        pattern: CfirPattern,
        reportVariableBindings: Boolean,
        reportKindOnWholePattern: Boolean,
    ) {
        when (pattern) {
            is CfirOrPattern -> {
                val firstBinding = pattern.bindingOccurrences().firstOrNull()
                if (firstBinding != null && reportVariableBindings) {
                    reporter.reportOn(
                        source = firstBinding.source ?: pattern.source,
                        factory = CfirErrors.VAR_IN_OR_PATTERN,
                    )
                } else if (firstBinding == null) {
                    // 官方 ChkMatchCasePatterns 中变量绑定检查失败后会短路，不继续检查 kind 一致性。
                    checkOrPatternKindConsistency(pattern, reportKindOnWholePattern)
                }
                for (alt in pattern.alternatives) {
                    checkOrPatternConstraints(alt, reportVariableBindings, reportKindOnWholePattern)
                }
            }
            is CfirTuplePattern -> {
                for (element in pattern.elements) {
                    checkOrPatternConstraints(element, reportVariableBindings, reportKindOnWholePattern)
                }
            }
            is CfirEnumPattern -> {
                for (argument in pattern.arguments) {
                    checkOrPatternConstraints(argument, reportVariableBindings, reportKindOnWholePattern)
                }
            }
            is CfirBindingPattern -> {
                pattern.nestedPattern?.let {
                    checkOrPatternConstraints(it, reportVariableBindings, reportKindOnWholePattern)
                }
            }
            else -> Unit
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOrPatternKindConsistency(
        orPattern: CfirOrPattern,
        reportKindOnWholePattern: Boolean,
    ) {
        val alternatives = orPattern.alternatives
        if (alternatives.size < 2) return

        val firstKind = patternKindKey(alternatives.first())
        for (i in 1 until alternatives.size) {
            val altKind = patternKindKey(alternatives[i])
            if (altKind != firstKind) {
                reporter.reportOn(
                    source = if (reportKindOnWholePattern) {
                        orPattern.patternRangeSource() ?: orPattern.source
                    } else {
                        alternatives[i].source
                    },
                    factory = CfirErrors.DIFFERENT_OR_PATTERN,
                    a = "expected '${patternKindName(alternatives.first())}' but found '${patternKindName(alternatives[i])}'",
                )
            }
        }
    }

    /**
     * let-condition 的 `CfirOrPattern.source` 可能锚定整棵 `let` 表达式。
     * 诊断需要覆盖用户写下的 pattern 段，因此用首尾 alternative source 合成稳定范围。
     */
    private fun CfirOrPattern.patternRangeSource(): CjSourceElement? {
        val first = alternatives.firstOrNull()?.source ?: return null
        val last = alternatives.lastOrNull()?.source ?: return null
        if (first.startOffset >= last.endOffset) return first
        return first.fakeElement(
            CjFakeSourceElementKind.SyntheticCall,
            CjSourceElementOffsetStrategy.Custom.Initialized(
                startOffset = first.startOffset,
                endOffset = last.endOffset,
            ),
        )
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
