package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.calculateMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.inferExpressionType
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria.isWellTyped
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangjieType

object ExhaustivenessAnalyzer {
    private val dispatcher = HybridDispatcher.DEFAULT

    fun checkMatch(
        match: CfirMatchExpression,
        context: CheckerContext,
    ): ExhaustivenessResult {
        val subjectType = inferExpressionType(match.subject)
        val matrix = try {
            match.calculateMatrix(subjectType)
        } catch (e: Exception) {
            return ExhaustivenessResult.Error("failed to build matrix: ${e.message}")
        }
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("matrix is not well typed")
        }
        return dispatcher.check(matrix, subjectType, context)
    }

    fun checkPattern(
        pattern: CfirPattern,
        expression: CfirExpression?,
        context: CheckerContext,
    ): ExhaustivenessResult {
        val type = inferExpressionType(expression)
        val matrix = try {
            pattern.calculateMatrix(type)
        } catch (e: Exception) {
            return ExhaustivenessResult.Error("failed to build matrix: ${e.message}")
        }
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("pattern matrix is not well typed")
        }
        return dispatcher.check(matrix, type, context)
    }

    fun checkMatrix(
        matrix: CfirMatrix,
        type: ConeCangjieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("matrix is not well typed")
        }
        return dispatcher.check(matrix, type, context)
    }

    fun getMissingPatterns(
        match: CfirMatchExpression,
        context: CheckerContext,
    ): List<CfirMatchPattern>? {
        return when (val result = checkMatch(match, context)) {
            is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
            is ExhaustivenessResult.Exhaustive -> null
            else -> null
        }
    }

    fun getMissingPatternTexts(
        match: CfirMatchExpression,
        context: CheckerContext,
    ): List<String> {
        return when (val result = checkMatch(match, context)) {
            is ExhaustivenessResult.NonExhaustive -> result.getMissingPatternTexts()
            else -> emptyList()
        }
    }

    fun checkWithAnalysis(
        match: CfirMatchExpression,
        context: CheckerContext,
    ): DispatchAnalysis? {
        val subjectType = inferExpressionType(match.subject)
        val matrix = try {
            match.calculateMatrix(subjectType)
        } catch (_: Exception) {
            return null
        }
        return AnalyzingDispatcher.DEFAULT.checkWithAnalysis(matrix, subjectType, context)
    }
}

