package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.enumMatchTracker
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.reportEnumUsageInMatch
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * selector-based match 穷尽性检查器。
 *
 * 该检查器复用共享 [ExhaustivenessAnalyzer]，并把分析结果缓存回 match 表达式，避免后续阶段
 * 重复计算同一个 pattern matrix。
 */
object CfirMatchExhaustivenessChecker : CfirMatchExpressionChecker( ) {

    /**
     * 检查 match 表达式是否穷尽。
     *
     * subject 类型错误或 pattern 本身非法时跳过；非穷尽结果在 match 表达式位置报告缺失 case 文本。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val subject = expression.subject ?: return
        val subjectType = subject.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return
        if (expression.hasPatternLegalityProblem(context)) return

        context.session.enumMatchTracker?.reportEnumUsageInMatch(context.containingFilePath, subjectType)

        val exhaustiveness = checkerExhaustivenessStatus(expression, context)
        if (exhaustiveness !is CfirMatchExhaustivenessStatus.NonExhaustive) return
        reporter.reportOn(
            source,
            CfirErrors.NON_EXHAUSTIVE_MATCH,
            exhaustiveness.missingCaseTexts,
        )
    }

    /**
     * 获取或计算 match 穷尽性状态。
     *
     * 已缓存状态直接复用；新计算结果会转换为 CFIR 表达式上的 exhaustiveness 状态并写回节点。
     */
    private fun checkerExhaustivenessStatus(
        expression: CfirMatchExpression,
        context: CheckerContext,
    ): CfirMatchExhaustivenessStatus {
        val cached = expression.exhaustiveness
        if (cached !is CfirMatchExhaustivenessStatus.Unknown) {
            return cached
        }

        val resolved = when (val result = ExhaustivenessAnalyzer.checkMatch(expression, context.session)) {
            ExhaustivenessResult.Exhaustive -> CfirMatchExhaustivenessStatus.Exhaustive(
                source = CfirMatchExhaustivenessStatus.Source.Checker,
            )

            is ExhaustivenessResult.NonExhaustive -> CfirMatchExhaustivenessStatus.NonExhaustive(
                missingCaseTexts = result.getMissingPatternTexts(),
                source = CfirMatchExhaustivenessStatus.Source.Checker,
            )

            is ExhaustivenessResult.Error,
            ExhaustivenessResult.Skipped,
            -> CfirMatchExhaustivenessStatus.Unknown
        }
        expression.replaceExhaustiveness(resolved)
        return resolved
    }
}
