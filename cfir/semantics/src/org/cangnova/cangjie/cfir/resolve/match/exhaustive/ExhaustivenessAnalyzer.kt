/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.calculateMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.isWellTyped
import org.cangnova.cangjie.cfir.resolve.match.inferExpressionType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 仓颉 match 穷尽性共享分析入口。
 *
 * 对位原 checkers 侧 `ExhaustivenessAnalyzer`，但依赖 [MatchExhaustivenessContext]，
 * 从而可被 BODY_RESOLVE 与 CHECKERS 共同复用。
 */
object ExhaustivenessAnalyzer {
    private val dispatcher = HybridDispatcher.DEFAULT

    fun checkMatch(match: CfirMatchExpression, session: CfirSession): ExhaustivenessResult {
        return checkMatch(match, MatchExhaustivenessContext.fromSession(session))
    }

    fun checkMatch(
        match: CfirMatchExpression,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val subjectType = inferExpressionType(match.subject)
        if (subjectType is ConeErrorType) return ExhaustivenessResult.Skipped

        val matrix = try {
            match.calculateMatrix(subjectType, context.session)
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
        session: CfirSession,
    ): ExhaustivenessResult {
        return checkPattern(pattern, expression, MatchExhaustivenessContext.fromSession(session))
    }

    fun checkPattern(
        pattern: CfirPattern,
        expression: CfirExpression?,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        val type = inferExpressionType(expression)
        if (type is ConeErrorType) return ExhaustivenessResult.Skipped

        val matrix = try {
            pattern.calculateMatrix(type, context.session)
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
        type: ConeCangJieType,
        session: CfirSession,
    ): ExhaustivenessResult {
        return checkMatrix(matrix, type, MatchExhaustivenessContext.fromSession(session))
    }

    fun checkMatrix(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        if (type is ConeErrorType) return ExhaustivenessResult.Skipped
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("matrix is not well typed")
        }
        return dispatcher.check(matrix, type, context)
    }

    fun getMissingPatterns(
        match: CfirMatchExpression,
        context: MatchExhaustivenessContext,
    ): List<CfirMatchPattern>? {
        return when (val result = checkMatch(match, context)) {
            is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
            is ExhaustivenessResult.Exhaustive -> null
            else -> null
        }
    }

    fun getMissingPatternTexts(
        match: CfirMatchExpression,
        context: MatchExhaustivenessContext,
    ): List<String> {
        return when (val result = checkMatch(match, context)) {
            is ExhaustivenessResult.NonExhaustive -> result.getMissingPatternTexts()
            else -> emptyList()
        }
    }

    fun checkWithAnalysis(
        match: CfirMatchExpression,
        session: CfirSession,
    ): DispatchAnalysis? {
        return checkWithAnalysis(match, MatchExhaustivenessContext.fromSession(session))
    }

    fun checkWithAnalysis(
        match: CfirMatchExpression,
        context: MatchExhaustivenessContext,
    ): DispatchAnalysis? {
        val subjectType = inferExpressionType(match.subject)
        if (subjectType is ConeErrorType) return null

        val matrix = try {
            match.calculateMatrix(subjectType, context.session)
        } catch (_: Exception) {
            return null
        }
        return AnalyzingDispatcher.DEFAULT.checkWithAnalysis(matrix, subjectType, context)
    }
}
