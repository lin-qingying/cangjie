package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.source.CjRealSourceElementKind

/**
 * `return` 语句合法性检查。
 *
 * 该检查器负责两类框架级语义：
 * 1. raw-cfir 已明确标记 `ReturnNotAllowed` 的返回语句，统一映射为 `CFIR_INVALID_RETURN`。
 * 2. `handle` 子句内部的显式 `return`，按当前 CFIR 规则禁止直接跳出，统一报 `CFIR_INVALID_RETURN`。
 *
 * 说明：
 * - 这里只处理“语句位置是否合法”，不处理返回值类型是否匹配（由 [CfirReturnTypeMismatchChecker] 负责）。
 * - 仅对真实源码中的显式 `return` 报告，避免对 fake source 的隐式返回产生噪音。
 */
object CfirReturnLegalityChecker : CfirReturnExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirReturnExpression) {
        val source = expression.source ?: return
        if (source.kind != CjRealSourceElementKind) return

        // handle 子句中的 return 目前不允许直接跨边界返回。
        if (context.containingElements.any { it is CfirHandleClause }) {
            reporter.reportOn(source, CfirErrors.INVALID_RETURN)
            return
        }

        // raw-cfir 在无法绑定函数目标时，会把 return 绑定到 CfirErrorFunction 并附带 ReturnNotAllowed。
        val target = expression.target.labeledElement as? CfirErrorFunction ?: return
        val diagnostic = target.diagnostic as? ConeSimpleDiagnostic ?: return
        if (diagnostic.kind == DiagnosticKind.ReturnNotAllowed) {
            reporter.reportOn(source, CfirErrors.INVALID_RETURN)
        }
    }
}
