package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * 鍑芥暟杩斿洖绫诲瀷妫€鏌ュ櫒銆? *
 * 妫€鏌?`return expr` 涓?`expr` 鐨勭被鍨嬫槸鍚︿负鍖呭惈鍑芥暟澹版槑杩斿洖绫诲瀷鐨勫瓙绫诲瀷銆? */
object CfirReturnTypeMismatchChecker : CfirReturnExpressionChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirReturnExpression) {
        val result = expression.result ?: return
        val source = result.source as? AbstractCjSourceElement ?: return
        val actualType = result.coneTypeOrNull ?: return
        val containingFunction = context.findClosestDeclaration<CfirFunction>() ?: return
        val expectedTypeRef = containingFunction.returnTypeRef as? CfirResolvedTypeRef ?: return
        val expectedType = expectedTypeRef.coneType
        if (!CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType)) {
            reporter.reportOn(
                source, CfirErrors.RETURN_TYPE_MISMATCH,
                expectedType,
                actualType,
                false,
            )
        }
    }
}

