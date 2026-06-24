package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder.MatchSyntheticElseDecision
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext

/**
 * CFIR body-resolve 数据流门面，对位 Kotlin FIR `FirDataFlowAnalyzer` 的 CFG / assignment facade 角色。
 *
 * 当前仓颉侧尚未迁入 Kotlin 全量 smart-cast/logic-system，但以下职责已经切换到真实状态对象：
 * - CFG 建图统一委托给 [CfirDataFlowAnalyzerContext.graphBuilder]
 * - 局部变量赋值稳定性统一委托给 [CfirDataFlowAnalyzerContext.variableAssignmentAnalyzer]
 * - 匿名函数 return 收集优先从 CFG 投影，而不是手工 DFS
 */
class CfirDataFlowAnalyzer(
    /** 提供当前 CFIR session 的 holder。 */
    private val sessionHolder: SessionHolder,
    /** body resolve 上下文，持有 CFG builder 与局部变量赋值分析器状态。 */
    private val context: BodyResolveContext,
) : SessionHolder by sessionHolder {
    /** 当前数据流上下文中的 CFG builder。 */
    private val graphBuilder
        get() = context.dataFlowAnalyzerContext.graphBuilder

    /** 当前数据流上下文中的局部变量赋值稳定性分析器。 */
    private val variableAssignmentAnalyzer
        get() = context.dataFlowAnalyzerContext.variableAssignmentAnalyzer

    /** 当前是否存在正在构建的 CFG。 */
    private val hasActiveGraph: Boolean
        get() = graphBuilder.currentGraphOrNull != null

    /**
     * 进入调用实参列表，记录其中的 lambda 参数并通知 CFG builder。
     */
    fun enterCallArguments(call: CfirCall, arguments: List<CfirExpression>) {
        val lambdaArguments = collectAnonymousFunctionArguments(arguments)
        variableAssignmentAnalyzer.enterFunctionCall(lambdaArguments)
        if (hasActiveGraph) {
            graphBuilder.enterCallArguments(call, lambdaArguments)
        }
    }

    /**
     * 结束调用显式接收者节点。
     */
    fun exitCallExplicitReceiver() {
        if (hasActiveGraph) {
            graphBuilder.exitCallExplicitReceiver()
        }
    }

    /**
     * 结束调用实参列表。
     */
    fun exitCallArguments() {
        if (hasActiveGraph) {
            graphBuilder.exitCallArguments()
        }
    }

    /**
     * 进入函数调用表达式节点。
     */
    fun enterFunctionCall(functionCall: CfirFunctionCall) {
        if (hasActiveGraph) {
            graphBuilder.enterFunctionCall(functionCall)
        }
    }

    /**
     * 退出函数调用表达式，并同步函数调用是否完成给局部变量赋值分析器。
     */
    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean) {
        if (hasActiveGraph) {
            graphBuilder.exitFunctionCall(functionCall, callCompleted)
        }
        variableAssignmentAnalyzer.exitFunctionCall(callCompleted)
    }

    /**
     * 进入 block CFG 节点。
     */
    fun enterBlock(block: CfirBlock) {
        if (hasActiveGraph) {
            graphBuilder.enterBlock(block)
        }
    }

    /**
     * 退出 block CFG 节点。
     */
    fun exitBlock(block: CfirBlock) {
        if (hasActiveGraph) {
            graphBuilder.exitBlock(block)
        }
    }

    /**
     * 进入 jump 表达式节点。
     */
    fun enterJump(jump: CfirJump<*>) {
        if (hasActiveGraph) {
            graphBuilder.enterJump(jump)
        }
    }

    /**
     * 进入文件级 CFG。
     */
    fun enterFile(file: CfirFile, buildGraph: Boolean) {
        if (buildGraph) {
            graphBuilder.enterFile(file)
        }
    }

    /**
     * 退出文件级 CFG 并返回构建结果。
     */
    fun exitFile(): ControlFlowGraph? {
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.File }
            ?.let { graphBuilder.exitFile().second }
    }

    /**
     * 进入 class CFG 和局部变量赋值分析作用域。
     */
    fun enterClass(klass: CfirClass, buildGraph: Boolean) {
        if (buildGraph) {
            graphBuilder.enterClass(klass)
        }
        variableAssignmentAnalyzer.enterClass(klass)
    }

    /**
     * 退出 class CFG 和局部变量赋值分析作用域。
     */
    fun exitClass(): ControlFlowGraph? {
        variableAssignmentAnalyzer.exitClass()
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.Class }
            ?.let { graphBuilder.exitClass().second }
    }

    /**
     * 进入函数 CFG 和局部变量赋值分析作用域。
     */
    fun enterFunction(function: CfirFunction) {
        variableAssignmentAnalyzer.enterFunction(function)
        if (function is CfirAnonymousFunction) {
            graphBuilder.enterAnonymousFunction(function)
        } else {
            graphBuilder.enterFunction(function)
        }
    }

    /**
     * 退出函数 CFG 和局部变量赋值分析作用域，并返回函数 CFG 引用。
     */
    fun exitFunction(function: CfirFunction): CfirControlFlowGraphReference? {
        variableAssignmentAnalyzer.exitFunction()
        val graph = if (function is CfirAnonymousFunction) {
            graphBuilder.exitAnonymousFunction(function).third
        } else {
            graphBuilder.exitFunction(function).second
        }
        return CfirControlFlowGraphReferenceImpl(graph)
    }

    /**
     * 进入代码片段 CFG 和局部变量分析顶层作用域。
     */
    fun enterCodeFragment(codeFragment: CfirCodeFragment) {
        variableAssignmentAnalyzer.enterCodeFragment(codeFragment)
        graphBuilder.enterCodeFragment(codeFragment)
    }

    /**
     * 退出代码片段 CFG 和局部变量分析顶层作用域。
     */
    fun exitCodeFragment(codeFragment: CfirCodeFragment): ControlFlowGraph {
        variableAssignmentAnalyzer.exitCodeFragment(codeFragment)
        return graphBuilder.exitCodeFragment().second
    }

    /**
     * 进入匿名函数表达式 CFG 节点。
     */
    fun enterAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterAnonymousFunctionExpression(anonymousFunctionExpression)
        }
    }

    /**
     * 进入字段初始化器 CFG。
     */
    fun enterFieldInitializer(field: CfirFieldVariable) {
        if (hasActiveGraph) {
            graphBuilder.enterFieldInitializer(field)
        }
    }

    /**
     * 退出字段初始化器 CFG。
     */
    fun exitFieldInitializer(): ControlFlowGraph? {
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.FieldInitializer }
            ?.let { graphBuilder.exitFieldInitializer().second }
    }

    /**
     * 进入值参数默认值 CFG。
     */
    fun enterValueParameter(parameter: CfirValueParameter) {
        if (!hasActiveGraph || parameter.defaultValue == null) return
        graphBuilder.enterValueParameter(parameter)
        graphBuilder.enterDefaultArguments(parameter)
    }

    /**
     * 退出值参数默认值 CFG。
     */
    fun exitValueParameter(parameter: CfirValueParameter): ControlFlowGraph? {
        if (!hasActiveGraph || parameter.defaultValue == null) return null
        val graph = graphBuilder.exitDefaultArguments().second
        graphBuilder.exitValueParameter(parameter)
        return graph
    }

    /**
     * 进入不建图的通用循环赋值分析作用域。
     */
    fun enterLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.enterLoop(loop)
    }

    /**
     * 退出不建图的通用循环赋值分析作用域。
     */
    fun exitLoop() {
        variableAssignmentAnalyzer.exitLoop()
    }

    /**
     * 进入 while 循环 CFG 和赋值分析作用域。
     */
    fun enterWhileLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            graphBuilder.enterWhileLoop(loop)
        }
    }

    /**
     * 退出 while 条件 CFG 节点。
     */
    fun exitWhileLoopCondition(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWhileLoopCondition(loop)
        }
    }

    /**
     * 退出 while 循环 CFG 和赋值分析作用域。
     */
    fun exitWhileLoop(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWhileLoop(loop)
        }
        variableAssignmentAnalyzer.exitLoop()
    }

    /**
     * 进入 do-while 循环 CFG 和赋值分析作用域。
     */
    fun enterDoWhileLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            graphBuilder.enterDoWhileLoop(loop)
        }
    }

    /**
     * 进入 do-while 条件 CFG 节点。
     */
    fun enterDoWhileLoopCondition(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterDoWhileLoopCondition(loop)
        }
    }

    /**
     * 退出 do-while 循环 CFG 和赋值分析作用域。
     */
    fun exitDoWhileLoop(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitDoWhileLoop(loop)
        }
        variableAssignmentAnalyzer.exitLoop()
    }

    /**
     * 进入 match 表达式 CFG。
     */
    fun enterMatchExpression(matchExpression: CfirMatchExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterMatchExpression(matchExpression)
        }
    }

    /**
     * 进入 match 分支条件 CFG。
     */
    fun enterMatchBranchCondition(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.enterMatchBranchCondition(branch)
        }
    }

    /**
     * 退出 match 分支条件 CFG。
     */
    fun exitMatchBranchCondition(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchBranchCondition(branch)
        }
    }

    /**
     * 退出 match 分支结果 CFG。
     */
    fun exitMatchBranchResult(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchBranchResult(branch)
        }
    }

    /**
     * 从 tree 正式承载字段读取 CFG synthetic else 决策。
     *
     * `Unknown / Error` 在这里不再视为框架硬错误，而是视为“shared analyzer 当前无法稳定证明穷尽”，
     * CFG 必须保守地补 synthetic else，继续后续分析链。
     */
    fun matchSyntheticElseDecision(matchExpression: CfirMatchExpression): MatchSyntheticElseDecision {
        return when (val exhaustiveness = matchExpression.exhaustiveness) {
            is CfirMatchExhaustivenessStatus.Exhaustive -> MatchSyntheticElseDecision.NotRequired
            is CfirMatchExhaustivenessStatus.NonExhaustive -> MatchSyntheticElseDecision.Required
            CfirMatchExhaustivenessStatus.Unknown,
            is CfirMatchExhaustivenessStatus.Error,
            -> MatchSyntheticElseDecision.Required
        }
    }

    /**
     * 退出 match 表达式 CFG。
     */
    fun exitMatchExpression(
        matchExpression: CfirMatchExpression,
        syntheticElseDecision: MatchSyntheticElseDecision,
        callCompleted: Boolean,
    ) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchExpression(matchExpression, syntheticElseDecision, callCompleted)
        }
    }

    /**
     * 进入 try 表达式 CFG。
     */
    fun enterTryExpression(tryExpression: CfirTryExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterTryExpression(tryExpression)
        }
    }

    /**
     * 退出 try 主体 CFG。
     */
    fun exitTryMainBlock() {
        if (hasActiveGraph) {
            graphBuilder.exitTryMainBlock()
        }
    }

    /**
     * 进入 catch 子句 CFG。
     */
    fun enterCatchClause(catch: CfirCatch) {
        if (hasActiveGraph) {
            graphBuilder.enterCatchClause(catch)
        }
    }

    /**
     * 退出 catch 子句 CFG。
     */
    fun exitCatchClause(catch: CfirCatch) {
        if (hasActiveGraph) {
            graphBuilder.exitCatchClause(catch)
        }
    }

    /**
     * 进入 handle 子句 CFG。
     */
    fun enterHandleClause(handleClause: CfirHandleClause) {
        if (hasActiveGraph) {
            graphBuilder.enterHandleClause(handleClause)
        }
    }

    /**
     * 退出 handle 子句 CFG。
     */
    fun exitHandleClause(handleClause: CfirHandleClause) {
        if (hasActiveGraph) {
            graphBuilder.exitHandleClause(handleClause)
        }
    }

    /**
     * 进入 finally block CFG。
     */
    fun enterFinallyBlock() {
        if (hasActiveGraph) {
            graphBuilder.enterFinallyBlock()
        }
    }

    /**
     * 退出 finally block CFG。
     */
    fun exitFinallyBlock() {
        if (hasActiveGraph) {
            graphBuilder.exitFinallyBlock()
        }
    }

    /**
     * 退出 try 表达式 CFG。
     */
    fun exitTryExpression(callCompleted: Boolean) {
        if (hasActiveGraph) {
            graphBuilder.exitTryExpression(callCompleted)
        }
    }

    /**
     * 进入可选链表达式 CFG。
     */
    fun enterOptionalChain(optionalChainExpression: CfirOptionalChainExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterOptionalChain(optionalChainExpression)
        }
    }

    /**
     * 退出可选链表达式 CFG。
     */
    fun exitOptionalChain(optionalChainExpression: CfirOptionalChainExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitOptionalChain(optionalChainExpression)
        }
    }

    /**
     * 退出包装表达式 CFG。
     */
    fun exitWrappedExpression(wrappedExpression: CfirWrappedExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWrappedExpression(wrappedExpression)
        }
    }

    /**
     * 退出 jump 表达式 CFG。
     */
    fun exitJump(jump: CfirJump<*>) {
        if (hasActiveGraph) {
            graphBuilder.exitJump(jump)
        }
    }

    /**
     * 退出 throw 表达式 CFG。
     */
    fun exitThrowException(throwExpression: CfirThrowExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitThrowExceptionNode(throwExpression)
        }
    }

    /**
     * 退出 qualified access 表达式 CFG。
     */
    fun exitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression)
        }
    }

    /**
     * 退出字面量表达式 CFG。
     */
    fun exitLiteralExpression(literalExpression: CfirLiteralExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitLiteralExpression(literalExpression)
        }
    }

    /**
     * 退出变量赋值 CFG。
     */
    fun exitVariableAssignment(assignment: CfirAssignment) {
        if (hasActiveGraph) {
            graphBuilder.exitVariableAssignment(assignment)
        }
    }

    /**
     * 把赋值表达式右侧类型记录到局部变量赋值分析器。
     */
    fun recordAssignment(assignment: CfirAssignment) {
        val lValue = assignment.lValue as? CfirQualifiedAccessExpression ?: return
        val symbol = (lValue.calleeReference as? org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference)?.resolvedSymbol?.cfir
            ?: return
        val declaration = symbol as? org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration ?: return
        val type = assignment.rValue.coneTypeOrNull ?: return
        variableAssignmentAnalyzer.visitAssignment(declaration, type)
    }

    /**
     * 重置 smart-cast 位置。
     *
     * 当前 CFIR 数据流门面尚未接入 smart-cast 位置栈，该入口保留为调用侧框架挂点。
     */
    fun resetSmartCastPosition() {
    }

    /**
     * 查询匿名函数 CFG 中收集到的 return 表达式。
     */
    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction): Collection<CfirAnonymousFunctionReturnExpressionInfo> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function)
            ?.map(::CfirAnonymousFunctionReturnExpressionInfo)
            .orEmpty()
    }

    /**
     * 查询普通函数 CFG 中收集到的 return 表达式。
     */
    fun returnExpressionsOfFunction(function: CfirFunction): Collection<CfirExpression> {
        return graphBuilder.returnExpressionsOfFunction(function).orEmpty()
    }

    /**
     * 匿名函数 return 表达式信息。
     */
    data class CfirAnonymousFunctionReturnExpressionInfo(
        /** CFG 中记录到的 return 表达式。 */
        val expression: CfirExpression,
    )

    /**
     * 从调用实参中收集匿名函数参数。
     */
    private fun collectAnonymousFunctionArguments(arguments: List<CfirExpression>): List<CfirAnonymousFunction> {
        return arguments.mapNotNull { argument ->
            (argument as? CfirAnonymousFunctionExpression)?.anonymousFunction
        }
    }
}
