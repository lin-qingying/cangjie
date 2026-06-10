package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * 表达式位置不能把 class / struct / enum 等类型名当作值使用。
 *
 * Kotlin FIR 在 `FirStandaloneQualifierChecker` 中检查独立 qualifier；
 * 本地 CFIR 暂无独立 qualifier 节点，类型名会以 `CfirResolvedNamedReference`
 * 暂存，因此在 qualified-access checker 层对齐官方仓颉 `sema_ref_not_be_type`。
 */
object CfirClassifierAsExpressionChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return
        if (expression.isUsedAsOuterReceiver()) return

        val resolvedReference = expression.calleeReference as? CfirResolvedNamedReference ?: return
        resolvedReference.resolvedSymbol as? CfirClassLikeSymbol<*> ?: return
        val source = resolvedReference.source ?: expression.source ?: return

        reporter.reportOn(
            source = CjOffsetsOnlySourceElement(source.startOffset, source.startOffset + 1),
            factory = CfirErrors.REF_NOT_BE_TYPE,
        )
    }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isUsedAsOuterReceiver(): Boolean {
        return context.callsOrAssignments.asReversed().drop(1).any { call ->
            call is CfirQualifiedAccessExpression && call.explicitReceiver === this
        }
    }
}
