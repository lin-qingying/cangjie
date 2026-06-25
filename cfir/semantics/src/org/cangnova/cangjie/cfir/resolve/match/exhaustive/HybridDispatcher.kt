package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized.BooleanChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized.CharIntervalChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized.IntegerIntervalChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized.SmallEnumBitVectorChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.structural.NestedFlattenChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.structural.TupleComponentChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.trivial.TrivialChecker
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 穷尽性检查器分派器。
 *
 * 结构与 checkers 侧既有实现保持同构，按优先级选择 specialized/structural/maranget 检查器。
 *
 * @property checkers 按优先级排序后的候选检查器列表。
 */
class HybridDispatcher private constructor(
    /**
     * 按优先级排序后的候选检查器列表。
     */
    private val checkers: List<ExhaustivenessChecker>,
) {
    /**
     * 按优先级选择可用 checker 并执行穷尽性检查。
     */
    fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val patterns = matrix.mapNotNull { it.firstOrNull() }
        for (checker in checkers) {
            if (checker.isApplicable(type, patterns, context)) {
                val result = checker.check(matrix, type, context)
                if (result !is ExhaustivenessResult.Skipped) return result
            }
        }
        return ExhaustivenessResult.Error("cannot determine exhaustiveness: no applicable checker")
    }

    /**
     * 选择第一个适用于当前类型与模式集合的 checker。
     */
    fun selectChecker(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessChecker? = checkers.find { it.isApplicable(type, patterns, context) }

    /**
     * 根据目标类型推荐最合适的 checker 来源。
     */
    fun getRecommendedSource(type: ConeCangJieType): CheckSource {
        return when {
            type is ConePrimitiveType && type.kind == PrimitiveTypeKind.BOOLEAN -> CheckSource.BOOLEAN_FLAG
            type is ConeEnumType -> CheckSource.ENUM_BITVECTOR
            type is ConePrimitiveType && type.kind.isInteger -> CheckSource.INTEGER_INTERVAL
            type is ConePrimitiveType && type.kind == PrimitiveTypeKind.RUNE -> CheckSource.CHAR_INTERVAL
            type is ConeTupleType -> CheckSource.TUPLE_COMPONENT
            else -> CheckSource.MARANGET
        }
    }

    /**
     * 分派器构造工具。
     */
    companion object {
        /**
         * 创建默认混合分派器。
         */
        fun createDefault(): HybridDispatcher {
            return HybridDispatcher(
                listOf(
                    TrivialChecker.INSTANCE,
                    BooleanChecker.INSTANCE,
                    SmallEnumBitVectorChecker.INSTANCE,
                    IntegerIntervalChecker.INSTANCE,
                    CharIntervalChecker.INSTANCE,
                    TupleComponentChecker.INSTANCE,
                    NestedFlattenChecker.INSTANCE,
                    MarangetChecker.INSTANCE,
                )
            )
        }

        /**
         * 创建只使用 Maranget 通用算法的分派器。
         */
        fun createMarangetOnly(): HybridDispatcher = HybridDispatcher(listOf(MarangetChecker.INSTANCE))

        /**
         * 使用指定 checker 列表创建分派器。
         */
        fun create(checkers: List<ExhaustivenessChecker>): HybridDispatcher =
            HybridDispatcher(checkers.sortedBy { it.priority })

        /** 默认混合分派器。 */
        val DEFAULT: HybridDispatcher by lazy { createDefault() }
    }
}

/**
 * 分派分析结果。
 *
 * @property typeName 被分析类型文本。
 * @property patternCount 模式数量。
 * @property complexity 模式复杂度。
 * @property recommendedSource 按类型推荐的 checker。
 * @property actualSource 实际产出结果的 checker。
 * @property result 穷尽性检查结果。
 */
data class DispatchAnalysis(
    /**
     * 被分析类型文本。
     */
    val typeName: String,
    /**
     * 模式数量。
     */
    val patternCount: Int,
    /**
     * 模式复杂度。
     */
    val complexity: PatternComplexity,
    /**
     * 按类型推荐的 checker。
     */
    val recommendedSource: CheckSource,
    /**
     * 实际产出结果的 checker。
     */
    val actualSource: CheckSource?,
    /**
     * 穷尽性检查结果。
     */
    val result: ExhaustivenessResult,
)

/**
 * 带分派分析信息的 checker 调度器。
 *
 * @property delegate 实际执行检查的混合分派器。
 */
class AnalyzingDispatcher(private val delegate: HybridDispatcher) {
    /**
     * 执行穷尽性检查并返回分派分析信息。
     */
    fun checkWithAnalysis(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): DispatchAnalysis {
        val patterns = matrix.mapNotNull { it.firstOrNull() }
        val complexity = PatternComplexity.analyze(patterns)
        val recommended = delegate.getRecommendedSource(type)
        val result = delegate.check(matrix, type, context)
        val actual = when (result) {
            is ExhaustivenessResult.NonExhaustive -> result.source
            is ExhaustivenessResult.Exhaustive -> delegate.selectChecker(type, patterns, context)?.source
            else -> null
        }
        return DispatchAnalysis(
            typeName = type.toString(),
            patternCount = patterns.size,
            complexity = complexity,
            recommendedSource = recommended,
            actualSource = actual,
            result = result,
        )
    }

    /**
     * 分析调度器构造工具。
     */
    companion object {
        /** 默认分析调度器。 */
        val DEFAULT by lazy { AnalyzingDispatcher(HybridDispatcher.DEFAULT) }
    }
}
