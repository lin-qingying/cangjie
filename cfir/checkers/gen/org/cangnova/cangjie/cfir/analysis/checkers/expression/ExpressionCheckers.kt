

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
