package org.cangnova.cangjie.cfir.resolve.match.exhaustive.structural

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized.BooleanChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.specialized.SmallEnumBitVectorChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.trivial.TrivialChecker
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTupleType

class TupleComponentChecker(
    private val componentCheckers: List<ExhaustivenessChecker>,
) : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.TUPLE_COMPONENT
    override val priority: Int = 40

    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean {
        if (type !is ConeTupleType) return false
        return patterns.all { pattern ->
            when (val kind = pattern.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
                is CfirMatchPatternKind.Tuple -> kind.subPatterns.all(::isSimplePattern)
                else -> false
            }
        }
    }

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val tupleType = type as? ConeTupleType ?: return ExhaustivenessResult.Skipped
        val elementTypes = tupleType.elementTypes
        val arity = elementTypes.size

        if (arity == 0) {
            return if (matrix.isNotEmpty()) {
                ExhaustivenessResult.Exhaustive
            } else {
                ExhaustivenessResult.NonExhaustive(listOf(CfirMatchPattern.wild(type)), source)
            }
        }

        val columnPatterns = (0 until arity).map { col ->
            extractColumnPatterns(matrix, col, elementTypes[col])
        }

        if (!canAnalyzeIndependently(columnPatterns)) return ExhaustivenessResult.Skipped

        val columnResults = columnPatterns.mapIndexed { col, patterns ->
            val columnType = elementTypes[col]
            val columnMatrix = patterns.map { listOf(it) }
            checkColumn(columnMatrix, columnType, context)
        }

        return combineResults(columnResults, elementTypes, type)
    }

    private fun extractColumnPatterns(
        matrix: CfirMatrix,
        columnIndex: Int,
        elementType: ConeCangJieType,
    ): List<CfirMatchPattern> {
        return matrix.mapNotNull { row ->
            val first = row.firstOrNull() ?: return@mapNotNull null
            when (val kind = first.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> CfirMatchPattern.wild(elementType)
                is CfirMatchPatternKind.Tuple -> kind.subPatterns.getOrNull(columnIndex) ?: CfirMatchPattern.wild(elementType)
                else -> null
            }
        }
    }

    private fun canAnalyzeIndependently(columnPatterns: List<List<CfirMatchPattern>>): Boolean =
        columnPatterns.all { it.isNotEmpty() }

    private fun checkColumn(
        columnMatrix: CfirMatrix,
        columnType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val patterns = columnMatrix.flatten()
        for (checker in componentCheckers) {
            if (checker.isApplicable(columnType, patterns, context)) {
                val result = checker.check(columnMatrix, columnType, context)
                if (result !is ExhaustivenessResult.Skipped) return result
            }
        }
        return ExhaustivenessResult.Skipped
    }

    private fun combineResults(
        columnResults: List<ExhaustivenessResult>,
        elementTypes: List<ConeCangJieType>,
        tupleType: ConeCangJieType,
    ): ExhaustivenessResult {
        if (columnResults.any { it is ExhaustivenessResult.Skipped }) return ExhaustivenessResult.Skipped
        val error = columnResults.filterIsInstance<ExhaustivenessResult.Error>().firstOrNull()
        if (error != null) return error
        if (columnResults.all { it is ExhaustivenessResult.Exhaustive }) return ExhaustivenessResult.Exhaustive

        val missingByColumn = columnResults.mapIndexed { index, result ->
            when (result) {
                is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
                else -> listOf(CfirMatchPattern.wild(elementTypes[index]))
            }
        }
        val firstMissingTuple = missingByColumn.map { it.firstOrNull() ?: CfirMatchPattern.wild() }
        val missingTuple = CfirMatchPattern(tupleType, CfirMatchPatternKind.Tuple(firstMissingTuple), null)
        return ExhaustivenessResult.NonExhaustive(listOf(missingTuple), source)
    }

    private fun isSimplePattern(pattern: CfirMatchPattern): Boolean = when (val kind = pattern.kind) {
        CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
        is CfirMatchPatternKind.Const -> true
        is CfirMatchPatternKind.Enum -> kind.subPatterns.all(::isSimplePattern)
        is CfirMatchPatternKind.Type -> true
        else -> false
    }

    companion object {
        fun createDefault(): TupleComponentChecker {
            return TupleComponentChecker(
                listOf(
                    TrivialChecker.INSTANCE,
                    BooleanChecker.INSTANCE,
                    SmallEnumBitVectorChecker.INSTANCE,
                )
            )
        }

        val INSTANCE by lazy { createDefault() }
    }
}
