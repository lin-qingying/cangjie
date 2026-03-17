package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

/**
 * 璧嬪€肩被鍨嬫鏌ュ櫒銆? *
 * 妫€鏌?`lValue = rValue` 涓彸鍊肩被鍨嬫槸鍚︿负宸﹀€肩被鍨嬬殑瀛愮被鍨嬨€? */
object CfirAssignmentTypeMismatchChecker : CfirAssignmentChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val lValueType = expression.lValue.coneTypeOrNull ?: return
        val rValueType = expression.rValue.coneTypeOrNull ?: return
        val rValueSource = expression.rValue.source as? AbstractCjSourceElement ?: return
        if (!CfirTypeCheckUtils.isSubtypeOf(rValueType, lValueType)) {
            reporter.reportOn(
                rValueSource, CfirErrors.ASSIGNMENT_TYPE_MISMATCH,
                lValueType,
                rValueType,
                false,
            )
        }
    }
}

