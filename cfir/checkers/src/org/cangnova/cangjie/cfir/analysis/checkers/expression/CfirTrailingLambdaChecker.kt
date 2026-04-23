package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType

/**
 * trailing lambda 只能用于函数类型。
 *
 * 对齐 C++ DiagKind::sema_trailing_lambda_cannot_used_for_non_function:
 * 形如 `x { body }` 中 x 若是非函数类型的值，应报告 TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION。
 *
 * 依赖 CfirFunctionCall.hasTrailingLambda 字段（由 PsiRawCfirBuilder 根据 PSI lambdaArguments 设置）。
 */
object CfirTrailingLambdaChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        if (!expression.hasTrailingLambda) return
        val receiver = expression.explicitReceiver ?: return
        val type = receiver.coneTypeOrNull ?: return
        if (type is ConeErrorType) return
        if (type is ConeFunctionType) return
        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION,
            a = type,
        )
    }
}
