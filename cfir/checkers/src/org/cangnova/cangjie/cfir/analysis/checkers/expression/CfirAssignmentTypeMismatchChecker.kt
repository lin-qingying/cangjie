package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.ConeErrorType
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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
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
            reporter.reportOn(
                rValueSource, CfirErrors.ASSIGNMENT_TYPE_MISMATCH,
                lValueType,
                rValueType,
                false,
            )
        }
    }
}
