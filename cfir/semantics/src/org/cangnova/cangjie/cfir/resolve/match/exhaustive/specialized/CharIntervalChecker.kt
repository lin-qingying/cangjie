package org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized

import org.cangnova.cangjie.cfir.resolve.match.CfirConstantValue
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * Rune 类型的 Unicode 区间穷尽性检查器。
 *
 * 该 checker 跳过 surrogate 区间，只在合法 Unicode code point 范围内检查缺口。
 */
class CharIntervalChecker : ExhaustivenessChecker {
    /** 当前 checker 来源。 */
    override val source: CheckSource = CheckSource.CHAR_INTERVAL

    /** 当前 checker 优先级。 */
    override val priority: Int = 35

    /** Unicode code point 下界。 */
    private val unicodeMin = 0

    /** Unicode code point 上界。 */
    private val unicodeMax = 0x10FFFF

    /** surrogate 区间起点。 */
    private val surrogateStart = 0xD800

    /** surrogate 区间终点。 */
    private val surrogateEnd = 0xDFFF

    /** Rune primitive 类型适用该 checker。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.RUNE

    /** 执行 Rune 区间覆盖检查。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        if (!isApplicable(type, emptyList(), context)) return ExhaustivenessResult.Skipped

        val intervals = mutableListOf<IntRange>()
        var hasWildcard = false

        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue
            when (val kind = pattern.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> hasWildcard = true
                is CfirMatchPatternKind.Const -> {
                    val rune = kind.value as? CfirConstantValue.RuneConst
                    if (rune != null) intervals += rune.value..rune.value
                }

                else -> Unit
            }
            if (hasWildcard) return ExhaustivenessResult.Exhaustive
        }

        if (hasWildcard) return ExhaustivenessResult.Exhaustive

        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged)
        return if (gaps.isEmpty()) {
            ExhaustivenessResult.Exhaustive
        } else {
            ExhaustivenessResult.NonExhaustive(listOf(CfirMatchPattern.wild(type)), source)
        }
    }

    /** 合并相交或相邻 Rune 区间。 */
    private fun mergeIntervals(intervals: List<IntRange>): List<IntRange> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val result = mutableListOf<IntRange>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.last + 1) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                result += current
                current = next
            }
        }
        result += current
        return result
    }

    /** 在合法 Unicode 范围内查找未覆盖区间。 */
    private fun findGaps(intervals: List<IntRange>): List<IntRange> {
        val validRanges = listOf(
            unicodeMin until surrogateStart,
            (surrogateEnd + 1)..unicodeMax,
        )
        val gaps = mutableListOf<IntRange>()
        for (validRange in validRanges) {
            var pos = validRange.first
            for (interval in intervals) {
                if (interval.last < validRange.first) continue
                if (interval.first > validRange.last) break
                val effectiveStart = maxOf(interval.first, validRange.first)
                val effectiveEnd = minOf(interval.last, validRange.last)
                if (pos < effectiveStart) gaps += pos until effectiveStart
                pos = effectiveEnd + 1
            }
            if (pos <= validRange.last) gaps += pos..validRange.last
        }
        return gaps
    }

    /** 单例实例。 */
    companion object {
        /** 默认 Rune 区间 checker 实例。 */
        val INSTANCE = CharIntervalChecker()
    }
}
