package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
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
     * VArray 下标赋值需要在这里按元素类型处理，普通下标赋值仍由 operator set 调用解析负责；
     * 当 `this` / `super` 接收者参与不兼容赋值时额外报告接收者位置的类型不兼容诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
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
        }
        if (lValue is CfirQualifiedAccessExpression && CfirMutationTargetClassifier.isVArraySizeAccess(lValue)) return

        val lValueType = lValue.coneTypeOrNull ?: return
        val rValueType = expression.rValue.coneTypeOrNull ?: return
        if (lValueType is ConeErrorType || rValueType is ConeErrorType) return
        val rValueSource = expression.rValue.source as? AbstractCjSourceElement ?: return
        specificTypeMismatchDiagnostic(
            source = rValueSource,
            expectedType = lValueType,
            actualType = rValueType,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, rValueType, lValueType) != true) {
            val receiverSource = (lValue as? CfirQualifiedAccessExpression)
                ?.explicitReceiver
                ?.takeIf { receiver ->
                    receiver is CfirThisReceiverExpression || receiver is CfirSuperReceiverExpression
                }
                ?.source as? AbstractCjSourceElement
            if (receiverSource != null) {
                reporter.reportOn(
                    receiverSource.firstCharacterDiagnosticSource(),
                    CfirErrors.TYPE_INCOMPATIBLE,
                    "assignment expression",
                )
                reporter.reportOn(
                    rValueSource,
                    CfirErrors.TYPE_MISMATCH,
                    lValueType,
                    rValueType,
                    false,
                )
                return
            }

            reporter.reportOn(
                rValueSource,
                if (expression.rValue.isResolvedClassLikeValueReference()) {
                    CfirErrors.TYPE_MISMATCH
                } else {
                    CfirErrors.ASSIGNMENT_TYPE_MISMATCH
                },
                lValueType,
                rValueType,
                false,
            )
        }
    }
}
