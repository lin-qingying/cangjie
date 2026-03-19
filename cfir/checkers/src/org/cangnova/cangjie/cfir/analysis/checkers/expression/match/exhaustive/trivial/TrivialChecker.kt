package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.trivial

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.types.ConeCangjieType

class TrivialChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.TRIVIAL
    override val priority: Int = 0

    override fun isApplicable(
        type: ConeCangjieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean {
        if (patterns.isEmpty()) return true
        return patterns.any(::isTopLevelWildcard)
    }

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangjieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        if (matrix.isEmpty()) {
            return ExhaustivenessResult.NonExhaustive(listOf(CfirMatchPattern.wild(type)), source)
        }

        val firstColumn = matrix.mapNotNull { it.firstOrNull() }
        val first = firstColumn.firstOrNull()
        if (first != null && isTopLevelWildcard(first)) return ExhaustivenessResult.Exhaustive
        if (firstColumn.any(::isTopLevelWildcard)) return ExhaustivenessResult.Exhaustive

        return ExhaustivenessResult.Skipped
    }

    private fun isTopLevelWildcard(pattern: CfirMatchPattern): Boolean {
        return when (pattern.kind) {
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
            else -> false
        }
    }

    companion object {
        val INSTANCE = TrivialChecker()
    }
}

