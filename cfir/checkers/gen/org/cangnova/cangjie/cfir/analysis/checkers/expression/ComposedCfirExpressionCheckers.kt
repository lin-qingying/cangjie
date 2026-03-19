

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedCfirExpressionCheckers(val predicate: (CfirCheckerWithDispatchKind) -> Boolean) : CfirExpressionCheckers() {
    constructor(dispatchKind: CheckerDispatchKind) : this({ it.dispatchKind == dispatchKind })

    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker>
        get() = _basicExpressionCheckers
    override val literalExpressionCheckers: Set<CfirLiteralExpressionChecker>
        get() = _literalExpressionCheckers
    override val functionCallCheckers: Set<CfirFunctionCallChecker>
        get() = _functionCallCheckers
    override val propertyAccessCheckers: Set<CfirPropertyAccessChecker>
        get() = _propertyAccessCheckers
    override val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker>
        get() = _qualifiedAccessCheckers
    override val assignmentCheckers: Set<CfirAssignmentChecker>
        get() = _assignmentCheckers
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
    override val jumpExpressionCheckers: Set<CfirJumpExpressionChecker>
        get() = _jumpExpressionCheckers
    override val rangeExpressionCheckers: Set<CfirRangeExpressionChecker>
        get() = _rangeExpressionCheckers
    override val subscriptExpressionCheckers: Set<CfirSubscriptExpressionChecker>
        get() = _subscriptExpressionCheckers
    override val errorExpressionCheckers: Set<CfirErrorExpressionChecker>
        get() = _errorExpressionCheckers

    private val _basicExpressionCheckers: MutableSet<CfirBasicExpressionChecker> = mutableSetOf()
    private val _literalExpressionCheckers: MutableSet<CfirLiteralExpressionChecker> = mutableSetOf()
    private val _functionCallCheckers: MutableSet<CfirFunctionCallChecker> = mutableSetOf()
    private val _propertyAccessCheckers: MutableSet<CfirPropertyAccessChecker> = mutableSetOf()
    private val _qualifiedAccessCheckers: MutableSet<CfirQualifiedAccessChecker> = mutableSetOf()
    private val _assignmentCheckers: MutableSet<CfirAssignmentChecker> = mutableSetOf()
    private val _binaryOpCheckers: MutableSet<CfirBinaryOpChecker> = mutableSetOf()
    private val _comparisonExpressionCheckers: MutableSet<CfirComparisonExpressionChecker> = mutableSetOf()
    private val _typeOperatorCheckers: MutableSet<CfirTypeOperatorChecker> = mutableSetOf()
    private val _ifExpressionCheckers: MutableSet<CfirIfExpressionChecker> = mutableSetOf()
    private val _matchExpressionCheckers: MutableSet<CfirMatchExpressionChecker> = mutableSetOf()
    private val _tryExpressionCheckers: MutableSet<CfirTryExpressionChecker> = mutableSetOf()
    private val _throwExpressionCheckers: MutableSet<CfirThrowExpressionChecker> = mutableSetOf()
    private val _returnExpressionCheckers: MutableSet<CfirReturnExpressionChecker> = mutableSetOf()
    private val _jumpExpressionCheckers: MutableSet<CfirJumpExpressionChecker> = mutableSetOf()
    private val _rangeExpressionCheckers: MutableSet<CfirRangeExpressionChecker> = mutableSetOf()
    private val _subscriptExpressionCheckers: MutableSet<CfirSubscriptExpressionChecker> = mutableSetOf()
    private val _errorExpressionCheckers: MutableSet<CfirErrorExpressionChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: CfirExpressionCheckers) {
        checkers.basicExpressionCheckers.filterTo(_basicExpressionCheckers, predicate)
        checkers.literalExpressionCheckers.filterTo(_literalExpressionCheckers, predicate)
        checkers.functionCallCheckers.filterTo(_functionCallCheckers, predicate)
        checkers.propertyAccessCheckers.filterTo(_propertyAccessCheckers, predicate)
        checkers.qualifiedAccessCheckers.filterTo(_qualifiedAccessCheckers, predicate)
        checkers.assignmentCheckers.filterTo(_assignmentCheckers, predicate)
        checkers.binaryOpCheckers.filterTo(_binaryOpCheckers, predicate)
        checkers.comparisonExpressionCheckers.filterTo(_comparisonExpressionCheckers, predicate)
        checkers.typeOperatorCheckers.filterTo(_typeOperatorCheckers, predicate)
        checkers.ifExpressionCheckers.filterTo(_ifExpressionCheckers, predicate)
        checkers.matchExpressionCheckers.filterTo(_matchExpressionCheckers, predicate)
        checkers.tryExpressionCheckers.filterTo(_tryExpressionCheckers, predicate)
        checkers.throwExpressionCheckers.filterTo(_throwExpressionCheckers, predicate)
        checkers.returnExpressionCheckers.filterTo(_returnExpressionCheckers, predicate)
        checkers.jumpExpressionCheckers.filterTo(_jumpExpressionCheckers, predicate)
        checkers.rangeExpressionCheckers.filterTo(_rangeExpressionCheckers, predicate)
        checkers.subscriptExpressionCheckers.filterTo(_subscriptExpressionCheckers, predicate)
        checkers.errorExpressionCheckers.filterTo(_errorExpressionCheckers, predicate)
    }
}
