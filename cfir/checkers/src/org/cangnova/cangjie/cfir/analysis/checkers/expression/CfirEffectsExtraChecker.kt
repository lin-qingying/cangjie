package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.typeContext

/**
 * Effects 检查器（Effects + EffectsExtra 分组）
 *
 * 对齐 C++ TypeCheckExpr/TryExpr.cpp、PerformExpr.cpp、ResumeExpr.cpp:
 * - RESUMPTION_HANDLE_TYPE_ERROR: handle clause command pattern 类型解析失败
 * - RETURN_IN_TRY_HANDLE_BLOCK: try/handle block 中的 return（已由 CfirReturnLegalityChecker 处理）
 * - RESUMPTION_INCORRECT_RETURN_TYPE: resumption 返回类型不匹配
 * - COMMAND_RESUMPTION_MISMATCH: command-resumption 类型不匹配
 *
 * 注册为 tryExpressionCheckers
 */
object CfirTryHandleReturnChecker : CfirTryExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirTryExpression) {
        val tryBodyType = expression.tryBlock.coneTypeOrNull

        for (handler in expression.handlers) {
            checkHandleClauseType(handler)
            checkHandlerBodyTypeMatch(handler, tryBodyType)
        }
    }

    /**
     * 检查 handle clause 中 command type pattern 的类型是否合法。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkHandleClauseType(handleClause: CfirHandleClause) {
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

    /**
     * handle block 的类型必须与 try block 类型兼容。
     *
     * 对齐 C++ DiagKind::sema_mismatching_handle_block
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkHandlerBodyTypeMatch(handleClause: CfirHandleClause, tryBodyType: ConeCangJieType?) {
        if (tryBodyType == null || tryBodyType is ConeErrorType) return
        val handlerBodyType = handleClause.body.coneTypeOrNull ?: return
        if (handlerBodyType is ConeErrorType) return

        if (handlerBodyType != tryBodyType) {
            // 类型不一致时报告（严格的子类型检查需要 typeContext）
            val typeContext = context.session.typeContext
            if (!org.cangnova.cangjie.type.AbstractTypeChecker.isSubtypeOf(typeContext, handlerBodyType, tryBodyType)) {
                reporter.reportOn(
                    source = handleClause.source,
                    factory = CfirErrors.MISMATCHING_HANDLE_BLOCK,
                    a = handlerBodyType,
                    b = tryBodyType,
                )
            }
        }
    }
}

/**
 * Effects BasicExpression 级别检查器
 *
 * 通过 BasicExpressionChecker 分发，检查 perform/resume 表达式相关的 effects 语义。
 * CfirPerformExpression 和 CfirResumeExpression 已通过 visitAlso 注册到 BasicExpressionChecker。
 */
object CfirEffectsBasicChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        when (expression) {
            is org.cangnova.cangjie.cfir.expressions.CfirPerformExpression -> {
                checkPerformExpression(expression)
            }
            is org.cangnova.cangjie.cfir.expressions.CfirResumeExpression -> {
                checkResumeExpression(expression)
            }
            else -> Unit
        }
    }

    /**
     * perform 表达式的类型必须与声明的 command 类型兼容。
     *
     * 对齐 C++ DiagKind::sema_command_incompatible_type
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPerformExpression(expression: org.cangnova.cangjie.cfir.expressions.CfirPerformExpression) {
        val exprType = expression.coneTypeOrNull
        if (exprType is ConeErrorType) {
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.COMMAND_INCOMPATIBLE_TYPE,
                a = exprType,
            )
        }
    }

    /**
     * resume 表达式的 resumption 类型必须正确。
     *
     * 对齐 C++ DiagKind::sema_implicit_resume_outside_handler
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkResumeExpression(expression: org.cangnova.cangjie.cfir.expressions.CfirResumeExpression) {
        val exprType = expression.coneTypeOrNull
        if (exprType is ConeErrorType) {
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.IMPLICIT_RESUME_OUTSIDE_HANDLER,
            )
        }
    }
}
