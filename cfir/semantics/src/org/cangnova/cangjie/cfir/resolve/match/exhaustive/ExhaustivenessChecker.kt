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
    fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult

    fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean

    val source: CheckSource

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

data class PatternComplexity(
    val maxNestingDepth: Int,
    val hasOrPattern: Boolean,
    val hasGuard: Boolean,
    val hasSlicePattern: Boolean,
    val hasRangePattern: Boolean,
    val totalPatterns: Int,
    val distinctConstructors: Int,
) {
    val isSimple: Boolean
        get() = maxNestingDepth <= 2 &&
            !hasOrPattern &&
            !hasGuard &&
            !hasSlicePattern

    val needsFullMaranget: Boolean
        get() = maxNestingDepth > 3 ||
            hasGuard ||
            hasSlicePattern ||
            (hasOrPattern && maxNestingDepth > 1)

    companion object {
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
