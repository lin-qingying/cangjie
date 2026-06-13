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



package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

@Suppress("UNCHECKED_CAST")
abstract class ExpressionCheckers {
    companion object {
        val EMPTY: ExpressionCheckers = object : ExpressionCheckers() {}
    }

    open val basicExpressionCheckers: Set<CfirBasicExpressionChecker> = emptySet()
    open val literalExpressionCheckers: Set<CfirLiteralExpressionChecker> = emptySet()
    open val functionCallCheckers: Set<CfirFunctionCallChecker> = emptySet()
    open val namedAccessCheckers: Set<CfirNamedAccessChecker> = emptySet()
    open val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker> = emptySet()
    open val superReceiverExpressionCheckers: Set<CfirSuperReceiverExpressionChecker> = emptySet()
    open val assignmentCheckers: Set<CfirAssignmentChecker> = emptySet()
    open val incrementDecrementExpressionCheckers: Set<CfirIncrementDecrementExpressionChecker> = emptySet()
    open val binaryOpCheckers: Set<CfirBinaryOpChecker> = emptySet()
    open val comparisonExpressionCheckers: Set<CfirComparisonExpressionChecker> = emptySet()
    open val typeOperatorCheckers: Set<CfirTypeOperatorChecker> = emptySet()
    open val ifExpressionCheckers: Set<CfirIfExpressionChecker> = emptySet()
    open val matchExpressionCheckers: Set<CfirMatchExpressionChecker> = emptySet()
    open val tryExpressionCheckers: Set<CfirTryExpressionChecker> = emptySet()
    open val throwExpressionCheckers: Set<CfirThrowExpressionChecker> = emptySet()
    open val returnExpressionCheckers: Set<CfirReturnExpressionChecker> = emptySet()
    open val loopJumpCheckers: Set<CfirLoopJumpChecker> = emptySet()
    open val rangeExpressionCheckers: Set<CfirRangeExpressionChecker> = emptySet()
    open val subscriptExpressionCheckers: Set<CfirSubscriptExpressionChecker> = emptySet()
    open val errorExpressionCheckers: Set<CfirErrorExpressionChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicExpressionCheckers: Array<CfirBasicExpressionChecker> by lazy { basicExpressionCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allLiteralExpressionCheckers: Array<CfirLiteralExpressionChecker> by lazy { (literalExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirLiteralExpressionChecker> }
    @CheckersComponentInternal internal val allFunctionCallCheckers: Array<CfirFunctionCallChecker> by lazy { (functionCallCheckers + qualifiedAccessCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirFunctionCallChecker> }
    @CheckersComponentInternal internal val allNamedAccessCheckers: Array<CfirNamedAccessChecker> by lazy { (namedAccessCheckers + qualifiedAccessCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirNamedAccessChecker> }
    @CheckersComponentInternal internal val allQualifiedAccessCheckers: Array<CfirQualifiedAccessChecker> by lazy { (qualifiedAccessCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirQualifiedAccessChecker> }
    @CheckersComponentInternal internal val allSuperReceiverExpressionCheckers: Array<CfirSuperReceiverExpressionChecker> by lazy { (superReceiverExpressionCheckers + qualifiedAccessCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirSuperReceiverExpressionChecker> }
    @CheckersComponentInternal internal val allAssignmentCheckers: Array<CfirAssignmentChecker> by lazy { (assignmentCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirAssignmentChecker> }
    @CheckersComponentInternal
    internal val allIncrementDecrementExpressionCheckers: Array<CfirIncrementDecrementExpressionChecker> by lazy { (incrementDecrementExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirIncrementDecrementExpressionChecker> }
    @CheckersComponentInternal internal val allBinaryOpCheckers: Array<CfirBinaryOpChecker> by lazy { (binaryOpCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirBinaryOpChecker> }
    @CheckersComponentInternal internal val allComparisonExpressionCheckers: Array<CfirComparisonExpressionChecker> by lazy { (comparisonExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirComparisonExpressionChecker> }
    @CheckersComponentInternal internal val allTypeOperatorCheckers: Array<CfirTypeOperatorChecker> by lazy { (typeOperatorCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirTypeOperatorChecker> }
    @CheckersComponentInternal internal val allIfExpressionCheckers: Array<CfirIfExpressionChecker> by lazy { (ifExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirIfExpressionChecker> }
    @CheckersComponentInternal internal val allMatchExpressionCheckers: Array<CfirMatchExpressionChecker> by lazy { (matchExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirMatchExpressionChecker> }
    @CheckersComponentInternal internal val allTryExpressionCheckers: Array<CfirTryExpressionChecker> by lazy { (tryExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirTryExpressionChecker> }
    @CheckersComponentInternal internal val allThrowExpressionCheckers: Array<CfirThrowExpressionChecker> by lazy { (throwExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirThrowExpressionChecker> }
    @CheckersComponentInternal internal val allReturnExpressionCheckers: Array<CfirReturnExpressionChecker> by lazy { (returnExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirReturnExpressionChecker> }
    @CheckersComponentInternal internal val allLoopJumpCheckers: Array<CfirLoopJumpChecker> by lazy { (loopJumpCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirLoopJumpChecker> }
    @CheckersComponentInternal internal val allRangeExpressionCheckers: Array<CfirRangeExpressionChecker> by lazy { (rangeExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirRangeExpressionChecker> }
    @CheckersComponentInternal internal val allSubscriptExpressionCheckers: Array<CfirSubscriptExpressionChecker> by lazy { (subscriptExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirSubscriptExpressionChecker> }
    @CheckersComponentInternal internal val allErrorExpressionCheckers: Array<CfirErrorExpressionChecker> by lazy { (errorExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<CfirErrorExpressionChecker> }
}
