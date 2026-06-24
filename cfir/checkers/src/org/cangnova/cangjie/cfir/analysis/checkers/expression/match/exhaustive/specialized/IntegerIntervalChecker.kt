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

/** 针对整数 primitive 的区间覆盖穷尽性 checker。 */
class IntegerIntervalChecker : ExhaustivenessChecker {
    /** 当前 checker 的来源标记。 */
    override val source: CheckSource = CheckSource.INTEGER_INTERVAL
    /** 整数区间 checker 的调度优先级。 */
    override val priority: Int = 30

    /** 仅处理整数 primitive 类型。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean = type is ConePrimitiveType && type.kind.isInteger

    /** 通过常量区间合并和 gap 查找判断整数模式是否穷尽。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        val primitive = type as? ConePrimitiveType ?: return ExhaustivenessResult.Skipped
        if (!primitive.kind.isInteger) return ExhaustivenessResult.Skipped
        val typeRange = getTypeRange(primitive.kind) ?: return ExhaustivenessResult.Skipped

        val intervals = mutableListOf<LongRange>()
        var hasWildcard = false

        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue
            when (val kind = pattern.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> hasWildcard = true
                is CfirMatchPatternKind.Const -> {
                    val longValue = toLong(kind.value)
                    if (longValue != null) intervals += longValue..longValue
                }
                else -> Unit
            }
            if (hasWildcard) return ExhaustivenessResult.Exhaustive
        }

        if (hasWildcard) return ExhaustivenessResult.Exhaustive

        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged, typeRange)

        return if (gaps.isEmpty()) {
            ExhaustivenessResult.Exhaustive
        } else {
            val missing = collectMissingPatterns(type, gaps, maxPatterns = 5)
            ExhaustivenessResult.NonExhaustive(missing, source)
        }
    }

    /** 返回 primitive 整数类型对应的可表达区间。 */
    private fun getTypeRange(kind: PrimitiveTypeKind): LongRange? = when (kind) {
        PrimitiveTypeKind.INT8 -> Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
        PrimitiveTypeKind.INT16 -> Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()
        PrimitiveTypeKind.INT32 -> Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
        PrimitiveTypeKind.INT64, PrimitiveTypeKind.INT_NATIVE -> Long.MIN_VALUE..Long.MAX_VALUE
        PrimitiveTypeKind.UINT8 -> 0L..255L
        PrimitiveTypeKind.UINT16 -> 0L..65535L
        PrimitiveTypeKind.UINT32 -> 0L..4294967295L
        PrimitiveTypeKind.UINT64, PrimitiveTypeKind.UINT_NATIVE -> 0L..Long.MAX_VALUE
        PrimitiveTypeKind.IDEAL_INT -> Long.MIN_VALUE..Long.MAX_VALUE
        else -> null
    }

    /** 将整数常量模型转换为 Long 值；非整数常量返回空。 */
    private fun toLong(value: CfirConstantValue): Long? = when (value) {
        is CfirConstantValue.SignedIntConst -> value.value
        is CfirConstantValue.UnsignedIntConst -> value.value.toLong()
        else -> null
    }

    /** 合并排序后的整数区间，重叠或相邻区间会折叠为一个区间。 */
    private fun mergeIntervals(intervals: List<LongRange>): List<LongRange> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val result = mutableListOf<LongRange>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            val areAdjacentOrOverlap = if (current.last == Long.MAX_VALUE) {
                true
            } else {
                next.first <= current.last + 1
            }
            if (areAdjacentOrOverlap) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                result += current
                current = next
            }
        }
        result += current
        return result
    }

    /** 在类型全集区间中找出当前已覆盖区间之间的缺口。 */
    private fun findGaps(intervals: List<LongRange>, typeRange: LongRange): List<LongRange> {
        if (intervals.isEmpty()) return listOf(typeRange)
        val gaps = mutableListOf<LongRange>()
        if (intervals.first().first > typeRange.first) {
            gaps += typeRange.first until intervals.first().first
        }
        for (i in 0 until intervals.size - 1) {
            val gapStart = if (intervals[i].last == Long.MAX_VALUE) {
                continue
            } else {
                intervals[i].last + 1
            }
            val gapEnd = intervals[i + 1].first - 1
            if (gapStart <= gapEnd) gaps += gapStart..gapEnd
        }
        if (intervals.last().last < typeRange.last) {
            val tailStart = intervals.last().last + 1
            gaps += tailStart..typeRange.last
        }
        return gaps
    }

    /** 从缺口区间采样少量缺失整数模式，限制诊断展示数量。 */
    private fun collectMissingPatterns(
        type: ConeCangJieType,
        gaps: List<LongRange>,
        maxPatterns: Int,
    ): List<CfirMatchPattern> {
        if (maxPatterns <= 0) return emptyList()
        val result = ArrayList<CfirMatchPattern>(maxPatterns)
        for (gap in gaps) {
            if (result.size >= maxPatterns) break
            val sampledValues = sampleValues(gap)
            for (value in sampledValues) {
                if (result.size >= maxPatterns) break
                result += createIntegerPattern(type, value)
            }
        }
        return result
    }

    /** 对缺口区间采样端点或全部小范围值。 */
    private fun sampleValues(gap: LongRange): LongArray {
        if (gap.first > gap.last) return LongArray(0)
        return if (safeSpanGreaterThan(gap, 10L)) {
            if (gap.first == gap.last) {
                longArrayOf(gap.first)
            } else {
                longArrayOf(gap.first, gap.last)
            }
        } else {
            val size = (gap.last - gap.first + 1).toInt()
            LongArray(size) { index -> gap.first + index }
        }
    }

    /** 判断区间长度是否大于阈值，溢出时按大区间处理。 */
    private fun safeSpanGreaterThan(range: LongRange, limit: Long): Boolean {
        val span = safeSpan(range) ?: return true
        return span > limit
    }

    /** 安全计算闭区间长度；溢出或非法区间返回空。 */
    private fun safeSpan(range: LongRange): Long? {
        val diff = range.last - range.first
        if (range.last >= range.first && diff < 0) return null
        val span = diff + 1
        if (span <= 0) return null
        return span
    }

    /** 根据 primitive 有无符号性创建缺失整数常量模式。 */
    private fun createIntegerPattern(type: ConeCangJieType, value: Long): CfirMatchPattern {
        val const = when ((type as? ConePrimitiveType)?.kind) {
            PrimitiveTypeKind.UINT8,
            PrimitiveTypeKind.UINT16,
            PrimitiveTypeKind.UINT32,
            PrimitiveTypeKind.UINT64,
            PrimitiveTypeKind.UINT_NATIVE,
            -> CfirConstantValue.UnsignedIntConst(value.toULong())
            else -> CfirConstantValue.SignedIntConst(value)
        }
        return CfirMatchPattern(type, CfirMatchPatternKind.Const(const), null)
    }

    companion object {
        /** 默认整数区间 checker 单例。 */
        val INSTANCE = IntegerIntervalChecker()
    }
}
