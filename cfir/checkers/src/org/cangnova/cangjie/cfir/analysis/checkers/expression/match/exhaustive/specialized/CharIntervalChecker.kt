package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirConstantValue
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/** 针对 `Rune`/字符模式的 Unicode 区间穷尽性 checker。 */
class CharIntervalChecker : ExhaustivenessChecker {
    /** 当前 checker 在调度结果中使用的来源标记。 */
    override val source: CheckSource = CheckSource.CHAR_INTERVAL

    /** 字符区间 checker 的调度优先级，位于布尔、枚举和整数专项检查之后。 */
    override val priority: Int = 35

    /** Unicode 标量值空间的最小码点。 */
    private val unicodeMin = 0

    /** Unicode 标量值空间的最大码点。 */
    private val unicodeMax = 0x10FFFF

    /** UTF-16 代理区间起始码点，Rune 穷尽性检查需要排除该区间。 */
    private val surrogateStart = 0xD800

    /** UTF-16 代理区间结束码点，Rune 穷尽性检查需要排除该区间。 */
    private val surrogateEnd = 0xDFFF

    /** 仅对 primitive `RUNE` 类型启用字符区间穷尽性分析。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.RUNE

    /** 合并已出现的字符常量区间，并根据有效 Unicode 标量值区间判断是否仍存在缺口。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
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

    /** 对字符常量区间做排序合并，重叠或相邻区间会折叠为一个区间。 */
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

    /** 在排除 UTF-16 代理区的 Unicode 有效范围内查找未覆盖的字符区间。 */
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

    /** 字符区间 checker 的共享单例容器。 */
    companion object {
        /** 默认字符区间 checker 单例。 */
        val INSTANCE = CharIntervalChecker()
    }
}
