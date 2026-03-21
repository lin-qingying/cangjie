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

class CharIntervalChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.CHAR_INTERVAL
    override val priority: Int = 35

    private val unicodeMin = 0
    private val unicodeMax = 0x10FFFF
    private val surrogateStart = 0xD800
    private val surrogateEnd = 0xDFFF

    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean = type is ConePrimitiveType && type.kind == PrimitiveTypeKind.RUNE

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

    companion object {
        val INSTANCE = CharIntervalChecker()
    }
}

