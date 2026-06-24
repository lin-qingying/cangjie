package org.cangnova.cangjie.cfir.resolve.match.exhaustive.trivial

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 平凡穷尽性检查器。
 *
 * 空矩阵或顶层通配/绑定模式可以不进入复杂算法，直接产出结论或缺失通配模式。
 */
class TrivialChecker : ExhaustivenessChecker {
    /** 当前 checker 来源。 */
    override val source: CheckSource = CheckSource.TRIVIAL

    /** 当前 checker 优先级最高。 */
    override val priority: Int = 0

    /** 判断是否存在可由平凡规则处理的模式。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean {
        if (patterns.isEmpty()) return true
        return patterns.any(::isTopLevelWildcard)
    }

    /** 执行平凡穷尽性检查。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
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

    /** 判断模式是否为顶层通配或绑定模式。 */
    private fun isTopLevelWildcard(pattern: CfirMatchPattern): Boolean {
        return when (pattern.kind) {
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
            else -> false
        }
    }

    /** 单例实例。 */
    companion object {
        /** 默认平凡检查器实例。 */
        val INSTANCE = TrivialChecker()
    }
}
