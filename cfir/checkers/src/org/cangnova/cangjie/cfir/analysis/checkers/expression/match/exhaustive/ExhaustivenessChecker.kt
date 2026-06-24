package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/** 单个 match 穷尽性算法实现的统一接口。 */
interface ExhaustivenessChecker {
    /** 对给定模式矩阵和 subject 类型执行穷尽性检查。 */
    fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
    ): ExhaustivenessResult

    /** 判断当前 checker 是否适合处理这组类型和模式。 */
    fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean

    /** 当前 checker 的结果来源，用于诊断和调度优先级。 */
    val source: CheckSource

    /** 当前 checker 的调度优先级，数值越小越优先。 */
    val priority: Int
        get() = when (source) {
            CheckSource.TRIVIAL -> 0
            CheckSource.BOOLEAN_FLAG -> 10
            CheckSource.ENUM_BITVECTOR -> 20
            CheckSource.INTEGER_INTERVAL -> 30
            CheckSource.CHAR_INTERVAL -> 35
            CheckSource.TUPLE_COMPONENT -> 40
            CheckSource.NESTED_FLATTEN -> 50
            CheckSource.MARANGET -> 100
            else -> 1000
        }
}

/**
 * 一组 match pattern 的结构复杂度。
 *
 * 调度器根据复杂度决定是否使用轻量特化 checker，或升级到完整 Maranget 算法。
 */
data class PatternComplexity(
    /** 模式树最大嵌套深度。 */
    val maxNestingDepth: Int,
    /** 是否包含 or pattern。 */
    val hasOrPattern: Boolean,
    /** 是否包含 guard 条件。 */
    val hasGuard: Boolean,
    /** 是否包含 slice pattern。 */
    val hasSlicePattern: Boolean,
    /** 是否包含 range pattern。 */
    val hasRangePattern: Boolean,
    /** 当前集合中的模式总数。 */
    val totalPatterns: Int,
    /** 当前集合中出现的不同构造器数量。 */
    val distinctConstructors: Int,
) {
    /** 是否可以交给轻量 checker 快速处理。 */
    val isSimple: Boolean
        get() = maxNestingDepth <= 2 &&
            !hasOrPattern &&
            !hasGuard &&
            !hasSlicePattern

    /** 是否需要完整 Maranget usefulness 算法。 */
    val needsFullMaranget: Boolean
        get() = maxNestingDepth > 3 ||
            hasGuard ||
            hasSlicePattern ||
            (hasOrPattern && maxNestingDepth > 1)

    companion object {
        /** 从归一化 pattern 列表统计复杂度。 */
        fun analyze(patterns: List<CfirMatchPattern>): PatternComplexity {
            var maxDepth = 0
            var hasOr = false
            var hasGuard = false
            var hasSlice = false
            var hasRange = false
            val constructors = mutableSetOf<Any>()

            fun analyzePattern(pattern: CfirMatchPattern, depth: Int) {
                maxDepth = maxOf(maxDepth, depth)
                pattern.constructors.forEach { constructors.add(it) }
                when (val kind = pattern.kind) {
                    is org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind.Enum ->
                        kind.subPatterns.forEach { analyzePattern(it, depth + 1) }
                    is org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind.Tuple ->
                        kind.subPatterns.forEach { analyzePattern(it, depth + 1) }
                    else -> Unit
                }
            }

            patterns.forEach { analyzePattern(it, 1) }

            return PatternComplexity(
                maxNestingDepth = maxDepth,
                hasOrPattern = hasOr,
                hasGuard = hasGuard,
                hasSlicePattern = hasSlice,
                hasRangePattern = hasRange,
                totalPatterns = patterns.size,
                distinctConstructors = constructors.size,
            )
        }
    }
}
