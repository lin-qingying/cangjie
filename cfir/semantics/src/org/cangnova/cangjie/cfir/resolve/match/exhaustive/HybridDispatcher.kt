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
 */
class HybridDispatcher private constructor(
    private val checkers: List<ExhaustivenessChecker>,
) {
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

    fun selectChecker(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessChecker? = checkers.find { it.isApplicable(type, patterns, context) }

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

        fun createMarangetOnly(): HybridDispatcher = HybridDispatcher(listOf(MarangetChecker.INSTANCE))

        fun create(checkers: List<ExhaustivenessChecker>): HybridDispatcher =
            HybridDispatcher(checkers.sortedBy { it.priority })

        val DEFAULT: HybridDispatcher by lazy { createDefault() }
    }
}

data class DispatchAnalysis(
    val typeName: String,
    val patternCount: Int,
    val complexity: PatternComplexity,
    val recommendedSource: CheckSource,
    val actualSource: CheckSource?,
    val result: ExhaustivenessResult,
)

class AnalyzingDispatcher(private val delegate: HybridDispatcher) {
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

    companion object {
        val DEFAULT by lazy { AnalyzingDispatcher(HybridDispatcher.DEFAULT) }
    }
}
