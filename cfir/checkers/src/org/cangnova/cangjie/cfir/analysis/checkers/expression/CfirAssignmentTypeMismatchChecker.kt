package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAssignmentRhsRootValidity
import org.cangnova.cangjie.cfir.expressions.CfirAssignmentTypeMismatchPrimaryDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirAssignmentTypeMismatchOutcome
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchedTypesMultipleAssignError
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 赋值类型检查器。
 * 检查 `lValue = rValue` 中右值类型是否为左值类型的子类型。
 *
 * 普通 `a[i] = v` 仍由 operator set 调用解析验证参数类型。
 * VArray 下标是官方内建语义，不会解糖为用户 operator set，因此这里
 * 按元素类型检查右值。
 */
object CfirAssignmentTypeMismatchChecker : CfirAssignmentChecker() {
    /**
     * 检查赋值右值类型是否能赋给左值类型。
     *
     * VArray 下标赋值需要在这里按元素类型处理，普通下标赋值仍由 operator set 调用解析负责。
     * 普通赋值只消费 resolve 阶段写入的 assignment-local mismatch outcome，不再根据 receiver
     * 重新推导 expected-type 检查后的根有效性；RHS 语法仅交给共享分类器选择官方规定的专用诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        // 复合赋值在 AssignExpr precheck 层拥有专用 TYPE_INCOMPATIBLE 语义；普通赋值
        // checker 不能再把其解糖后的 operator 结果报告为 TYPE_MISMATCH。
        if (expression.augmentedOperation != null) return

        val rootDiagnostic = (expression.coneTypeOrNull as? ConeErrorType)?.diagnostic
        val multipleAssignmentDiagnostic = when (rootDiagnostic) {
            is ConeMismatchedTypesMultipleAssignError -> rootDiagnostic
            is ConeUnreportedDuplicateDiagnostic ->
                rootDiagnostic.original as? ConeMismatchedTypesMultipleAssignError
            else -> null
        }
        if (multipleAssignmentDiagnostic != null) {
            val rValueSource = expression.rValue.source as? AbstractCjSourceElement ?: return
            reporter.reportOn(
                rValueSource,
                CfirErrors.TYPE_MISMATCH,
                multipleAssignmentDiagnostic.expectedType,
                multipleAssignmentDiagnostic.actualType,
                false,
            )
            return
        }

        val lValue = expression.lValue
        if (lValue is CfirSubscriptExpression) {
            val receiverType = lValue.receiver.coneTypeOrNull
                ?.fullyExpandedType(context.session)
            if (receiverType !is ConeVArrayType) return
            checkVArraySubscriptAssignment(expression, lValue)
            return
        }
        if (lValue is CfirQualifiedAccessExpression && CfirMutationTargetClassifier.isVArraySizeAccess(lValue)) return

        val targetType = lValue.coneTypeOrNull.takeUnless { lValue is CfirTupleLiteral }
        if (targetType != null && checkTargetTypedExpression(expression.rValue, targetType).isHandled) return

        val outcome = expression.typeMismatchOutcome ?: return
        reportOrdinaryAssignmentMismatch(expression, outcome)
    }

    /**
     * 消费 resolve 固化的普通赋值 mismatch 结果并渲染诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportOrdinaryAssignmentMismatch(
        expression: CfirAssignment,
        outcome: CfirAssignmentTypeMismatchOutcome,
    ) {
        val rValueSource = expression.rValue.source as? AbstractCjSourceElement ?: return
        when (val primaryDiagnostic = outcome.primaryDiagnostic) {
            is CfirAssignmentTypeMismatchPrimaryDiagnostic.CannotConvertLiteral -> {
                reporter.reportOn(
                    rValueSource,
                    CfirErrors.CANNOT_CONVERT_LITERAL,
                    primaryDiagnostic.literalDescription,
                    outcome.expectedType,
                )
                return
            }

            CfirAssignmentTypeMismatchPrimaryDiagnostic.TypeMismatch -> Unit
        }

        specificTypeMismatchDiagnostic(
            source = rValueSource,
            expectedType = outcome.expectedType,
            actualType = outcome.actualType,
            expression = expression.rValue,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (outcome.rhsRootValidity == CfirAssignmentRhsRootValidity.VALID_AFTER_MISMATCH) {
            val assignmentSource = expression.source as? AbstractCjSourceElement
            if (assignmentSource != null) {
                reporter.reportOn(
                    assignmentSource.firstCharacterDiagnosticSource(),
                    CfirErrors.TYPE_INCOMPATIBLE,
                    "assignment expression",
                )
            }
        }

        reporter.reportOn(
            rValueSource,
            CfirErrors.TYPE_MISMATCH,
            outcome.expectedType,
            outcome.actualType,
            false,
        )
    }

    /**
     * VArray 下标赋值不经过普通赋值 outcome，继续按内建元素类型完成独立检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVArraySubscriptAssignment(
        expression: CfirAssignment,
        lValue: CfirSubscriptExpression,
    ) {

        val lValueType = lValue.coneTypeOrNull ?: return
        val rValueType = expression.rValue.coneTypeOrNull ?: return
        if (lValueType is ConeErrorType || rValueType is ConeErrorType) return
        val rValueSource = expression.rValue.source as? AbstractCjSourceElement ?: return
        specificTypeMismatchDiagnostic(
            source = rValueSource,
            expectedType = lValueType,
            actualType = rValueType,
            expression = expression.rValue,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, rValueType, lValueType) != true) {
            reporter.reportOn(
                rValueSource,
                CfirErrors.ASSIGNMENT_TYPE_MISMATCH,
                lValueType,
                rValueType,
                false,
            )
        }
    }
}
