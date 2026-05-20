package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostic.ConeResumeThrowingMismatchTypeError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirResumeExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConeRigidType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Effects 检查器（Effects + EffectsExtra 分组）
 *
 * 对齐 C++ TypeCheckExpr/TryExpr.cpp、PerformExpr.cpp、ResumeExpr.cpp:
 * - RESUMPTION_HANDLE_TYPE_ERROR: handle clause command pattern 类型解析失败
 * - RETURN_IN_TRY_HANDLE_BLOCK: try/handle block 中的 return
 * - RESUMPTION_INCORRECT_RETURN_TYPE: resumption 返回类型不匹配
 * - COMMAND_RESUMPTION_MISMATCH: command-resumption 类型不匹配
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkHandlerBodyTypeMatch(handleClause: CfirHandleClause, tryBodyType: ConeCangJieType?) {
        if (tryBodyType == null || tryBodyType is ConeErrorType) return
        val handlerBodyType = handleClause.body.coneTypeOrNull ?: return
        if (handlerBodyType is ConeErrorType) return

        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, handlerBodyType, tryBodyType) != true) {
            reporter.reportOn(
                source = handleClause.body.source ?: handleClause.source ?: return,
                factory = CfirErrors.MISMATCHING_HANDLE_BLOCK,
                a = handlerBodyType,
                b = tryBodyType,
            )
        }
    }
}

/**
 * Effects BasicExpression 级别检查器
 */
object CfirEffectsBasicChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        when (expression) {
            is CfirHandleClause -> checkHandleControlFlow(expression)
            is CfirResumeExpression -> checkResumeExpression(expression)
            else -> Unit
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkHandleControlFlow(handleClause: CfirHandleClause) {
        handleClause.body.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitBreakExpression(breakExpression: CfirBreakExpression) {
                reporter.reportOn(breakExpression.source, CfirErrors.INVALID_LOOP_CONTROL)
            }

            override fun visitContinueExpression(continueExpression: CfirContinueExpression) {
                reporter.reportOn(continueExpression.source, CfirErrors.INVALID_LOOP_CONTROL)
            }

            override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
                reporter.reportOn(returnExpression.source, CfirErrors.RETURN_IN_TRY_HANDLE_BLOCK)
            }
        })
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkResumeExpression(expression: CfirResumeExpression) {
        val exprType = expression.coneTypeOrNull
        val errorDiagnostic = (exprType as? ConeErrorType)?.diagnostic
        if (errorDiagnostic is ConeResumeThrowingMismatchTypeError) return

        // `resume with` 的类型校验必须绑定到最近的 handle 子句。
        // 某些遍历路径下 containingElements 里不一定保留 CfirHandleClause，
        // 这里补充 containingStatements 兜住同一语义层级，避免漏报 TYPE_MISMATCH。
        val handleClause = (
            context.containingElements.lastOrNull { it is CfirHandleClause }
                ?: context.containingStatements.lastOrNull { it is CfirHandleClause }
            ) as? CfirHandleClause ?: return
        val commandType = resolveCommandResultType(handleClause) ?: return
        val withExpression = expression.withExpression ?: return
        val actualType = withExpression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType || commandType is ConeErrorType) return
        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, commandType) != true) {
            reporter.reportOn(
                source = withExpression.source ?: return,
                factory = CfirErrors.TYPE_MISMATCH,
                a = commandType,
                b = actualType,
                c = false,
            )
        }
    }

    context(context: CheckerContext)
    private fun resolveCommandResultType(handleClause: CfirHandleClause): ConeCangJieType? {
        val commandType = handleClause.commandPattern.typeRefs.firstOrNull()?.coneType ?: return null
        return findCommandSupertype(commandType)?.typeArguments?.firstOrNull()?.type
    }

    context(context: CheckerContext)
    private fun findCommandSupertype(type: ConeCangJieType?): ConeClassLikeType? {
        if (type == null) return null
        return collectSupertypeChain(type, context.session.typeContext)
            .filterIsInstance<ConeClassLikeType>()
            .firstOrNull { it.lookupTag.classId == StdlibClassIds.Command }
    }

    private fun collectSupertypeChain(
        type: ConeCangJieType,
        typeContext: ConeInferenceContext,
    ): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        val visited = mutableSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue.add(type)
        visited.add(type)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result += current
            val constructor = with(typeContext) { (current as? ConeRigidType)?.typeConstructor() } ?: continue
            val supertypes = with(typeContext) {
                constructor.supertypes().mapNotNull { it as? ConeCangJieType }
            }
            supertypes.forEach { supertype ->
                if (visited.add(supertype)) queue.add(supertype)
            }
        }
        return result
    }
}
