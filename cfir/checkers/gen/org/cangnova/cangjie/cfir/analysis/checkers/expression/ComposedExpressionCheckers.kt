

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker>
        get() = _basicExpressionCheckers
    override val literalExpressionCheckers: Set<CfirLiteralExpressionChecker>
        get() = _literalExpressionCheckers
    override val functionCallCheckers: Set<CfirFunctionCallChecker>
        get() = _functionCallCheckers
    override val namedAccessCheckers: Set<CfirNamedAccessChecker>
        get() = _namedAccessCheckers
    override val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker>
        get() = _qualifiedAccessCheckers
    override val superReceiverExpressionCheckers: Set<CfirSuperReceiverExpressionChecker>
        get() = _superReceiverExpressionCheckers
    override val assignmentCheckers: Set<CfirAssignmentChecker>
        get() = _assignmentCheckers
    override val incrementDecrementExpressionCheckers: Set<CfirIncrementDecrementExpressionChecker>
        get() = _incrementDecrementExpressionCheckers
    override val binaryOpCheckers: Set<CfirBinaryOpChecker>
        get() = _binaryOpCheckers
    override val comparisonExpressionCheckers: Set<CfirComparisonExpressionChecker>
        get() = _comparisonExpressionCheckers
    override val typeOperatorCheckers: Set<CfirTypeOperatorChecker>
        get() = _typeOperatorCheckers
    override val ifExpressionCheckers: Set<CfirIfExpressionChecker>
        get() = _ifExpressionCheckers
    override val matchExpressionCheckers: Set<CfirMatchExpressionChecker>
        get() = _matchExpressionCheckers
    override val tryExpressionCheckers: Set<CfirTryExpressionChecker>
        get() = _tryExpressionCheckers
    override val throwExpressionCheckers: Set<CfirThrowExpressionChecker>
        get() = _throwExpressionCheckers
    override val returnExpressionCheckers: Set<CfirReturnExpressionChecker>
        get() = _returnExpressionCheckers
    override val loopJumpCheckers: Set<CfirLoopJumpChecker>
        get() = _loopJumpCheckers
    override val rangeExpressionCheckers: Set<CfirRangeExpressionChecker>
        get() = _rangeExpressionCheckers
    override val subscriptExpressionCheckers: Set<CfirSubscriptExpressionChecker>
        get() = _subscriptExpressionCheckers
    override val errorExpressionCheckers: Set<CfirErrorExpressionChecker>
        get() = _errorExpressionCheckers

    private val _basicExpressionCheckers: MutableSet<CfirBasicExpressionChecker> = mutableSetOf()
    private val _literalExpressionCheckers: MutableSet<CfirLiteralExpressionChecker> = mutableSetOf()
    private val _functionCallCheckers: MutableSet<CfirFunctionCallChecker> = mutableSetOf()
    private val _namedAccessCheckers: MutableSet<CfirNamedAccessChecker> = mutableSetOf()
    private val _qualifiedAccessCheckers: MutableSet<CfirQualifiedAccessChecker> = mutableSetOf()
    private val _superReceiverExpressionCheckers: MutableSet<CfirSuperReceiverExpressionChecker> = mutableSetOf()
    private val _assignmentCheckers: MutableSet<CfirAssignmentChecker> = mutableSetOf()
    private val _incrementDecrementExpressionCheckers: MutableSet<CfirIncrementDecrementExpressionChecker> = mutableSetOf()
    private val _binaryOpCheckers: MutableSet<CfirBinaryOpChecker> = mutableSetOf()
    private val _comparisonExpressionCheckers: MutableSet<CfirComparisonExpressionChecker> = mutableSetOf()
    private val _typeOperatorCheckers: MutableSet<CfirTypeOperatorChecker> = mutableSetOf()
    private val _ifExpressionCheckers: MutableSet<CfirIfExpressionChecker> = mutableSetOf()
    private val _matchExpressionCheckers: MutableSet<CfirMatchExpressionChecker> = mutableSetOf()
    private val _tryExpressionCheckers: MutableSet<CfirTryExpressionChecker> = mutableSetOf()
    private val _throwExpressionCheckers: MutableSet<CfirThrowExpressionChecker> = mutableSetOf()
    private val _returnExpressionCheckers: MutableSet<CfirReturnExpressionChecker> = mutableSetOf()
    private val _loopJumpCheckers: MutableSet<CfirLoopJumpChecker> = mutableSetOf()
    private val _rangeExpressionCheckers: MutableSet<CfirRangeExpressionChecker> = mutableSetOf()
    private val _subscriptExpressionCheckers: MutableSet<CfirSubscriptExpressionChecker> = mutableSetOf()
    private val _errorExpressionCheckers: MutableSet<CfirErrorExpressionChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: ExpressionCheckers) {
        _basicExpressionCheckers.addAll(checkers.basicExpressionCheckers)
        _literalExpressionCheckers.addAll(checkers.literalExpressionCheckers)
        _functionCallCheckers.addAll(checkers.functionCallCheckers)
        _namedAccessCheckers.addAll(checkers.namedAccessCheckers)
        _qualifiedAccessCheckers.addAll(checkers.qualifiedAccessCheckers)
        _superReceiverExpressionCheckers.addAll(checkers.superReceiverExpressionCheckers)
        _assignmentCheckers.addAll(checkers.assignmentCheckers)
        _incrementDecrementExpressionCheckers.addAll(checkers.incrementDecrementExpressionCheckers)
        _binaryOpCheckers.addAll(checkers.binaryOpCheckers)
        _comparisonExpressionCheckers.addAll(checkers.comparisonExpressionCheckers)
        _typeOperatorCheckers.addAll(checkers.typeOperatorCheckers)
        _ifExpressionCheckers.addAll(checkers.ifExpressionCheckers)
        _matchExpressionCheckers.addAll(checkers.matchExpressionCheckers)
        _tryExpressionCheckers.addAll(checkers.tryExpressionCheckers)
        _throwExpressionCheckers.addAll(checkers.throwExpressionCheckers)
        _returnExpressionCheckers.addAll(checkers.returnExpressionCheckers)
        _loopJumpCheckers.addAll(checkers.loopJumpCheckers)
        _rangeExpressionCheckers.addAll(checkers.rangeExpressionCheckers)
        _subscriptExpressionCheckers.addAll(checkers.subscriptExpressionCheckers)
        _errorExpressionCheckers.addAll(checkers.errorExpressionCheckers)
    }
}
