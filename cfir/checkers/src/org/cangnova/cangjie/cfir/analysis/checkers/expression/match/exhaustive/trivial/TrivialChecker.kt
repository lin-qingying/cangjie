package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.trivial

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/** 处理空矩阵和首层通配符这类最基础穷尽性情况的 checker。 */
class TrivialChecker : ExhaustivenessChecker {
    /** 当前 checker 在穷尽性结果中使用的来源标记。 */
    override val source: CheckSource = CheckSource.TRIVIAL

    /** 基础 checker 的最低调度优先级。 */
    override val priority: Int = 0

    /** 当模式为空或存在首层通配符时，该 checker 可以直接给出结果。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean {
        if (patterns.isEmpty()) return true
        return patterns.any(::isTopLevelWildcard)
    }

    /** 识别空矩阵的缺失通配符与已有首层通配符带来的直接穷尽结果。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        if (matrix.isEmpty()) {
            return ExhaustivenessResult.NonExhaustive(listOf(CfirMatchPattern.wild(type)), source)
        }

        val firstColumn = matrix.mapNotNull { it.firstOrNull() }
        val first = firstColumn.firstOrNull()
        if (first != null && isTopLevelWildcard(first)) return ExhaustivenessResult.Exhaustive
        if (firstColumn.any(::isTopLevelWildcard)) return ExhaustivenessResult.Exhaustive

        return ExhaustivenessResult.Skipped
    }

    /** 判断单个模式是否为首层可覆盖全集的通配符或绑定模式。 */
    private fun isTopLevelWildcard(pattern: CfirMatchPattern): Boolean {
        return when (pattern.kind) {
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
            else -> false
        }
    }

    /** 基础穷尽性 checker 的共享单例容器。 */
    companion object {
        /** 默认基础穷尽性 checker 单例。 */
        val INSTANCE = TrivialChecker()
    }
}
