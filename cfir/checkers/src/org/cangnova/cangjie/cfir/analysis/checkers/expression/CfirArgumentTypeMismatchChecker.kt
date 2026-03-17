package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * 鍑芥暟鍙傛暟绫诲瀷妫€鏌ュ櫒銆? *
 * 妫€鏌ュ嚱鏁拌皟鐢ㄧ殑姣忎釜瀹炲弬绫诲瀷鏄惁涓哄搴斿舰鍙傜被鍨嬬殑瀛愮被鍨嬨€? * 浠呮鏌ュ凡瑙ｆ瀽鐨勮皟鐢紙鏈В鏋愮殑璋冪敤宸叉湁 error type锛屼笉浼氳鎶ワ級銆? */
object CfirArgumentTypeMismatchChecker : CfirFunctionCallChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val resolvedRef = expression.calleeReference as? CfirResolvedNamedReference
        if (resolvedRef == null) {
            System.err.println("[DEBUG-ARG] calleeRef=${expression.calleeReference::class.simpleName}")
            return
        }
        val functionSymbol = resolvedRef.resolvedSymbol as? CfirFunctionSymbol ?: return
        val function = functionSymbol.cfir
        val parameters = function.valueParameters
        val arguments = expression.arguments

        for (i in arguments.indices) {
            if (i >= parameters.size) break
            val argSource = arguments[i].source as? AbstractCjSourceElement ?: continue
            val actualType = arguments[i].coneTypeOrNull ?: continue
            val expectedTypeRef = parameters[i].returnTypeRef as? CfirResolvedTypeRef ?: continue
            val expectedType = expectedTypeRef.coneType
            if (!CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType)) {
                reporter.reportOn(
                    argSource, CfirErrors.ARGUMENT_TYPE_MISMATCH,
                    expectedType ,
                    actualType ,
                    false,
                )
            }
        }
    }
}

