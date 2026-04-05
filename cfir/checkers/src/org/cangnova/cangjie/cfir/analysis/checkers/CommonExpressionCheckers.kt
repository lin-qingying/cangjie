package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirAssignmentChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirAssignmentTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirConstEvalArithmeticChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirErrorExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirIllegalSuperReferenceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirFunctionCallChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirIfExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirIfConditionTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirLiteralExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirLiteralNumericOverflowChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirLoopConditionTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirQualifiedAccessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirReturnExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirReturnTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirSuperReceiverExpressionChecker

object CommonExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker>
        get() = setOf(
            CfirLoopConditionTypeMismatchChecker,
        )

    override val ifExpressionCheckers: Set<CfirIfExpressionChecker>
        get() = setOf(CfirIfConditionTypeMismatchChecker)

    override val errorExpressionCheckers: Set<CfirErrorExpressionChecker>
        get() = emptySet()

    override val matchExpressionCheckers: Set<CfirMatchExpressionChecker>
        get() = setOf(CfirMatchExhaustivenessChecker)

    override val assignmentCheckers: Set<CfirAssignmentChecker>
        get() = setOf(CfirAssignmentTypeMismatchChecker)

    override val returnExpressionCheckers: Set<CfirReturnExpressionChecker>
        get() = setOf(CfirReturnTypeMismatchChecker)

    override val literalExpressionCheckers: Set<CfirLiteralExpressionChecker>
        get() = setOf(CfirLiteralNumericOverflowChecker)

    override val functionCallCheckers: Set<CfirFunctionCallChecker>
        get() = setOf(
//            CfirArgumentTypeMismatchChecker,
            CfirConstEvalArithmeticChecker,
        )

    override val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker>
        get() = emptySet()

    override val superReceiverExpressionCheckers: Set<CfirSuperReceiverExpressionChecker>
        get() = setOf(CfirIllegalSuperReferenceChecker)
}
