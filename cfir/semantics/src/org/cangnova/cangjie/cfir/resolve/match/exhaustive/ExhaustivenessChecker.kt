package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * match 穷尽性子算法接口。
 *
 * 对位原 checkers 侧 `ExhaustivenessChecker`，但上下文降维为 [MatchExhaustivenessContext]，
 * 从而可在 resolve/checkers 间共享实现。
 */
interface ExhaustivenessChecker {
    /**
     * 对模式矩阵执行穷尽性检查。
     */
    fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult

    /**
     * 判断当前 checker 是否适用于给定类型与模式集合。
     */
    fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean

    /** 当前 checker 的来源标识。 */
    val source: CheckSource

    /** 当前 checker 在混合调度中的优先级。 */
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
 * 模式复杂度统计结果。
 *
 * @property maxNestingDepth 最大模式嵌套深度。
 * @property hasOrPattern 是否包含 or-pattern。
 * @property hasGuard 是否包含 guard。
 * @property hasSlicePattern 是否包含 slice 模式。
 * @property hasRangePattern 是否包含 range 模式。
 * @property totalPatterns 模式总数。
 * @property distinctConstructors 不同构造器数量。
 */
data class PatternComplexity(
    /**
     * 最大模式嵌套深度。
     */
    val maxNestingDepth: Int,
    /**
     * 是否包含 or-pattern。
     */
    val hasOrPattern: Boolean,
    /**
     * 是否包含 guard。
     */
    val hasGuard: Boolean,
    /**
     * 是否包含 slice 模式。
     */
    val hasSlicePattern: Boolean,
    /**
     * 是否包含 range 模式。
     */
    val hasRangePattern: Boolean,
    /**
     * 模式总数。
     */
    val totalPatterns: Int,
    /**
     * 不同构造器数量。
     */
    val distinctConstructors: Int,
) {
    /** 是否可由轻量专用 checker 处理。 */
    val isSimple: Boolean
        get() = maxNestingDepth <= 2 &&
            !hasOrPattern &&
            !hasGuard &&
            !hasSlicePattern

    /** 是否需要完整 Maranget 算法处理。 */
    val needsFullMaranget: Boolean
        get() = maxNestingDepth > 3 ||
            hasGuard ||
            hasSlicePattern ||
            (hasOrPattern && maxNestingDepth > 1)

    /**
     * 模式复杂度分析工具。
     */
    companion object {
        /**
         * 分析模式集合复杂度。
         */
        fun analyze(patterns: List<CfirMatchPattern>): PatternComplexity {
            var maxDepth = 0
            var hasOr = false
            var hasGuard = false
            var hasSlice = false
            var hasRange = false
            val constructors = mutableSetOf<Any>()

            /**
             * 递归分析单个模式。
             */
            fun analyzePattern(pattern: CfirMatchPattern, depth: Int) {
                maxDepth = maxOf(maxDepth, depth)
                pattern.constructors.forEach { constructors.add(it) }
                when (val kind = pattern.kind) {
                    is CfirMatchPatternKind.Enum ->
                        kind.subPatterns.forEach { analyzePattern(it, depth + 1) }
                    is CfirMatchPatternKind.Tuple ->
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
