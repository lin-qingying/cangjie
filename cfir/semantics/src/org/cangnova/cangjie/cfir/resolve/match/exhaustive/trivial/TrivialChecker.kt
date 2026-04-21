package org.cangnova.cangjie.cfir.resolve.match.exhaustive.trivial

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType

class TrivialChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.TRIVIAL
    override val priority: Int = 0

    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean {
        if (patterns.isEmpty()) return true
        return patterns.any(::isTopLevelWildcard)
    }

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
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

