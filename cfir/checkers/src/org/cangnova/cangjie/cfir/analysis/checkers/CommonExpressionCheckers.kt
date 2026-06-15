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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.*

object CommonExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker>
        get() = setOf(
            CfirLoopConditionTypeMismatchChecker,
            CfirSpawnSemanticsChecker,
            CfirExpressionWithErrorTypeChecker,
            CfirFunctionBodyTypeMismatchChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirFinalizerThisUsageChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirEffectsBasicChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirRangeSemanticsChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirQuoteImportChecker,
        )

    override val ifExpressionCheckers: Set<CfirIfExpressionChecker>
        get() = setOf(CfirIfConditionTypeMismatchChecker)

    override val errorExpressionCheckers: Set<CfirErrorExpressionChecker>
        get() = emptySet()

    override val matchExpressionCheckers: Set<CfirMatchExpressionChecker>
        get() = setOf(
            CfirMatchCaseTypeChecker,
            CfirMatchPatternLegalityChecker,
            CfirMatchExhaustivenessChecker,
            CfirOrPatternVariableChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchUnreachablePatternChecker,
        )

    override val assignmentCheckers: Set<CfirAssignmentChecker>
        get() = setOf(
            CfirCompoundAssignmentSemanticsChecker,
            CfirAssignmentLegalityChecker,
            CfirAssignmentTypeMismatchChecker,
            CfirImmutableFunctionCannotModifyFieldChecker,
        )

    override val incrementDecrementExpressionCheckers: Set<CfirIncrementDecrementExpressionChecker>
        get() = setOf(
            CfirIncrementDecrementLegalityChecker,
            CfirIncrementDecrementTypeChecker,
        )

    override val returnExpressionCheckers: Set<CfirReturnExpressionChecker>
        get() = setOf(
            CfirReturnLegalityChecker,
            CfirReturnTypeMismatchChecker,
        )

    override val literalExpressionCheckers: Set<CfirLiteralExpressionChecker>
        get() = setOf(
            CfirLiteralNumericOverflowChecker,
            CfirFloatLiteralRangeChecker,
        )

    override val functionCallCheckers: Set<CfirFunctionCallChecker>
        get() = setOf(
//            CfirArgumentTypeMismatchChecker,
            CfirSignedLiteralNumericOverflowChecker,
            CfirConstEvalArithmeticChecker,
            CfirConstructorDelegationCallChecker,
            CfirImmutableFunctionCannotAccessMutableFunctionChecker,
            CfirImmutableValueCannotAccessMutableFunctionChecker,
            CfirMockApiChecker,
            CfirDeprecatedCallChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTrailingLambdaChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirVArrayConstructorArgChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirInoutArgumentChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirApiLevelRefHigherChecker,
            CfirInoutSemanticsChecker,
        )

    override val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker>
        get() = setOf(
            CfirFunctionReferenceLegalityChecker,
            CfirGenericBareClassifierAccessChecker,
            CfirUpperBoundViolatedQualifiedAccessExpressionChecker,
            CfirClassifierAsExpressionChecker,
            CfirMutFuncReferenceChecker,
            CfirUnsafeFuncReferenceChecker,
        )

    override val superReceiverExpressionCheckers: Set<CfirSuperReceiverExpressionChecker>
        get() = setOf(CfirIllegalSuperReferenceChecker)

    override val tryExpressionCheckers: Set<CfirTryExpressionChecker>
        get() = setOf(
            CfirTryHandleReturnChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTryTargetTypeMismatchChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTryResourceTypeChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirCatchTypeChecker,
        )

    override val throwExpressionCheckers: Set<CfirThrowExpressionChecker>
        get() = setOf(
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirThrowExpressionTypeChecker,
        )

    override val subscriptExpressionCheckers: Set<CfirSubscriptExpressionChecker>
        get() = setOf(CfirSubscriptAssignmentChecker)
}
