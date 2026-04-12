package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirAssignmentChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirAssignmentLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirAssignmentTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirConstEvalArithmeticChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirConstructorDelegationCallChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirErrorExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirGenericBareClassifierAccessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirIllegalSuperReferenceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirFunctionCallChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirFunctionReferenceLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirIfExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirIfConditionTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirLiteralExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirLiteralNumericOverflowChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirLoopConditionTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMockApiChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirImmutableFunctionCannotAccessMutableFunctionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirImmutableFunctionCannotModifyFieldChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchCaseTypeChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchPatternLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirQualifiedAccessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirReturnExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirReturnLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirReturnTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirSuperReceiverExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirFloatLiteralRangeChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMutFuncReferenceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirUnsafeFuncReferenceChecker

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
        get() = setOf(
            CfirMatchCaseTypeChecker,
            CfirMatchPatternLegalityChecker,
            CfirMatchExhaustivenessChecker,
        )

    override val assignmentCheckers: Set<CfirAssignmentChecker>
        get() = setOf(
            CfirAssignmentLegalityChecker,
            CfirAssignmentTypeMismatchChecker,
            CfirImmutableFunctionCannotModifyFieldChecker,
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
            CfirConstEvalArithmeticChecker,
            CfirConstructorDelegationCallChecker,
            CfirImmutableFunctionCannotAccessMutableFunctionChecker,
            CfirMockApiChecker,
        )

    override val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker>
        get() = setOf(
            CfirFunctionReferenceLegalityChecker,
            CfirGenericBareClassifierAccessChecker,
            CfirMutFuncReferenceChecker,
            CfirUnsafeFuncReferenceChecker,
        )

    override val superReceiverExpressionCheckers: Set<CfirSuperReceiverExpressionChecker>
        get() = setOf(CfirIllegalSuperReferenceChecker)
}
