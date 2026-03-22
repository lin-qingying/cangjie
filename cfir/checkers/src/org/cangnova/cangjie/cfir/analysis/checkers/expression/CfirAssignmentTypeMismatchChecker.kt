package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 赋值类型检查器。
 * 检查 `lValue = rValue` 中右值类型是否为左值类型的子类型。
 */
object CfirAssignmentTypeMismatchChecker : CfirAssignmentChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val lValueType = expression.lValue.coneTypeOrNull ?: return
        val rValueType = expression.rValue.coneTypeOrNull ?: return
        if (lValueType is ConeErrorType || rValueType is ConeErrorType) return
        val rValueSource = expression.rValue.source as? AbstractCjSourceElement ?: return
        if (!CfirTypeCheckUtils.isSubtypeOf(rValueType, lValueType, context.session)) {
            reporter.reportOn(
                rValueSource, CfirErrors.ASSIGNMENT_TYPE_MISMATCH,
                lValueType,
                rValueType,
                false,
            )
        }
    }
}

