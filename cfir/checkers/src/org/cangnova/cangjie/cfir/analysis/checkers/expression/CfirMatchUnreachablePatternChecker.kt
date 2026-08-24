package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
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
            val branchRows = runCatching {
                branch.pattern.calculateMatrix(subjectType, context.session)
            }.getOrElse { emptyList() }
            val unreachable = branchRows.isNotEmpty() && branchRows.all { row ->
                row.isCoveredBy(previousRows, matchContext)
            }
            if (unreachable) {
                reporter.reportOn(
                    source = branch.pattern.source ?: branch.source,
                    factory = CfirErrors.UNREACHABLE_PATTERN,
                )
            }

            // 只有可达且无 guard 的模式才能构成后续 Sema 覆盖矩阵。
            if (branch.guard == null && !unreachable) {
                previousRows += branchRows
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
