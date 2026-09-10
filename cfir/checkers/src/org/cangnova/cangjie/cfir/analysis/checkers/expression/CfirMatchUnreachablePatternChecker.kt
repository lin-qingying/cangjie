package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.calculateMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.Usefulness
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * Sema 层的 match 模式覆盖检查器。
 *
 * 这里只回答“当前模式是否已被前序无 guard 模式覆盖”。它完全基于 Maranget matrix，
 * 不消费局部变量的运行时值、CFG 或常量流事实。由常量传播得到的分支不可达属于独立
 * CFA pass，必须在函数 CFG 上按真实 success/failure edge 判定。
 */
object CfirMatchUnreachablePatternChecker : CfirMatchExpressionChecker() {
    /**
     * 检查 selector-based match 分支是否被前序无 guard 分支完全覆盖。
     *
     * pattern 合法性已经失败时跳过，避免在错误 pattern 上继续运行 usefulness 算法。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return
        if (expression.hasPatternLegalityProblem(context)) return

        val matchContext = MatchExhaustivenessContext.fromSession(
            session = context.session,
            accessContext = context.accessContext(CfirAccessKind.EXTEND),
        )
        val previousRows = mutableListOf<List<CfirMatchPattern>>()

        for (branch in expression.branches) {
            val orPattern = branch.pattern as? CfirOrPattern
            val alternatives = orPattern?.alternatives ?: listOf(branch.pattern)
            // guard 不贡献后续 case 覆盖，但同组 OR 内仍需记录前一项的覆盖范围。
            val localRows = if (branch.guard == null) previousRows else previousRows.toMutableList()
            val unreachableAlternatives = mutableListOf<CfirPattern>()
            var hasReachableAlternative = false
            for (alternative in alternatives) {
                val rows = alternative.calculateMatrix(subjectType, context.session)
                val unreachable = rows.isNotEmpty() && rows.all { row -> row.isCoveredBy(localRows, matchContext) }
                if (unreachable) {
                    unreachableAlternatives += alternative
                } else {
                    hasReachableAlternative = true
                    localRows += rows
                }
            }
            if (!hasReachableAlternative) {
                // 官方对整组无 witness 的 OR 只报告完整模式一次。
                reporter.reportOn(
                    source = orPattern?.patternRangeSource() ?: branch.pattern.source,
                    factory = CfirErrors.UNREACHABLE_PATTERN,
                )
            } else {
                for (alternative in unreachableAlternatives) {
                    reporter.reportOn(alternative.source, CfirErrors.UNREACHABLE_PATTERN)
                }
            }
        }
    }

    /** 使用 Maranget usefulness 算法判断当前模式行是否被前序矩阵覆盖。 */
    private fun List<CfirMatchPattern>.isCoveredBy(
        previousRows: CfirMatrix,
        context: MatchExhaustivenessContext,
    ): Boolean {
        return MarangetChecker.INSTANCE.isUseful(
            matrix = previousRows,
            patterns = this,
            withWitness = false,
            context = context,
            isTopLevel = true,
        ) is Usefulness.Useless
    }
}
