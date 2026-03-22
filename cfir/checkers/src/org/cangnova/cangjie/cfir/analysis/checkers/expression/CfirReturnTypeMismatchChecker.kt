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
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 函数返回类型检查器。
 * 检查 `return expr` 中 `expr` 的类型是否为所在函数返回类型的子类型。
 */
object CfirReturnTypeMismatchChecker : CfirReturnExpressionChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirReturnExpression) {
        val result = expression.result ?: return
        val source = result.source as? AbstractCjSourceElement ?: return
        val actualType = result.coneTypeOrNull ?: return
        if (actualType is ConeErrorType) return
        val containingFunction = context.findClosestDeclaration<CfirFunction>() ?: return
        val expectedTypeRef = containingFunction.returnTypeRef as? CfirResolvedTypeRef ?: return
        val expectedType = expectedTypeRef.coneType
        if (expectedType is ConeErrorType) return
        if (!CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType, context.session)) {
            reporter.reportOn(
                source, CfirErrors.RETURN_TYPE_MISMATCH,
                expectedType,
                actualType,
                false,
            )
        }
    }
}
