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
    private val sessionHolder: SessionHolder,
    private val context: BodyResolveContext,
) : SessionHolder by sessionHolder {
    private val graphBuilder
        get() = context.dataFlowAnalyzerContext.graphBuilder

    private val variableAssignmentAnalyzer
        get() = context.dataFlowAnalyzerContext.variableAssignmentAnalyzer

    private val hasActiveGraph: Boolean
        get() = graphBuilder.currentGraphOrNull != null

    fun enterCallArguments(call: CfirCall, arguments: List<CfirExpression>) {
        val lambdaArguments = collectAnonymousFunctionArguments(arguments)
        variableAssignmentAnalyzer.enterFunctionCall(lambdaArguments)
        if (hasActiveGraph) {
            graphBuilder.enterCallArguments(call, lambdaArguments)
        }
    }

    fun exitCallExplicitReceiver() {
        if (hasActiveGraph) {
            graphBuilder.exitCallExplicitReceiver()
        }
    }

    fun exitCallArguments() {
        if (hasActiveGraph) {
            graphBuilder.exitCallArguments()
        }
    }

    fun enterFunctionCall(functionCall: CfirFunctionCall) {
        if (hasActiveGraph) {
            graphBuilder.enterFunctionCall(functionCall)
        }
    }

    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean) {
        if (hasActiveGraph) {
            graphBuilder.exitFunctionCall(functionCall, callCompleted)
        }
        variableAssignmentAnalyzer.exitFunctionCall(callCompleted)
    }

    fun enterBlock(block: CfirBlock) {
        if (hasActiveGraph) {
            graphBuilder.enterBlock(block)
        }
    }

    fun exitBlock(block: CfirBlock) {
        if (hasActiveGraph) {
            graphBuilder.exitBlock(block)
        }
    }

    fun enterJump(jump: CfirJump<*>) {
        if (hasActiveGraph) {
            graphBuilder.enterJump(jump)
        }
    }

    fun enterFile(file: CfirFile, buildGraph: Boolean) {
        if (buildGraph) {
            graphBuilder.enterFile(file)
        }
    }

    fun exitFile(): ControlFlowGraph? {
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.File }
            ?.let { graphBuilder.exitFile().second }
    }

    fun enterClass(klass: CfirClass, buildGraph: Boolean) {
        if (buildGraph) {
            graphBuilder.enterClass(klass)
        }
        variableAssignmentAnalyzer.enterClass(klass)
    }

    fun exitClass(): ControlFlowGraph? {
        variableAssignmentAnalyzer.exitClass()
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.Class }
            ?.let { graphBuilder.exitClass().second }
    }

    fun enterFunction(function: CfirFunction) {
        variableAssignmentAnalyzer.enterFunction(function)
        if (function is CfirAnonymousFunction) {
            graphBuilder.enterAnonymousFunction(function)
        } else {
            graphBuilder.enterFunction(function)
        }
    }

    fun exitFunction(function: CfirFunction): CfirControlFlowGraphReference? {
        variableAssignmentAnalyzer.exitFunction()
        val graph = if (function is CfirAnonymousFunction) {
            graphBuilder.exitAnonymousFunction(function).third
        } else {
            graphBuilder.exitFunction(function).second
        }
        return CfirControlFlowGraphReferenceImpl(graph)
    }

    fun enterCodeFragment(codeFragment: CfirCodeFragment) {
        variableAssignmentAnalyzer.enterCodeFragment(codeFragment)
        graphBuilder.enterCodeFragment(codeFragment)
    }

    fun exitCodeFragment(codeFragment: CfirCodeFragment): ControlFlowGraph {
        variableAssignmentAnalyzer.exitCodeFragment(codeFragment)
        return graphBuilder.exitCodeFragment().second
    }

    fun enterAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterAnonymousFunctionExpression(anonymousFunctionExpression)
        }
    }

    fun enterFieldInitializer(field: CfirFieldVariable) {
        if (hasActiveGraph) {
            graphBuilder.enterFieldInitializer(field)
        }
    }

    fun exitFieldInitializer(): ControlFlowGraph? {
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.FieldInitializer }
            ?.let { graphBuilder.exitFieldInitializer().second }
    }

    fun enterValueParameter(parameter: CfirValueParameter) {
        if (!hasActiveGraph || parameter.defaultValue == null) return
        graphBuilder.enterValueParameter(parameter)
        graphBuilder.enterDefaultArguments(parameter)
    }

    fun exitValueParameter(parameter: CfirValueParameter): ControlFlowGraph? {
        if (!hasActiveGraph || parameter.defaultValue == null) return null
        val graph = graphBuilder.exitDefaultArguments().second
        graphBuilder.exitValueParameter(parameter)
        return graph
    }

    fun enterLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.enterLoop(loop)
    }

    fun exitLoop() {
        variableAssignmentAnalyzer.exitLoop()
    }

    fun enterWhileLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            graphBuilder.enterWhileLoop(loop)
        }
    }

    fun exitWhileLoopCondition(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWhileLoopCondition(loop)
        }
    }

    fun exitWhileLoop(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWhileLoop(loop)
        }
        variableAssignmentAnalyzer.exitLoop()
    }

    fun enterDoWhileLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            graphBuilder.enterDoWhileLoop(loop)
        }
    }

    fun enterDoWhileLoopCondition(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterDoWhileLoopCondition(loop)
        }
    }

    fun exitDoWhileLoop(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitDoWhileLoop(loop)
        }
        variableAssignmentAnalyzer.exitLoop()
    }

    fun enterMatchExpression(matchExpression: CfirMatchExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterMatchExpression(matchExpression)
        }
    }

    fun enterMatchBranchCondition(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.enterMatchBranchCondition(branch)
        }
    }

    fun exitMatchBranchCondition(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchBranchCondition(branch)
        }
    }

    fun exitMatchBranchResult(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchBranchResult(branch)
        }
    }

    /**
     * 从 tree 正式承载字段读取 CFG synthetic else 决策。
     *
     * 不允许回退到语法兜底；若 body-resolve 尚未写出可信穷尽性状态，直接失败。
     */
    fun matchSyntheticElseDecision(matchExpression: CfirMatchExpression): MatchSyntheticElseDecision {
        return when (val exhaustiveness = matchExpression.exhaustiveness) {
            is CfirMatchExhaustivenessStatus.Exhaustive -> MatchSyntheticElseDecision.NotRequired
            is CfirMatchExhaustivenessStatus.NonExhaustive -> MatchSyntheticElseDecision.Required
            CfirMatchExhaustivenessStatus.Unknown -> error(
                "Missing match exhaustiveness status for ${matchExpression::class.qualifiedName}. " +
                    "Body-resolve must write CfirMatchExpression.exhaustiveness before CFG exit."
            )

            is CfirMatchExhaustivenessStatus.Error -> error(
                "Invalid match exhaustiveness status for ${matchExpression::class.qualifiedName}: ${exhaustiveness.reason}"
            )
        }
    }

    fun exitMatchExpression(
        matchExpression: CfirMatchExpression,
        syntheticElseDecision: MatchSyntheticElseDecision,
        callCompleted: Boolean,
    ) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchExpression(matchExpression, syntheticElseDecision, callCompleted)
        }
    }

    fun enterTryExpression(tryExpression: CfirTryExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterTryExpression(tryExpression)
        }
    }

    fun exitTryMainBlock() {
        if (hasActiveGraph) {
            graphBuilder.exitTryMainBlock()
        }
    }

    fun enterCatchClause(catch: CfirCatch) {
        if (hasActiveGraph) {
            graphBuilder.enterCatchClause(catch)
        }
    }

    fun exitCatchClause(catch: CfirCatch) {
        if (hasActiveGraph) {
            graphBuilder.exitCatchClause(catch)
        }
    }

    fun enterFinallyBlock() {
        if (hasActiveGraph) {
            graphBuilder.enterFinallyBlock()
        }
    }

    fun exitFinallyBlock() {
        if (hasActiveGraph) {
            graphBuilder.exitFinallyBlock()
        }
    }

    fun exitTryExpression(callCompleted: Boolean) {
        if (hasActiveGraph) {
            graphBuilder.exitTryExpression(callCompleted)
        }
    }

    fun enterOptionalChain(optionalChainExpression: CfirOptionalChainExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterOptionalChain(optionalChainExpression)
        }
    }

    fun exitOptionalChain(optionalChainExpression: CfirOptionalChainExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitOptionalChain(optionalChainExpression)
        }
    }

    fun exitWrappedExpression(wrappedExpression: CfirWrappedExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWrappedExpression(wrappedExpression)
        }
    }

    fun exitJump(jump: CfirJump<*>) {
        if (hasActiveGraph) {
            graphBuilder.exitJump(jump)
        }
    }

    fun exitThrowException(throwExpression: CfirThrowExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitThrowExceptionNode(throwExpression)
        }
    }

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression)
        }
    }

    fun exitLiteralExpression(literalExpression: CfirLiteralExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitLiteralExpression(literalExpression)
        }
    }

    fun exitVariableAssignment(assignment: CfirAssignment) {
        if (hasActiveGraph) {
            graphBuilder.exitVariableAssignment(assignment)
        }
    }

    fun recordAssignment(assignment: CfirAssignment) {
        val lValue = assignment.lValue as? CfirQualifiedAccessExpression ?: return
        val symbol = (lValue.calleeReference as? org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference)?.resolvedSymbol?.cfir
            ?: return
        val declaration = symbol as? org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration ?: return
        val type = assignment.rValue.coneTypeOrNull ?: return
        variableAssignmentAnalyzer.visitAssignment(declaration, type)
    }

    fun resetSmartCastPosition() {
    }

    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction): Collection<CfirAnonymousFunctionReturnExpressionInfo> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function)
            ?.map(::CfirAnonymousFunctionReturnExpressionInfo)
            .orEmpty()
    }

    fun returnExpressionsOfFunction(function: CfirFunction): Collection<CfirExpression> {
        return graphBuilder.returnExpressionsOfFunction(function).orEmpty()
    }

    data class CfirAnonymousFunctionReturnExpressionInfo(
        val expression: CfirExpression,
    )

    private fun collectAnonymousFunctionArguments(arguments: List<CfirExpression>): List<CfirAnonymousFunction> {
        return arguments.mapNotNull { argument ->
            (argument as? CfirAnonymousFunctionExpression)?.anonymousFunction
        }
    }
}
