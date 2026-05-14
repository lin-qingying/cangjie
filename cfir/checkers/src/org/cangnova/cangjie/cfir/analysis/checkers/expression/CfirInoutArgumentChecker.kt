package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType

/**
 * INOUT 实参与 CFunc 形参匹配检查。
 *
 * 对齐 C++ DiagKind::sema_inout_mismatch (TypeCheckCall.cpp:2795):
 * 调用 CFunc 时 VArray 类型的实参必须通过 `inout` 传入;否则报错。
 */
object CfirInoutArgumentChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val ref = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val funcSymbol = ref.resolvedSymbol as? CfirFunctionSymbol<*> ?: return
        val calleeFunc = funcSymbol.cfir
        val calleeType = (calleeFunc.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        val isCFuncCall = calleeFunc.status.isForeign
            || (calleeType as? ConeFunctionType)?.isCFunc == true

        for (argument in expression.argumentList.arguments) {
            val unwrapped = unwrapArgument(argument)
            val argIsInout = unwrapped is CfirInoutArgumentExpression
            val argExpr = if (argIsInout) (unwrapped as CfirInoutArgumentExpression).expression else unwrapped
            val argType = argExpr.coneTypeOrNull ?: continue

            if (isCFuncCall) {
                if (argType is ConeVArrayType && !argIsInout) {
                    reporter.reportOn(
                        source = argExpr.source ?: expression.source,
                        factory = CfirErrors.INOUT_MISMATCH,
                        a = argType,
                    )
                }
            }
        }
    }

    private fun unwrapArgument(argument: CfirExpression): CfirExpression {
        // 命名实参在 raw-cfir 中被包成单表达式 block（详见 PsiRawCfirBuilder.convertCallArgument）
        val inner = (argument as? CfirBlock)?.statements?.singleOrNull() as? CfirExpression ?: argument
        return inner
    }
}
