package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * Effects 补充检查器（EffectsExtra 分组）
 *
 * 对齐 C++ TypeCheckExpr/TryExpr.cpp:
 * - handle clause 中的 command type pattern 类型检查
 */
object CfirTryHandleReturnChecker : CfirTryExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirTryExpression) {
        for (handler in expression.handlers) {
            checkHandleClause(handler)
        }
    }

    /**
     * 检查 handle clause 中 command type pattern 的类型是否合法。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkHandleClause(handleClause: CfirHandleClause) {
        val commandPattern = handleClause.commandPattern
        for (typeRef in commandPattern.typeRefs) {
            val resolvedType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) {
                reporter.reportOn(
                    source = commandPattern.source ?: handleClause.source,
                    factory = CfirErrors.RESUMPTION_HANDLE_TYPE_ERROR,
                )
            }
        }
    }
}
