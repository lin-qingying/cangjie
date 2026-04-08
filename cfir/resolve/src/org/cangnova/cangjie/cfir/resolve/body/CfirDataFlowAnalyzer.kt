package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid

/**
 * Body-resolve 阶段的数据流门面。
 *
 * 目前仍未实现完整 CFG / smart-cast 分析，但已经需要与 Kotlin FIR 对齐这些调用级钩子：
 * - enter/exit call arguments
 * - exit explicit receiver
 * - enter/exit function call
 * - 匿名函数 return 表达式收集
 *
 * 这样可以先把调用解析与 completion 的结构边界固定住，后续再逐步往这些 frame 上叠加
 * CFG、赋值分析与 smart-cast 状态。
 */
class CfirDataFlowAnalyzer(
    private val sessionHolder: SessionHolder,
    private val context: BodyResolveContext,
) : SessionHolder by sessionHolder {
    val currentCallArgumentsFrame: CfirDataFlowAnalyzerContext.CallArgumentsFrame?
        get() = context.dataFlowAnalyzerContext.currentCallArgumentsFrame

    val currentFunctionCallFrame: CfirDataFlowAnalyzerContext.FunctionCallFrame?
        get() = context.dataFlowAnalyzerContext.currentFunctionCallFrame

    fun enterCallArguments(call: CfirCall, arguments: List<CfirExpression>) {
        context.dataFlowAnalyzerContext.enterCallArguments(
            call = call,
            lambdaArguments = collectAnonymousFunctionArguments(arguments),
        )
    }

    fun exitCallExplicitReceiver() {
        context.dataFlowAnalyzerContext.exitCallExplicitReceiver()
    }

    fun exitCallArguments() {
        context.dataFlowAnalyzerContext.exitCallArguments()
    }

    fun enterFunctionCall(functionCall: CfirFunctionCall) {
        context.dataFlowAnalyzerContext.enterFunctionCall(
            functionCall = functionCall,
            lambdaArguments = currentCallArgumentsFrame?.lambdaArguments.orEmpty(),
        )
    }

    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean) {
        context.dataFlowAnalyzerContext.exitFunctionCall(functionCall, callCompleted)
    }

    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction): Collection<CfirAnonymousFunctionReturnExpressionInfo> {
        val body = function.body ?: return emptyList()
        val collector = ReturnExpressionCollector(function)
        body.acceptChildren(collector)
        val results = collector.results.toMutableList()

        val lastExpression = body.statements.lastOrNull() as? CfirExpression
        if (lastExpression != null && lastExpression !is CfirReturnExpression) {
            if (results.none { it.expression === lastExpression }) {
                results += CfirAnonymousFunctionReturnExpressionInfo(lastExpression)
            }
        }

        return results
    }

    data class CfirAnonymousFunctionReturnExpressionInfo(
        val expression: CfirExpression,
    )

    private fun collectAnonymousFunctionArguments(arguments: List<CfirExpression>): List<CfirAnonymousFunction> {
        return arguments.mapNotNull { argument ->
            (argument as? CfirAnonymousFunctionExpression)?.anonymousFunction
        }
    }

    private class ReturnExpressionCollector(
        private val owner: CfirAnonymousFunction,
    ) : CfirDefaultVisitorVoid() {
        private val _results = mutableListOf<CfirAnonymousFunctionReturnExpressionInfo>()
        val results: List<CfirAnonymousFunctionReturnExpressionInfo>
            get() = _results

        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            element.acceptChildren(this)
        }

        override fun visitAnonymousFunction(anonymousFunction: CfirAnonymousFunction) {
            if (anonymousFunction === owner) {
                anonymousFunction.acceptChildren(this)
            }
        }

        override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
            returnExpression.result?.let { expression ->
                _results += CfirAnonymousFunctionReturnExpressionInfo(expression)
            }
            returnExpression.acceptChildren(this)
        }
    }
}
