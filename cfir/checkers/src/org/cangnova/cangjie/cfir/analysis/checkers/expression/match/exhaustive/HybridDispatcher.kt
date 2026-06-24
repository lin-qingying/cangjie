package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized.BooleanChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized.CharIntervalChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized.IntegerIntervalChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.specialized.SmallEnumBitVectorChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.structural.NestedFlattenChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.structural.TupleComponentChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.trivial.TrivialChecker
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 混合 match 穷尽性调度器。
 *
 * 调度器按 checker 优先级尝试特化算法，并在特化算法跳过时继续降级到后续 checker，
 * 最终通常由完整 Maranget checker 兜底。
 */
class HybridDispatcher private constructor(
    /** 已按优先级排序的 checker 列表。 */
    private val checkers: List<ExhaustivenessChecker>,
) {
    /** 对模式矩阵执行穷尽性检查并返回第一个非 skipped 结果。 */
    fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
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

    /** 返回第一项适用于当前输入的 checker，用于调试和分析。 */
    fun selectChecker(
        type: ConeCangJieType,
        patterns: List<org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern>,
        context: CheckerContext,
    ): ExhaustivenessChecker? = checkers.find { it.isApplicable(type, patterns, context) }

    /** 根据 subject 类型给出理论上最适合的 checker 来源。 */
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

    companion object {
        /** 创建包含全部特化 checker 和 Maranget 兜底的默认调度器。 */
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

        /** 创建只运行完整 Maranget 算法的调度器。 */
        fun createMarangetOnly(): HybridDispatcher = HybridDispatcher(listOf(MarangetChecker.INSTANCE))

        /** 从外部 checker 列表创建按 priority 排序的调度器。 */
        fun create(checkers: List<ExhaustivenessChecker>): HybridDispatcher =
            HybridDispatcher(checkers.sortedBy { it.priority })

        /** 默认全功能调度器单例。 */
        val DEFAULT: HybridDispatcher by lazy { createDefault() }
    }
}

/** 一次调度执行的分析结果，用于观察推荐 checker、实际 checker 和最终结果。 */
data class DispatchAnalysis(
    /** subject 类型文本。 */
    val typeName: String,
    /** 输入模式数量。 */
    val patternCount: Int,
    /** 输入模式复杂度。 */
    val complexity: PatternComplexity,
    /** 根据类型推荐的 checker 来源。 */
    val recommendedSource: CheckSource,
    /** 实际产生结果的 checker 来源；无法判定时为空。 */
    val actualSource: CheckSource?,
    /** 调度器返回的穷尽性结果。 */
    val result: ExhaustivenessResult,
)

/** 包装 [HybridDispatcher] 并额外返回调度分析信息的工具类。 */
class AnalyzingDispatcher(private val delegate: HybridDispatcher) {
    /** 执行检查并附带复杂度、推荐来源和实际来源。 */
    fun checkWithAnalysis(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
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

    companion object {
        /** 基于默认调度器的分析包装单例。 */
        val DEFAULT by lazy { AnalyzingDispatcher(HybridDispatcher.DEFAULT) }
    }
}
