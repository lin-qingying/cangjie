package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.CfirIterationOwner
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirCallExpression
import org.cangnova.cangjie.cfir.expressions.CfirUnaryExpression
import org.cangnova.cangjie.cfir.expressions.CfirBinaryExpression
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.expressions.unwrapAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder.MatchSyntheticElseDecision
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.calls.ImplicitValue
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind

/**
 * CFIR body-resolve 数据流门面，对位 Kotlin FIR `FirDataFlowAnalyzer` 的 CFG / assignment facade 角色。
 *
 * 当前仓颉侧尚未迁入 Kotlin 全量 smart-cast/logic-system，但以下职责已经切换到真实状态对象：
 * - CFG 建图统一委托给 [CfirDataFlowAnalyzerContext.graphBuilder]
 * - 局部变量赋值稳定性统一委托给 [CfirDataFlowAnalyzerContext.variableAssignmentAnalyzer]
 * - 匿名函数 return 收集优先从 CFG 投影，而不是手工 DFS
 */
@OptIn(CfgInternals::class)
class CfirDataFlowAnalyzer(
    /** 提供当前 CFIR session 的 holder。 */
    private val sessionHolder: SessionHolder,
    /** body resolve 上下文，持有 CFG builder 与局部变量赋值分析器状态。 */
    private val context: BodyResolveContext,
    /** 已解析图的正式分析遍历保留 phase，却为参数默认值建立本次独立子图。 */
    private val rebuildingResolvedGraph: Boolean = false,
    /** 拥有图重建对借用 reference 的有效目标投影；普通解析保留实际符号。 */
    private val symbolMapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { it },
) : SessionHolder by sessionHolder {
    private val logicSystem: LogicSystem by lazy(LazyThreadSafetyMode.NONE) {
        object : LogicSystem(session.typeContext) {}
    }

    /** 当前解析位置的真实流；调用实参结束及子图退出时按既有 CFG 位置恢复。 */
    var currentSmartCastPosition: Flow? = null
        private set
    /** 当前数据流上下文中的 CFG builder。 */
    private val graphBuilder
        get() = context.dataFlowAnalyzerContext.graphBuilder

    /** 当前数据流上下文中的局部变量赋值稳定性分析器。 */
    private val variableAssignmentAnalyzer
        get() = context.dataFlowAnalyzerContext.variableAssignmentAnalyzer

    /** 当前是否存在正在构建的 CFG。 */
    private val hasActiveGraph: Boolean
        get() = graphBuilder.currentGraphOrNull != null

    /** speculative 解析隔离实际 CFG/mini 状态，同时恢复调用前的 flow 与隐式 receiver 位置。 */
    fun <T> withIsolatedContext(block: () -> T): T {
        val originalPosition = currentSmartCastPosition
        return try {
            context.dataFlowAnalyzerContext.withIsolatedContext {
                resetSmartCastPositionTo(null)
                block()
            }
        } finally {
            resetSmartCastPositionTo(originalPosition)
        }
    }

    fun captureDataFlowAnalyzerContext(firMapper: SnapshotCfirMapper): CfirDataFlowAnalyzerContextSnapshot =
        context.dataFlowAnalyzerContext.createSnapshot(firMapper, currentSmartCastPosition)

    fun restoreDataFlowAnalyzerContext(snapshot: CfirDataFlowAnalyzerContextSnapshot) {
        context.dataFlowAnalyzerContext.resetFrom(snapshot.context)
        resetSmartCastPositionTo(snapshot.smartCastPosition)
    }

    /**
     * 进入调用实参列表，记录其中的 lambda 参数并通知 CFG builder。
     */
    fun enterCallArguments(call: CfirCall, arguments: List<CfirExpression>) {
        val lambdaArguments = collectAnonymousFunctionArguments(arguments)
        variableAssignmentAnalyzer.enterFunctionCall(lambdaArguments)
        if (hasActiveGraph) {
            graphBuilder.enterCallArguments(call, lambdaArguments)?.mergeIncomingFlow()
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
            val (split, exit) = graphBuilder.exitCallArguments()
            split?.mergeIncomingFlow()
            exit?.mergeIncomingFlow()
            exit?.let { resetSmartCastPositionTo(it.explicitReceiverExitNode.flow) }
        }
    }

    /**
     * 进入函数调用表达式节点。
     */
    fun enterFunctionCall(functionCall: CfirFunctionCall) {
        if (hasActiveGraph) {
            graphBuilder.enterFunctionCall(functionCall).mergeIncomingFlow()
        }
    }

    /**
     * 退出函数调用表达式，并同步函数调用是否完成给局部变量赋值分析器。
     */
    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean) {
        variableAssignmentAnalyzer.exitFunctionCall(callCompleted)
        if (hasActiveGraph) {
            val result = graphBuilder.exitFunctionCall(functionCall, callCompleted)
            result.lambdaExitNodes.forEach { it.mergeIncomingFlow() }
            result.value.mergeIncomingFlow(semanticTransferComplete = collectAnonymousFunctionArguments(functionCall.argumentList.arguments).isEmpty())
        }
    }

    /** 从实际 lexical owner 读取 active/published 图，修订它已有的 node；不借父 currentGraph。 */
    @org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals
    fun updateFunctionCallNodes(
        original: CfirFunctionCall,
        replacement: CfirFunctionCall,
        owner: CfirControlFlowGraphOwner,
    ) {
        val inProgress = graphBuilder.graphInProgress(owner)
        val graph = if (inProgress != null) inProgress else owner.controlFlowGraphReference?.controlFlowGraph
        if (graph != null) graphBuilder.updateFunctionCallNodes(original, replacement, graph)
    }

    fun describeFunctionCallNodes(call: CfirFunctionCall): String = graphBuilder.describeFunctionCallNodes(call)

    /**
     * 进入 block CFG 节点。
     */
    fun enterBlock(block: CfirBlock) {
        if (hasActiveGraph) {
            graphBuilder.enterBlock(block).mergeIncomingFlow()
        }
    }

    /**
     * 退出 block CFG 节点。
     */
    fun exitBlock(block: CfirBlock) {
        if (hasActiveGraph) {
            graphBuilder.exitBlock(block).mergeIncomingFlow()
        }
    }

    /** 普通 BODY 和已解析拥有图重建共用独立 If 的同一分析事件。 */
    fun enterIfExpression(expression: CfirIfExpression) {
        if (hasActiveGraph) graphBuilder.enterIfExpression(expression).mergeIncomingFlow()
    }

    fun exitIfCondition(expression: CfirIfExpression) {
        if (hasActiveGraph) {
            val (conditionExit, enter) = graphBuilder.exitIfCondition(expression)
            conditionExit.mergeIncomingFlow()
            enter.mergeIncomingFlow { _, flow ->
                if (expression.condition.coneTypeOrNull?.isBoolean == true) {
                    flow.getKnownVariableIfUsed(expression.condition)?.let { flow.commitOperationStatement(it eq true) }
                }
            }
        }
    }

    fun exitIfThenBranch(expression: CfirIfExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitIfBranchResult(expression, isThen = true).mergeIncomingFlow()
            if (expression.elseBranch == null) return
            graphBuilder.enterIfElseBranch(expression).mergeIncomingFlow { _, flow ->
                if (expression.condition.coneTypeOrNull?.isBoolean == true) {
                    flow.getKnownVariableIfUsed(expression.condition)?.let { flow.commitOperationStatement(it eq false) }
                }
            }
        }
    }

    fun exitIfExpression(expression: CfirIfExpression, callCompleted: Boolean) {
        if (hasActiveGraph) {
            if (expression.elseBranch != null) {
                graphBuilder.exitIfBranchResult(expression, isThen = false).mergeIncomingFlow()
            }
            val result = graphBuilder.exitIfExpression(expression, callCompleted)
            result.lambdaExitNodes.forEach { it.mergeIncomingFlow() }
            result.value.mergeIncomingFlow()
        }
    }

    fun enterBooleanOperatorExpression(expression: CfirExpression) {
        if (hasActiveGraph) graphBuilder.enterBooleanOperatorExpression(expression).mergeIncomingFlow()
    }

    fun exitLeftBooleanOperatorExpressionArgument(expression: CfirExpression) {
        if (hasActiveGraph) {
            val (left, right) = graphBuilder.exitLeftBooleanOperatorExpressionArgument(expression)
            left.mergeIncomingFlow()
            right.mergeIncomingFlow { _, flow ->
                flow.getKnownVariableIfUsed(expression.booleanOperatorLeft)?.let {
                    flow.commitOperationStatement(it eq (expression.booleanOperatorKind == org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind.AND))
                }
            }
        }
    }

    fun exitBooleanOperatorExpression(expression: CfirExpression) {
        if (hasActiveGraph) graphBuilder.exitBooleanOperatorExpression(expression).mergeBooleanLogicOperatorFlow()
    }

    fun exitTypeOperatorCall(expression: CfirTypeOperator) {
        if (hasActiveGraph) graphBuilder.exitTypeOperatorCall(expression).mergeIncomingFlow(
            semanticTransferComplete = expression.operation == org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind.IS,
        ) { _, flow ->
            if (expression.operation == org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind.IS) {
                val type = expression.typeRef.coneTypeOrNull ?: return@mergeIncomingFlow
                val argument = flow.getExpressionCompletionOrNull(expression.argument)?.value ?: return@mergeIncomingFlow
                val result = SyntheticVariable(expression)
                flow.addImplication(result eq true implies (argument typeEq type))
                flow.addImplication(result eq false implies (argument typeNotEq type))
            }
        }
    }

    fun exitComparisonExpression(expression: CfirComparisonExpression) {
        if (hasActiveGraph) graphBuilder.exitComparisonExpression(expression).mergeIncomingFlow()
    }

    fun enterUnsafeExpression(expression: CfirUnsafeExpression) {
        if (hasActiveGraph) graphBuilder.enterUnsafeExpression(expression).mergeIncomingFlow()
    }

    fun exitUnsafeExpression(expression: CfirUnsafeExpression) {
        if (hasActiveGraph) graphBuilder.exitUnsafeExpression(expression).mergeIncomingFlow()
    }

    fun enterSynchronizedExpression(expression: CfirSynchronizedExpression) {
        if (hasActiveGraph) graphBuilder.enterSynchronizedExpression(expression).mergeIncomingFlow()
    }

    fun exitSynchronizedExpression(expression: CfirSynchronizedExpression) {
        if (hasActiveGraph) graphBuilder.exitSynchronizedExpression(expression).mergeIncomingFlow()
    }

    fun exitSpawnExpression(expression: CfirSpawnExpression) {
        if (hasActiveGraph) graphBuilder.exitSpawnExpression(expression).mergeIncomingFlow()
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
            graphBuilder.enterFile(file).mergeIncomingFlow()
        }
    }

    /**
     * 退出文件级 CFG 并返回构建结果。
     */
    fun exitFile(): ControlFlowGraph? {
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.File }
            ?.let {
                val (node, graph) = graphBuilder.exitFile()
                node.mergeIncomingFlow()
                resetSmartCastPosition()
                graph
            }
    }

    /**
     * 进入 class CFG 和局部变量赋值分析作用域。
     */
    fun enterClass(klass: CfirClass, buildGraph: Boolean) {
        if (buildGraph) {
            graphBuilder.enterClass(klass).mergeIncomingFlow()
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
            ?.let {
                val (node, graph) = graphBuilder.exitClass()
                node.mergeIncomingFlow()
                resetSmartCastPosition()
                graph
            }
    }

    /**
     * 进入函数 CFG 和局部变量赋值分析作用域。
     */
    @JvmOverloads
    fun enterFunction(function: CfirFunctionBodyOwner, calledInPlace: Boolean = false) {
        val assigned = variableAssignmentAnalyzer.enterFunction(function)
        val (local, enter) = if (function is CfirAnonymousFunction) {
            null to graphBuilder.enterAnonymousFunction(function, calledInPlace)
        } else {
            graphBuilder.enterFunction(function)
        }
        local?.mergeIncomingFlow()
        enter.mergeIncomingFlow { _, flow ->
            function.valueParameters.forEach { parameter ->
                val variable = flow.getLocal(parameter.symbol, create = true)
                if (variable != null && flow.getValueBinding(variable) == null) {
                    logicSystem.initializeEntry(flow, variable, parameter, variable.originalType)
                }
            }
            context.implicitValueStorage.implicitValues.forEach { value ->
                val variable = flow.remember(RealVariable.implicit(symbolMapper(value.boundSymbol), value.originalType))
                if (flow.getValueBinding(variable) == null) {
                    logicSystem.initializeEntry(flow, variable, function, value.originalType)
                }
            }
            if (function is CfirAnonymousFunction) enterRepeatableStatement(flow, assigned)
        }
    }

    /**
     * 退出函数 CFG 和局部变量赋值分析作用域，并返回函数 CFG 引用。
     */
    fun exitFunction(function: CfirFunctionBodyOwner): CfirControlFlowGraphReference? {
        variableAssignmentAnalyzer.exitFunction()
        val (exit, graph) = graphBuilder.exitFunction(function)
        exit.mergeIncomingFlow()
        resetSmartCastPosition()
        return CfirControlFlowGraphReferenceImpl(graph)
    }

    /** 在变量 initializer 求值前写入 CFG declaration enter 节点。 */
    /** 资源 PRIVATE 执行根独立于复制的 formal try.Main，入口流仍来自保存的父状态。 */
    fun enterResourceInitializer(resource: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable) =
        graphBuilder.enterResourceInitializer(resource)
    fun suspendResourceInitializer(resource: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable) =
        graphBuilder.suspendResourceInitializer(resource)
    fun <T> withResourceInitializer(resource: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable, action: () -> T): T =
        graphBuilder.withResourceInitializer(resource, action)
    fun finishResourceInitializer(resource: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable): ControlFlowGraph =
        graphBuilder.finishResourceInitializer(resource)

    fun enterVariableDeclaration(variable: CfirVariable) {
        if (hasActiveGraph) {
            graphBuilder.enterVariableDeclaration(variable).mergeIncomingFlow()
        }
    }

    /** 在变量已进入作用域且 initializer 完成求值后写入 CFG declaration exit 节点。 */
    fun exitVariableDeclaration(variable: CfirVariable) {
        exitVariableDeclaration(variable, runtimePatternBinding = false)
    }

    /** runtime pattern 的缺 initializer 是本轮投影绑定，不能按普通未初始化声明授 ready。 */
    fun exitPatternBindingDeclaration(variable: CfirVariable) {
        exitVariableDeclaration(variable, runtimePatternBinding = true)
    }

    /** 当前模式值/判别模型未闭合的实际事件保留图级义务；将来完成 transfer 后才可解除。 */
    fun requirePatternSemanticTransfer(pattern: org.cangnova.cangjie.cfir.patterns.CfirPattern) {
        if (hasActiveGraph) graphBuilder.currentGraph.requireSemanticTransfer(pattern)
    }

    private fun exitVariableDeclaration(variable: CfirVariable, runtimePatternBinding: Boolean) {
        if (hasActiveGraph) {
            graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow(
                semanticTransferComplete = variable !is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable &&
                    (!runtimePatternBinding || variable.initializer != null),
            ) { _, flow ->
                val local = flow.getLocal(variable.symbol, create = true) ?: return@mergeIncomingFlow
                // 声明的静态上界属于 storage 知识；没有 initializer 时仍不产生值绑定或完成事件。
                flow.addTypeStatement(local typeEq local.originalType)
                val initializer = variable.initializer ?: return@mergeIncomingFlow
                exitVariableInitialization(flow, initializer, variable, local, !variable.isTypeImplicit, isAssignment = false)
            }
        }
    }

    /**
     * 进入代码片段 CFG 和局部变量分析顶层作用域。
     */
    fun enterCodeFragment(codeFragment: CfirCodeFragment) {
        variableAssignmentAnalyzer.enterCodeFragment(codeFragment)
        graphBuilder.enterCodeFragment(codeFragment).mergeIncomingFlow()
    }

    /**
     * 退出代码片段 CFG 和局部变量分析顶层作用域。
     */
    fun exitCodeFragment(codeFragment: CfirCodeFragment): ControlFlowGraph {
        variableAssignmentAnalyzer.exitCodeFragment(codeFragment)
        val (exit, graph) = graphBuilder.exitCodeFragment()
        exit.mergeIncomingFlow()
        resetSmartCastPosition()
        return graph
    }

    /**
     * 进入匿名函数表达式 CFG 节点。
     */
    fun enterAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        if (hasActiveGraph) {
            val (expression, capture) = graphBuilder.enterAnonymousFunctionExpression(anonymousFunctionExpression)
            capture?.mergeIncomingFlow()
            expression?.mergeIncomingFlow()
        }
    }

    /**
     * 进入字段初始化器 CFG。
     */
    fun enterFieldInitializer(field: CfirFieldVariable) {
        if (hasActiveGraph) {
            graphBuilder.enterFieldInitializer(field).mergeIncomingFlow()
        }
    }

    /**
     * 退出字段初始化器 CFG。
     */
    fun exitFieldInitializer(): ControlFlowGraph? {
        return graphBuilder.currentGraphOrNull
            ?.takeIf { it.kind == ControlFlowGraph.Kind.FieldInitializer }
            ?.let {
                val (exit, graph) = graphBuilder.exitFieldInitializer()
                exit.mergeIncomingFlow()
                resetSmartCastPosition()
                graph
            }
    }

    /**
     * 进入值参数默认值 CFG。
     */
    fun enterValueParameter(parameter: CfirValueParameter) {
        if (!hasActiveGraph || parameter.defaultValue == null) return
        graphBuilder.enterValueParameter(parameter).mergeIncomingFlow()
        // 保留参数自己的已完成默认图，只为当前函数图生成参数边界节点。
        if (rebuildingResolvedGraph || parameter.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
            graphBuilder.enterDefaultArguments(parameter).mergeIncomingFlow()
        } else {
            checkNotNull(parameter.controlFlowGraphReference?.controlFlowGraph) {
                "A resolved parameter default must retain its completed control-flow graph."
            }
        }
    }

    /**
     * 退出值参数默认值 CFG。
     */
    fun exitValueParameter(parameter: CfirValueParameter): ControlFlowGraph? {
        if (!hasActiveGraph || parameter.defaultValue == null) return null
        val graph = if (rebuildingResolvedGraph || parameter.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
            val (exit, graph) = graphBuilder.exitDefaultArguments()
            exit.mergeIncomingFlow()
            graph
        } else {
            checkNotNull(parameter.controlFlowGraphReference?.controlFlowGraph)
        }
        graphBuilder.exitValueParameter(parameter,
            if (rebuildingResolvedGraph || parameter.resolvePhase < CfirResolvePhase.BODY_RESOLVE) graph else null,
        ).mergeIncomingFlow()
        return graph
    }

    /**
     * 进入不建图的通用循环赋值分析作用域。
     */
    fun enterLoop(loop: CfirIterationOwner) {
        variableAssignmentAnalyzer.enterLoop(loop)
    }

    /**
     * 退出不建图的通用循环赋值分析作用域。
     */
    fun exitLoop() {
        variableAssignmentAnalyzer.exitLoop()
    }

    /** For 的迭代、guard 与 body 使用实际节点建图，同时进入重复赋值作用域。 */
    fun enterForInLoop(loop: CfirForInExpression) {
        val assigned = variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            val (enter, iteration) = graphBuilder.enterForInLoop(loop)
            enter.mergeIncomingFlow()
            if (iteration.isEntered) iteration.mergeIncomingFlow(
                semanticTransferComplete = loop.iterationKind != CfirSourceForInKind.DIRECT,
            ) { _, flow -> enterRepeatableStatement(flow, assigned) }
        }
    }

    fun exitForInLoopIteration(loop: CfirForInExpression) {
        if (hasActiveGraph) graphBuilder.exitForInLoopIteration(loop).mergeIncomingFlow()
    }

    fun enterForInLoopIteration(loop: CfirForInExpression) {
        if (hasActiveGraph) graphBuilder.enterForInLoopIteration(loop).mergeIncomingFlow { _, flow ->
            if (loop.iterationKind == CfirSourceForInKind.ITERATOR) {
                enterRepeatableStatement(flow, variableAssignmentAnalyzer.assignedInsideLoop(loop))
            }
        }
    }

    fun exitForInLoopInitialCheck(loop: CfirForInExpression) {
        if (hasActiveGraph) graphBuilder.exitForInLoopInitialCheck(loop).mergeIncomingFlow()
    }

    fun enterForInLoopCondition(loop: CfirForInExpression) {
        if (hasActiveGraph) graphBuilder.enterForInLoopCondition(loop).mergeIncomingFlow { _, flow ->
            enterRepeatableStatement(flow, variableAssignmentAnalyzer.assignedInsideLoop(loop))
        }
    }

    /** 内部 RANGE Cond 使用保存的比较操作和当前声明值。 */
    fun exitForInLoopRangeCheck(comparison: CfirBinaryExpression, index: CfirVariable, bound: CfirVariable) {
        if (hasActiveGraph) graphBuilder.exitForInLoopRangeCheck(comparison, index, bound).mergeIncomingFlow()
    }

    fun exitForInLoopStringCheck(loop: CfirForInExpression, index: CfirVariable, bound: CfirVariable) {
        if (hasActiveGraph) graphBuilder.exitForInLoopStringCheck(loop, index, bound).mergeIncomingFlow()
    }

    fun exitForInLoopStringUpdate(loop: CfirForInExpression, index: CfirVariable) {
        if (hasActiveGraph) graphBuilder.exitForInLoopStringUpdate(loop, index).mergeIncomingFlow { _, flow ->
            val variable = checkNotNull(flow.getLocal(index.symbol, create = false))
            logicSystem.recordNewAssignment(flow, variable, context.dataFlowAnalyzerContext.newAssignmentIndex())
            flow.addTypeStatement(variable typeEq context.returnTypeCalculator.tryCalculateReturnType(index).coneType)
        }
    }

    /** 借用 reference 保留原身份，有效 target 从同次符号投影获得。 */
    fun exitForInLoopIteratorCheck(loop: CfirForInExpression, result: CfirExpression, constructor: CfirResolvedNamedReference,
        target: org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>) {
        if (hasActiveGraph) graphBuilder.exitForInLoopIteratorCheck(loop, result, constructor, target)
            .mergeIncomingFlow(semanticTransferComplete = false)
    }

    fun exitForInLoopBody(loop: CfirForInExpression) {
        if (hasActiveGraph) graphBuilder.exitForInLoopBody(loop).mergeIncomingFlow()
    }

    /** 完成表达式按实际 kind 发语义事件，flow 产品由同一个 DFA owner 继续构建。 */
    fun exitCallExpression(expression: CfirCallExpression) {
        if (hasActiveGraph) graphBuilder.exitCallExpression(expression).mergeIncomingFlow(semanticTransferComplete = false)
    }

    fun exitUnaryExpression(expression: CfirUnaryExpression) {
        if (hasActiveGraph) graphBuilder.exitUnaryExpression(expression).mergeIncomingFlow { _, flow ->
            if (expression.operation == org.cangnova.cangjie.cfir.source.CfirSourceOperator.NOT) {
                val argument = flow.getKnownVariableIfUsed(expression.operand) ?: return@mergeIncomingFlow
                val result = SyntheticVariable(expression)
                logicSystem.translateVariableFromConditionInStatements(flow, argument, result) {
                    when (it.condition.operation) {
                        Operation.EqTrue -> result eq false implies it.effect
                        Operation.EqFalse -> result eq true implies it.effect
                        else -> null
                    }
                }
            }
        }
    }

    fun exitBinaryExpression(expression: CfirBinaryExpression) {
        if (hasActiveGraph) graphBuilder.exitBinaryExpression(expression).mergeIncomingFlow()
    }

    fun exitForInLoopGuard(loop: CfirForInExpression) {
        if (hasActiveGraph) {
            val (guard, body) = graphBuilder.exitForInLoopGuard(loop)
            guard?.mergeIncomingFlow()
            body.mergeIncomingFlow { _, flow ->
                loop.patternGuard?.takeIf { it.coneTypeOrNull?.isBoolean == true }?.let { expression ->
                    flow.getKnownVariableIfUsed(expression)?.let { flow.commitOperationStatement(it eq true) }
                }
            }
        }
    }

    fun exitForInLoop(loop: CfirForInExpression) {
        variableAssignmentAnalyzer.exitLoop()
        if (hasActiveGraph) graphBuilder.exitForInLoop(loop).third.mergeIncomingFlow()
    }

    /**
     * 进入 while 循环 CFG 和赋值分析作用域。
     */
    fun enterWhileLoop(loop: CfirLoopExpression) {
        val assigned = variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            val (enter, condition) = graphBuilder.enterWhileLoop(loop)
            enter.mergeIncomingFlow()
            condition.mergeIncomingFlow { _, flow -> enterRepeatableStatement(flow, assigned) }
        }
    }

    /**
     * 退出 while 条件 CFG 节点。
     */
    fun exitWhileLoopCondition(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            val (condition, body) = graphBuilder.exitWhileLoopCondition(loop)
            condition.mergeIncomingFlow()
            body.mergeIncomingFlow { _, flow ->
                if (loop.condition.coneTypeOrNull?.isBoolean == true) {
                    flow.getKnownVariableIfUsed(loop.condition)?.let { flow.commitOperationStatement(it eq true) }
                }
            }
        }
    }

    /**
     * 退出 while 循环 CFG 和赋值分析作用域。
     */
    fun exitWhileLoop(loop: CfirLoopExpression) {
        val assigned = variableAssignmentAnalyzer.exitLoop()
        if (hasActiveGraph) {
            val (condition, body, exit) = graphBuilder.exitWhileLoop(loop)
            body.mergeIncomingFlow()
            exit.mergeIncomingFlow { path, flow ->
                processWhileLoopExit(path, flow, exit, condition, assigned)
                processLoopExit(flow, exit, exit.previousNodes.filterIsInstance<LoopConditionExitNode>().single())
            }
        }
    }

    /**
     * 进入 do-while 循环 CFG 和赋值分析作用域。
     */
    fun enterDoWhileLoop(loop: CfirLoopExpression) {
        val assigned = variableAssignmentAnalyzer.enterLoop(loop)
        if (hasActiveGraph) {
            val (enter, body) = graphBuilder.enterDoWhileLoop(loop)
            enter.mergeIncomingFlow { _, flow -> enterRepeatableStatement(flow, assigned) }
            body.mergeIncomingFlow()
        }
    }

    /**
     * 进入 do-while 条件 CFG 节点。
     */
    fun enterDoWhileLoopCondition(loop: CfirLoopExpression) {
        if (hasActiveGraph) {
            val (body, condition) = graphBuilder.enterDoWhileLoopCondition(loop)
            body.mergeIncomingFlow()
            condition.mergeIncomingFlow()
        }
    }

    /**
     * 退出 do-while 循环 CFG 和赋值分析作用域。
     */
    fun exitDoWhileLoop(loop: CfirLoopExpression) {
        variableAssignmentAnalyzer.exitLoop()
        if (hasActiveGraph) {
            val (condition, exit) = graphBuilder.exitDoWhileLoop(loop)
            condition.mergeIncomingFlow()
            exit.mergeIncomingFlow { _, flow -> processLoopExit(flow, exit, condition) }
        }
    }

    /**
     * 进入 match 表达式 CFG。
     */
    fun enterMatchExpression(matchExpression: CfirMatchExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterMatchExpression(matchExpression).mergeIncomingFlow()
        }
    }

    /**
     * 进入 match 分支条件 CFG。
     */
    fun enterMatchBranchCondition(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.enterMatchBranchCondition(branch).mergeIncomingFlow()
        }
    }

    /**
     * 退出 match 分支条件 CFG。
     */
    fun exitMatchBranchCondition(branch: CfirMatchBranch, resolveGuard: () -> Unit) {
        if (hasActiveGraph) {
            val result = graphBuilder.exitMatchBranchCondition(branch,
                prepareGuard = { nodes, enter ->
                    nodes.forEach {
                        it.mergeIncomingFlow(semanticTransferComplete = it.pattern is org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern)
                    }
                    enter?.mergeIncomingFlow()
                }, resolveGuard = resolveGuard)
            result.guardDecision?.mergeIncomingFlow()
            result.failure.mergeIncomingFlow()
            result.conditionExit.mergeIncomingFlow()
            result.resultEnter.mergeIncomingFlow()
        } else {
            resolveGuard()
        }
    }

    /**
     * 退出 match 分支结果 CFG。
     */
    fun exitMatchBranchResult(branch: CfirMatchBranch) {
        if (hasActiveGraph) {
            graphBuilder.exitMatchBranchResult(branch).mergeIncomingFlow()
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
            val result = graphBuilder.exitMatchExpression(matchExpression, syntheticElseDecision, callCompleted)
            result.lambdaExitNodes.forEach { it.mergeIncomingFlow() }
            result.value.second?.mergeIncomingFlow()
            result.value.first.mergeIncomingFlow()
        }
    }

    /**
     * 进入 try 表达式 CFG。
     */
    fun enterTryExpression(tryExpression: CfirTryExpression) {
        if (hasActiveGraph) {
            val (enter, body) = graphBuilder.enterTryExpression(tryExpression)
            enter.mergeIncomingFlow()
            body.mergeIncomingFlow()
        }
    }

    /**
     * 退出 try 主体 CFG。
     */
    fun exitTryMainBlock() {
        if (hasActiveGraph) {
            graphBuilder.exitTryMainBlock().mergeIncomingFlow()
        }
    }

    /**
     * 进入 catch 子句 CFG。
     */
    fun enterCatchClause(catch: CfirCatch) {
        if (hasActiveGraph) {
            val catchEnter = graphBuilder.enterCatchClause(catch)
            graphBuilder.refineExceptionDispatch(catchEnter) { actual, expected ->
                AbstractTypeChecker.isSubtypeOf(session.typeContext, actual, expected)
            }
            catchEnter.mergeIncomingFlow(semanticTransferComplete = false)
        }
    }

    /**
     * 退出 catch 子句 CFG。
     */
    fun exitCatchClause(catch: CfirCatch) {
        if (hasActiveGraph) {
            graphBuilder.exitCatchClause(catch).mergeIncomingFlow()
        }
    }

    /**
     * 进入 handle 子句 CFG。
     */
    fun enterHandleClause(handleClause: CfirHandleClause) {
        if (hasActiveGraph) {
            graphBuilder.enterHandleClause(handleClause).mergeIncomingFlow(semanticTransferComplete = false)
        }
    }

    /**
     * 退出 handle 子句 CFG。
     */
    fun exitHandleClause(handleClause: CfirHandleClause) {
        if (hasActiveGraph) {
            graphBuilder.exitHandleClause(handleClause).mergeIncomingFlow()
        }
    }

    /**
     * 进入 finally block CFG。
     */
    fun enterFinallyBlock() {
        if (hasActiveGraph) {
            val enter = graphBuilder.enterFinallyBlock()
            enter.mergeIncomingFlow()
        }
    }

    /**
     * 退出 finally block CFG。
     */
    fun exitFinallyBlock() {
        if (hasActiveGraph) {
            graphBuilder.exitFinallyBlock().mergeIncomingFlow()
        }
    }

    /**
     * 退出 try 表达式 CFG。
     */
    fun exitTryExpression(callCompleted: Boolean) {
        if (hasActiveGraph) {
            val result = graphBuilder.exitTryExpression(callCompleted)
            result.lambdaExitNodes.forEach { it.mergeIncomingFlow() }
            result.value.mergeIncomingFlow()
        }
    }

    /**
     * 进入可选链表达式 CFG。
     */
    fun enterOptionalChain(optionalChainExpression: CfirOptionalChainExpression) {
        if (hasActiveGraph) {
            graphBuilder.enterOptionalChain(optionalChainExpression).mergeIncomingFlow(semanticTransferComplete = false)
        }
    }

    /**
     * 退出可选链表达式 CFG。
     */
    fun exitOptionalChain(optionalChainExpression: CfirOptionalChainExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitOptionalChain(optionalChainExpression).mergeIncomingFlow()
        }
    }

    /**
     * 退出包装表达式 CFG。
     */
    fun exitWrappedExpression(wrappedExpression: CfirWrappedExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitWrappedExpression(wrappedExpression).mergeIncomingFlow()
        }
    }

    /**
     * 退出 jump 表达式 CFG。
     */
    fun exitJump(jump: CfirJump<*>) {
        if (hasActiveGraph) {
            graphBuilder.exitJump(jump).mergeIncomingFlow()
        }
    }

    /**
     * 退出 throw 表达式 CFG。
     */
    fun exitThrowException(throwExpression: CfirThrowExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitThrowExceptionNode(throwExpression).mergeIncomingFlow()
        }
    }

    /**
     * 退出 qualified access 表达式 CFG。
     */
    fun exitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow()
        }
    }

    /**
     * 退出字面量表达式 CFG。
     */
    fun exitLiteralExpression(literalExpression: CfirLiteralExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitLiteralExpression(literalExpression).mergeIncomingFlow()
        }
    }

    fun enterTupleLiteral(tuple: CfirTupleLiteral) {
        if (hasActiveGraph) graphBuilder.enterTupleLiteral(tuple).mergeIncomingFlow()
    }

    fun exitTupleLiteralElement(tuple: CfirTupleLiteral, index: Int, element: CfirExpression) {
        if (hasActiveGraph) graphBuilder.exitTupleLiteralElement(tuple, index, element).mergeIncomingFlow()
    }

    fun exitTupleLiteral(tuple: CfirTupleLiteral) {
        if (hasActiveGraph) graphBuilder.exitTupleLiteral(tuple).mergeIncomingFlow()
    }

    /**
     * 退出变量赋值 CFG。
     */
    fun exitVariableAssignment(assignment: CfirAssignment) {
        val access = assignment.lValue as? CfirQualifiedAccessExpression
        val declaration = (access?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
            ?.let(symbolMapper)?.cfir as? CfirCallableDeclaration
        if (hasActiveGraph) {
            graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow(
                semanticTransferComplete = assignment.lValue !is org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral,
            ) { _, flow ->
                if (declaration !is CfirVariable && declaration !is CfirProperty) return@mergeIncomingFlow
                val variable = if (declaration.isLocal || !declaration.isMutableValue) {
                    flow.rememberVariableWithoutUnwrappingAlias(assignment.lValue)
                } else flow.getKnownVariableWithoutUnwrappingAlias(assignment.lValue)
                if (variable is RealVariable) {
                    if (declaration.isLocal || !declaration.isMutableValue) {
                        exitVariableInitialization(flow, assignment.rValue, declaration, variable, hasExplicitType = false, isAssignment = true)
                    } else {
                        logicSystem.recordNewAssignment(flow, variable, context.dataFlowAnalyzerContext.newAssignmentIndex())
                    }
                }
            }
        }
    }

    /**
     * 退出自增/自减表达式的 CFG。
     *
     * `CfirIncrementDecrementExpression` 不会转换成普通 assignment，因此必须通过专用
     * 节点向 CFG 暴露它对左值的写入效果。
     */
    fun exitIncrementDecrementExpression(expression: CfirIncrementDecrementExpression) {
        if (hasActiveGraph) {
            graphBuilder.exitIncrementDecrementExpression(expression).mergeIncomingFlow { _, flow ->
                val variable = flow.getKnownVariableWithoutUnwrappingAlias(expression.expression)
                if (variable is RealVariable) logicSystem.recordNewAssignment(flow, variable, context.dataFlowAnalyzerContext.newAssignmentIndex())
            }
        }
    }

    /**
     * 把赋值表达式右侧类型记录到局部变量赋值分析器。
     */
    fun recordAssignment(assignment: CfirAssignment) {
        val rootType = assignment.rValue.coneTypeOrNull ?: return
        CfirAssignmentExecution.of(assignment).targets.forEach { target ->
            val access = target.expression as? CfirQualifiedAccessExpression ?: return@forEach
            val reference = access.calleeReference as? CfirResolvedNamedReference ?: return@forEach
            val declaration = symbolMapper(reference.resolvedSymbol).cfir as? CfirCallableDeclaration ?: return@forEach
            recordAssignmentTarget(assignment, target.path, declaration, rootType)
        }
    }

    /** 已解析 replay 的赋值目标与 mini 使用同一有效声明，不改写借用的 lhs。 */
    fun recordAssignment(assignment: CfirAssignment, declaration: org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration) {
        val type = assignment.rValue.coneTypeOrNull ?: return
        check(assignment.lValue !is org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral) {
            "A tuple assignment requires a declaration for each actual target"
        }
        recordAssignmentTarget(assignment, emptyList(), declaration, type)
    }

    /** 每个目标只消费自己路径的完成类型，诊断中的错误结构不产生虚假的叶类型。 */
    private fun recordAssignmentTarget(assignment: CfirAssignment, path: List<Int>, declaration: CfirCallableDeclaration, rootType: ConeCangJieType) {
        var type = rootType
        for (index in path) {
            val tuple = type.fullyExpandedType(session) as? ConeTupleType ?: return
            type = tuple.elementTypes.getOrNull(index) ?: return
        }
        variableAssignmentAnalyzer.visitAssignment(declaration, AssignmentOccurrence(assignment, path), type)
    }

    /** raw 与正常降低的写事件在 mini 中保持同一次身份，不按 source offset 或填充顺序猜测。 */
    fun rebindAssignment(original: org.cangnova.cangjie.cfir.CfirElement, replacement: org.cangnova.cangjie.cfir.CfirElement) {
        variableAssignmentAnalyzer.rebindAssignment(original, replacement)
    }

    /** 子图退出和 partial restore 后恢复真实外层解析位置。 */
    fun resetSmartCastPosition() {
        resetSmartCastPositionTo(graphBuilder.lastNodeOrNull?.flow)
    }

    private fun CFGNode<*>.buildDefaultFlow(builder: (FlowPath, MutableFlow) -> Unit): MutableFlow {
        val previousFlows = mutableListOf<PersistentFlow>()
        val statementFlows = mutableListOf<PersistentFlow>()
        val results = mutableListOf<ExpressionCompletion?>()
        val requiredResults = mutableListOf<Boolean>()
        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (!usedInDfa(edge)) continue
            val previous = if (node is FinallyBlockExitNode) {
                val path = FlowPath.CfgEdge(edge.label, node.fir)
                val alternate = node.getAlternateFlow(path)
                check(path !in node.requiredAlternateFlowPaths || alternate != null) { "A required finally path is incomplete" }
                alternate ?: node.flow
            } else node.flow
            val flow = flowOnIncomingEdge(node, edge, previous)
            previousFlows += flow
            if (edge.label != PostponedPath) statementFlows += flow
            if (this is IfExitNode) {
                requiredResults += resultSources.containsKey(node)
                results += resultFrom(node, flow)
            }
        }
        val resultInput = if (this is IfExitNode) ExpressionJoinInput(fir, results, requiredResults) else null
        val result = logicSystem.joinFlow(previousFlows, statementFlows, isUnion, fir, resultInput)
        if (graphBuilder.lastNodeOrNull === this) {
            if (currentSmartCastPosition == null || currentSmartCastPosition != previousFlows.singleOrNull()) {
                resetSmartCastPositionTo(result)
            } else currentSmartCastPosition = result
        }
        builder(FlowPath.Default, result)
        return result
    }

    private fun CFGNode<*>.buildAlternateFlow(path: FlowPath.CfgEdge, builder: (FlowPath, MutableFlow) -> Unit): MutableFlow {
        val alternateStart = this is FinallyBlockEnterNode
        val previousFlows = mutableListOf<PersistentFlow>()
        val statementFlows = mutableListOf<PersistentFlow>()
        val results = mutableListOf<ExpressionCompletion?>()
        val requiredResults = mutableListOf<Boolean>()
        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (!usedInDfa(edge)) continue
            if (alternateStart) {
                if (path.fir === fir && edge.label != path.label) continue
                if (path.fir !== fir && !edge.label.continuesAlternateFlow) continue
            }
            val alternate = node.getAlternateFlow(path)
            check(path !in node.requiredAlternateFlowPaths || alternate != null) { "A required incoming alternate path is incomplete" }
            val flow = flowOnIncomingEdge(node, edge, alternate ?: node.flow)
            previousFlows += flow
            if (edge.label != PostponedPath) statementFlows += flow
            if (this is IfExitNode) {
                requiredResults += resultSources.containsKey(node)
                results += resultFrom(node, flow)
            }
        }
        val resultInput = if (this is IfExitNode) ExpressionJoinInput(fir, results, requiredResults) else null
        return logicSystem.joinFlow(previousFlows, statementFlows, isUnion, fir, resultInput).also { builder(path, it) }
    }

    /** 值来源由builder记录的真实分支出口决定；原语法无else时只产生其实际Unit结果。 */
    private fun IfExitNode.resultFrom(node: CFGNode<*>, flow: PersistentFlow): ExpressionCompletion? {
        if (!resultSources.containsKey(node)) return null
        val expression = resultSources[node]
        if (expression != null) return flow.getExpressionCompletionOrNull(expression)
        if (fir.coneTypeOrNull?.isUnit != true) return null
        val type = checkNotNull(fir.coneTypeOrNull)
        return ExpressionCompletion(CompletedValueVariable(fir, type), type, ExpressionEvent(fir, node.fir))
    }

    /** guard 分支在合流前分别批准，不能把 latch 的所有输入一并当作 guard=false。 */
    private fun CFGNode<*>.flowOnIncomingEdge(previous: CFGNode<*>, edge: Edge, source: PersistentFlow): PersistentFlow {
        val condition: CfirExpression
        val value: Boolean
        when {
            previous is ForInLoopGuardExitNode && this is LoopRepeatEnterNode<*> && fir === previous.loop -> {
                condition = previous.fir
                value = false
            }
            previous is MatchPatternDecisionNode && previous.guard != null &&
                (edge.label == MatchBranchSuccess || edge.label == MatchBranchFailure) -> {
                condition = checkNotNull(previous.guard)
                value = edge.label == MatchBranchSuccess
            }
            else -> return source
        }
        if (condition.coneTypeOrNull?.isBoolean != true) return source
        val variable = source.getKnownVariableIfUsed(condition) ?: return source
        return source.fork().also { it.commitOperationStatement(variable eq value) }.freeze()
    }

    /** 固定 Kotlin 的 incoming→transfer→freeze；不读取未进入的回边或补空流。 */
    private fun CFGNode<*>.mergeIncomingFlow(
        semanticTransferComplete: Boolean = true,
        builder: (FlowPath, MutableFlow) -> Unit = { _, _ -> },
    ) {
        var positionBeforeTransfer: PersistentFlow? = null
        var valuesReady = true
        val transfer: (FlowPath, MutableFlow) -> Unit = { path, flow ->
            // buildDefaultFlow已同步合流后的implicit栈；比较基线必须是尚未执行transfer的不可变P1。
            if (path === FlowPath.Default && currentSmartCastPosition === flow) positionBeforeTransfer = flow.freeze()
            val expression = valueExpression()
            val joinedResult = this is IfExitNode && flow.getExpressionCompletionOrNull(fir) != null
            if (!joinedResult) expression?.let { logicSystem.retireExpression(flow, it) }
            if (this is TupleLiteralEnterNode || this is TupleLiteralExitNode) {
                logicSystem.retireExpression(flow, fir as CfirTupleLiteral)
            }
            val event = expression?.takeUnless { joinedResult || isDead || it.coneTypeOrNull?.isNothing == true }
                ?.let { beginValue(path, flow, it) }
            if (expression != null && !joinedResult && !isDead && expression.coneTypeOrNull?.isNothing != true && event == null) valuesReady = false
            if (!isDead || this !is VariableAssignmentNode && this !is VariableDeclarationExitNode && this !is IncrementDecrementNode) {
                builder(path, flow)
            }
            if (event != null) logicSystem.completeExpression(flow, event)
            if (this is TupleLiteralElementExitNode && !isDead && flow.getExpressionCompletionOrNull(fir) == null) valuesReady = false
            if (this is TupleLiteralExitNode && !isDead) {
                val captured = elementExitNodes.map { it.getFlow(path).getExpressionCompletionOrNull(it.fir) }
                if (captured.any { it == null }) valuesReady = false
                else logicSystem.recordTuple(flow, fir, captured.filterNotNull())
            }
        }
        val mutable = buildDefaultFlow(transfer)
        val completed = mutable.freeze().also { flow = it }
        if (currentSmartCastPosition === mutable) {
            updateImplicitTypes(positionBeforeTransfer, completed)
            currentSmartCastPosition = completed
        }
        propagateAlternateFlows(transfer)
        if (this is FinallyBlockEnterNode) createAlternateFlows(transfer)
        dataFlowAnalysisState = if (semanticTransferComplete && valuesReady && hasCompletedRequiredAlternateFlows() && previousNodes.all {
                !usedInDfa(edgeFrom(it)) || it.dataFlowAnalysisState == DataFlowAnalysisState.SEMANTICS_READY
            }) DataFlowAnalysisState.SEMANTICS_READY else DataFlowAnalysisState.CONTROL_FLOW_READY
    }

    /** 只有实际表达式出口产生值；guard/loop决策和逐元素捕获不能重复计算原表达式。 */
    private fun CFGNode<*>.valueExpression(): CfirExpression? = when (this) {
        is LiteralExpressionNode, is QualifiedAccessNode, is FunctionCallExitNode,
        is TypeOperatorCallNode, is ComparisonExpressionNode, is BooleanOperatorExitNode,
        is BinaryExpressionNode, is UnaryExpressionNode, is CallExpressionNode,
        is WrappedExpressionNode, is BlockExitNode, is IfExitNode, is MatchExitNode,
        is TryExpressionExitNode, is UnsafeExitNode, is SynchronizedExitNode,
        is SpawnExpressionNode, is AnonymousFunctionExpressionNode, is AnonymousFunctionCaptureNode,
        is VariableAssignmentNode, is IncrementDecrementNode -> fir as CfirExpression
        else -> null
    }

    /** begin与finish处于同一真实transfer；复用稳定绑定仍分配独立的本次读取事件。 */
    private fun CFGNode<*>.beginValue(path: FlowPath, flow: MutableFlow, expression: CfirExpression): ExpressionEvent? {
        val type = expression.coneTypeOrNull ?: return null
        // 目标的声明/地址知识先登记；base失败只拒绝本次值，不删除合法的Real知识节点。
        val accessVariable = if (this is QualifiedAccessNode) flow.rememberVariableWithoutUnwrappingAlias(expression) else null
        if (this is IfExitNode || this is MatchExitNode || this is TryExpressionExitNode ||
            this is CallExpressionNode || this is AnonymousFunctionCaptureNode || this is AnonymousFunctionExpressionNode) {
            graphBuilder.currentGraph.requireSemanticTransfer(expression)
            return null
        }
        val operands = when (expression) {
            is CfirTypeOperator -> listOf(expression.argument)
            is CfirUnaryExpression -> listOf(expression.operand)
            is CfirBinaryExpression -> if (this is BooleanOperatorExitNode) emptyList() else listOf(expression.left, expression.right)
            is CfirFunctionCall -> expression.argumentList.arguments
            is CfirQualifiedAccessExpression -> expression.runtimeReceiversForDfa()
            else -> emptyList()
        }
        if (operands.any { flow.getExpressionCompletionOrNull(it) == null }) {
            graphBuilder.currentGraph.requireSemanticTransfer(expression)
            return null
        }
        if (this is BooleanOperatorExitNode) {
            val leftComplete = leftOperandNode.getFlow(path).getExpressionCompletionOrNull(fir.booleanOperatorLeft)
            val rightComplete = if (rightOperandNode.isDead) null else rightOperandNode.getFlow(path).getExpressionCompletionOrNull(fir.booleanOperatorRight)
            if (leftComplete == null || !rightOperandNode.isDead && rightComplete == null) {
                graphBuilder.currentGraph.requireSemanticTransfer(expression)
                return null
            }
        }
        val child = when (expression) {
            is CfirWrappedExpression -> expression.expression
            is CfirBlock -> (expression.statements.lastOrNull() as? CfirExpression).takeUnless { type.isUnit }
            is CfirUnsafeExpression -> expression.body
            is CfirSynchronizedExpression -> expression.body
            else -> null
        }
        if (child != null) {
            val completed = flow.getExpressionCompletionOrNull(child) ?: return null
            return logicSystem.beginCapturedExpression(flow, expression, ValueUse(completed.value, type))
        }
        if (this is QualifiedAccessNode) {
            val variable = accessVariable
            if (variable is RealVariable) {
                val declaration = variable.symbol.cfir as? CfirCallableDeclaration
                val stable = variable.getStability(flow, session) == CfirSmartcastStability.STABLE_VALUE &&
                    !variableAssignmentAnalyzer.isUnstableInCurrentScope(variable.symbol.cfir, null, session)
                if (flow.getValueBinding(variable) != null) {
                    if (stable) return logicSystem.beginStableRead(flow, expression, variable, type)
                } else if (declaration is CfirVariable || declaration is CfirProperty) {
                    val initialization = externalReadInitialization(flow, expression as CfirQualifiedAccessExpression, variable, declaration)
                    if (initialization == null) {
                        graphBuilder.currentGraph.requireSemanticTransfer(expression)
                        return null
                    }
                    if (stable) {
                        logicSystem.observeExternalStableValue(flow, variable, expression, type, initialization)
                        return logicSystem.beginStableRead(flow, expression, variable, type)
                    }
                }
            }
        }
        return logicSystem.beginExpression(flow, expression, type)
    }

    /**
     * N 初始化检查的外部入口例外：导入/FFI/已初始化声明，或构造完成对象的成员。
     * own this 在构造期间必须已经有该 field 的实际 entry/store；最终声明位不证明这个实例。
     */
    private fun externalReadInitialization(flow: Flow, access: CfirQualifiedAccessExpression,
        variable: RealVariable, declaration: CfirCallableDeclaration): ExternalReadInitialization? {
        if (declaration.isLocal) return null
        val receiver = access.dispatchReceiver
        val receiverVariable = variable.dispatchReceiver
        val constructingOwnReceiver = receiverVariable?.isImplicit == true &&
            context.containers.any { it is CfirConstructor } &&
            receiverVariable.symbol == context.containingClassLikeDeclaration?.symbol
        if (constructingOwnReceiver) return null
        val facts = declaration.exportFacts
        if (facts.isInitialized || facts.isImported || declaration.status.isForeign) {
            return ExternalReadInitialization.Declaration(declaration)
        }
        if (receiver != null && variable.dispatchReceiver != null) {
            val completed = flow.getExpressionCompletionOrNull(receiver) ?: return null
            return ExternalReadInitialization.Receiver(completed)
        }
        return null
    }

    private fun CFGNode<*>.propagateAlternateFlows(builder: (FlowPath, MutableFlow) -> Unit) {
        val propagated = mutableSetOf<FlowPath>()
        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (!edge.label.continuesAlternateFlow || !usedInDfa(edge)) continue
            for (path in node.requiredAlternateFlowPaths) {
                if (path !is FlowPath.CfgEdge || !graphBuilder.withinFinallyBlock(path.fir)) continue
                if (propagated.add(path)) {
                    requireAlternateFlow(path)
                    addAlternateFlow(path, buildAlternateFlow(path, builder).freeze())
                }
            }
        }
    }

    private fun CFGNode<*>.createAlternateFlows(builder: (FlowPath, MutableFlow) -> Unit = { _, _ -> }) {
        val labels = mutableSetOf<EdgeLabel>()
        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (edge.label == UncaughtExceptionPath || !usedInDfa(edge)) continue
            if (labels.add(edge.label)) {
                val path = FlowPath.CfgEdge(edge.label, fir)
                requireAlternateFlow(path)
                addAlternateFlow(path, buildAlternateFlow(path, builder).freeze())
            }
        }
    }

    /** 仓颉模式判定的成功/失败是 finally 内的普通分叉，跳转/异常/postponed 标签仍另行处理。 */
    private val EdgeLabel.continuesAlternateFlow: Boolean
        get() = this == NormalPath || this == MatchBranchSuccess || this == MatchBranchFailure

    private fun CFGNode<*>.getFlow(path: FlowPath): PersistentFlow = when (path) {
        FlowPath.Default -> flow
        else -> getAlternateFlow(path) ?: error("No alternate flow for $path")
    }

    @OptIn(ImplicitValue.ImplicitValueInternals::class)
    private fun implicitUpdated(statement: TypeStatement) {
        val variable = statement.variable as? RealVariable ?: error("Implicit flow statement requires a real variable")
        context.implicitValueStorage.replaceImplicitValueType(variable.symbol, statement.smartCastedType(session.typeContext))
    }

    private fun resetSmartCastPositionTo(flow: Flow?) {
        val previous = currentSmartCastPosition
        if (previous === flow) return
        updateImplicitTypes(previous, flow)
        currentSmartCastPosition = flow
    }

    private fun updateImplicitTypes(previous: Flow?, flow: Flow?) {
        context.implicitValueStorage.implicitValues.forEach { value ->
            val variable = RealVariable.implicit(symbolMapper(value.boundSymbol), value.originalType)
            val statement = flow?.getTypeStatement(variable)
            if (statement != previous?.getTypeStatement(variable)) implicitUpdated(statement ?: MutableTypeStatement(variable))
        }
    }

    private fun MutableFlow.addImplication(implication: Implication) = logicSystem.addImplication(this, implication)

    private fun MutableFlow.addTypeStatement(statement: TypeStatement) {
        val added = logicSystem.addTypeStatement(this, statement) ?: return
        val variable = added.variable
        if (variable is ValueVariable && this === currentSmartCastPosition) {
            getBoundImplicitVariables(variable).forEach { implicit ->
                getTypeStatement(implicit)?.let(::implicitUpdated)
            }
        }
    }

    private fun MutableFlow.addAllStatements(statements: TypeStatements) = statements.values.forEach { addTypeStatement(it) }

    private fun MutableFlow.addAllConditionally(condition: OperationStatement, statements: TypeStatements) =
        statements.values.forEach { addImplication(condition implies it) }

    private fun MutableFlow.addAllConditionally(condition: OperationStatement, from: Flow) =
        addAllConditionally(condition, getTypeStatementsNotInheritedFrom(from))

    private fun MutableFlow.getTypeStatementsNotInheritedFrom(parent: Flow): TypeStatements = buildMap {
        parent.knownVariables.forEach { variable ->
            if (unwrapVariable(variable) != variable) return@forEach
            val statement = parent.getTypeStatement(variable)
            if (statement != null && statement != getTypeStatement(variable)) put(variable, statement)
        }
    }

    private fun MutableFlow.commitOperationStatement(statement: OperationStatement) =
        addAllStatements(logicSystem.approveOperationStatement(this, statement, removeApprovedOrImpossible = true))

    fun Flow.getKnownVariableWithoutUnwrappingAlias(expression: CfirExpression): DataFlowVariable? =
        getVariableIfKnown(expression, unwrapAliasInReceivers = { unwrapReceiverAddress(it, session) }, symbolMapper = symbolMapper)

    private fun MutableFlow.rememberVariableWithoutUnwrappingAlias(expression: CfirExpression): DataFlowVariable? =
        remember(expression, unwrapAliasInReceivers = { unwrapReceiverAddress(it, session) }, symbolMapper = symbolMapper)

    private fun Flow.getKnownVariableIfUsed(expression: CfirExpression): DataFlowVariable? =
        getExpressionCompletionOrNull(expression)?.value?.takeIf { !getImplications(it).isNullOrEmpty() }

    private fun MutableFlow.rememberVariableIfUsedOrReal(expression: CfirExpression): DataFlowVariable? =
        rememberVariable(expression)?.takeIf { it.isReal() || !getImplications(it).isNullOrEmpty() }

    private fun MutableFlow.rememberVariable(expression: CfirExpression): DataFlowVariable? =
        remember(expression, unwrapAlias = { unwrapVariableIfStable(it) }, unwrapAliasInReceivers = { unwrapReceiverAddress(it, session) }, symbolMapper = symbolMapper)

    fun getOrCreateVariable(expression: CfirExpression): DataFlowVariable? {
        val flow = currentSmartCastPosition ?: return null
        return flow.getVariable(expression, unwrapAlias = { flow.unwrapVariableIfStable(it) }, unwrapAliasInReceivers = { flow.unwrapReceiverAddress(it, session) }, symbolMapper = symbolMapper)
    }

    private fun Flow.unwrapVariableIfStable(variable: RealVariable): RealVariable? {
        val unwrapped = unwrapVariable(variable)
        return if (unwrapped != variable && variableAssignmentAnalyzer.isUnstableInCurrentScope(variable.symbol.cfir, null, session)) {
            null
        } else unwrapped
    }

    /** 仓颉的参数和模式名称均由真实 Variable 声明承载，局部原型不制造 receiver。 */
    private fun MutableFlow.getLocal(symbol: CfirBasedSymbol<*>, create: Boolean): RealVariable? {
        val effective = symbolMapper(symbol)
        val declaration = effective.cfir as? CfirCallableDeclaration ?: return null
        if (declaration !is CfirVariable && declaration !is CfirProperty) return null
        val prototype = RealVariable(effective, false, null, null,
            context.returnTypeCalculator.tryCalculateReturnType(declaration).coneType)
        return if (create) remember(prototype) else getVariableIfKnown(prototype)
    }

    private val CfirCallableDeclaration.isMutableValue: Boolean
        get() = when (this) {
            is CfirVariable -> isVar
            is CfirProperty -> status.isMut
            else -> error("A DFA value storage must be a variable or property")
        }

    private fun exitVariableInitialization(flow: MutableFlow, initializer: CfirExpression, declaration: CfirCallableDeclaration,
        variable: RealVariable, hasExplicitType: Boolean, isAssignment: Boolean) {
        val completed = flow.getExpressionCompletionOrNull(initializer)
        if (completed == null) {
            graphBuilder.currentGraph.requireSemanticTransfer(initializer)
            return
        }
        val type = checkNotNull(initializer.coneTypeOrNull)
        val use = ValueUse(completed.value, type)
        // K的alias选择属于analyzer；先捕获真实RHS元数据，store失效后不回读旧值。
        val initializerVariable = flow.rememberVariableIfUsedOrReal(initializer)
        val targetStable = variable.getStability(flow, session) == CfirSmartcastStability.STABLE_VALUE
        val sourceStable = initializerVariable is RealVariable &&
            initializerVariable.getStability(flow, session) == CfirSmartcastStability.STABLE_VALUE &&
            !variableAssignmentAnalyzer.isUnstableInCurrentScope(initializerVariable.symbol.cfir, null, session)
        if (isAssignment) {
            logicSystem.bindCompletedValue(flow, variable, use, context.dataFlowAnalyzerContext.newAssignmentIndex(), declaration)
        } else {
            logicSystem.initializeCompletedValue(flow, variable, use, declaration)
        }
        var needToAddInitializerStatement = isAssignment
        if (targetStable) {
            if (!hasExplicitType && sourceStable) {
                logicSystem.addLocalVariableAlias(flow, variable, initializerVariable as RealVariable)
            } else if (initializerVariable != null && (!declaration.isLocal || !declaration.isMutableValue)) {
                // 完成值模型已共享实际Value；原条件消费者仍明确处理其implication关系。
                logicSystem.translateVariableFromConditionInStatements(flow, completed.value, variable)
            } else if (initializer is org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression &&
                flow.unwrapVariable(variable).originalType != type) {
                needToAddInitializerStatement = true
            }
        }
        if (needToAddInitializerStatement) {
            // 实际赋值的额外批准与finish的固有上界不同；不能为所有声明无条件升级来源。
            flow.addTypeStatement(variable typeEq type)
        }
    }

    private fun enterRepeatableStatement(flow: MutableFlow, reassigned: Set<CfirBasedSymbol<*>>) {
        for (symbol in reassigned) {
            val variable = flow.getLocal(symbol, create = false) ?: continue
            logicSystem.recordNewAssignment(flow, variable, context.dataFlowAnalyzerContext.newAssignmentIndex())
        }
    }

    private fun processWhileLoopExit(path: FlowPath, flow: MutableFlow, exit: LoopExitNode,
        condition: LoopConditionEnterNode, reassigned: Set<CfirBasedSymbol<*>>) {
        if (reassigned.isEmpty()) return
        val conditionFlow = condition.getFlow(path)
        val inputs = condition.previousLiveNodes.map { it.getFlow(path) }
        val outputs = exit.previousLiveNodes.map { it.getFlow(path) }
        reassigned.forEach { symbol ->
            val variable = flow.getLocal(symbol, create = false) ?: return@forEach
            val restored = logicSystem.or(inputs.map { it.getTypeStatement(variable) ?: return@forEach })
                ?.takeIf { it.isNotEmpty } ?: return@forEach
            val statement = logicSystem.or(outputs.map {
                val atExit = it.getTypeStatement(variable)
                if (logicSystem.isSameValueIn(conditionFlow, it, variable)) logicSystem.and(atExit, restored)
                else atExit ?: return@forEach
            }) ?: return@forEach
            flow.addTypeStatement(statement)
        }
    }

    private fun processLoopExit(flow: MutableFlow, exit: LoopExitNode, condition: LoopConditionExitNode) {
        if (condition.isDead || exit.previousNodes.count { !it.isDead } > 1) return
        if (condition.fir.coneTypeOrNull?.isBoolean == true) {
            flow.getKnownVariableIfUsed(condition.fir)?.let { flow.commitOperationStatement(it eq false) }
        }
    }

    /** 两种真实 Bool 表示共用固定 Kotlin 的短路蕴含组合；不转换 AST 或忽略 RHS 写入。 */
    private fun BooleanOperatorExitNode.mergeBooleanLogicOperatorFlow() = mergeIncomingFlow { path, flow ->
        val saturating = fir.booleanOperatorKind != org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind.AND
        val leftFlow = leftOperandNode.getFlow(path)
        val rightFlow = rightOperandNode.getFlow(path)
        val left = leftFlow.takeIf { fir.booleanOperatorLeft.coneTypeOrNull?.isBoolean == true }
            ?.getKnownVariableIfUsed(fir.booleanOperatorLeft)
        val right = rightFlow.takeIf { fir.booleanOperatorRight.coneTypeOrNull?.isBoolean == true }
            ?.getKnownVariableIfUsed(fir.booleanOperatorRight)
        fun literal(expression: CfirExpression): Boolean? =
            (expression as? CfirLiteralExpression)?.takeIf { it.kind == org.cangnova.cangjie.cfir.expressions.CfirLiteralKind.BOOLEAN }?.value as? Boolean
        when {
            !isDead && rightOperandNode.isDead -> left?.let { flow.commitOperationStatement(it eq saturating) }
            literal(fir.booleanOperatorLeft) == !saturating -> right?.let {
                logicSystem.translateVariableFromConditionInStatements(flow, it, SyntheticVariable(fir))
            }
            literal(fir.booleanOperatorRight) == !saturating -> left?.let {
                logicSystem.translateVariableFromConditionInStatements(flow, it, SyntheticVariable(fir))
            }
            else -> {
                val rightStatements = flow.getTypeStatementsNotInheritedFrom(rightFlow)
                fun whenRight(value: Boolean): TypeStatements = if (right != null) {
                    logicSystem.andForTypeStatements(rightStatements, logicSystem.approveOperationStatement(rightFlow, right eq value))
                } else rightStatements
                val nonSaturating = whenRight(!saturating)
                val saturated = if (left != null) {
                    logicSystem.orForTypeStatements(logicSystem.approveOperationStatement(leftFlow, left eq saturating), whenRight(saturating))
                } else emptyMap()
                flow.addAllStatements(logicSystem.orForTypeStatements(saturated, nonSaturating))
                val variable = SyntheticVariable(fir)
                flow.addAllConditionally(variable eq saturating, saturated)
                flow.addAllConditionally(variable eq !saturating, nonSaturating)
            }
        }
    }

    /**
     * 查询匿名函数 CFG 中收集到的 return 表达式。
     */
    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction): Collection<CfirAnonymousFunctionReturnExpressionInfo> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function).orEmpty()
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
        /** CFG 中记录到的 return 结果表达式。 */
        val expression: CfirExpression,
        /** 来自真实 return jump，而非 block 的尾表达式。 */
        val isExplicit: Boolean,
        /** 实际 return 或 block owner；postponed adoption 必须写回该正式槽。 */
        val containingStatement: CfirStatement,
    )

    /**
     * 从调用实参中收集匿名函数参数。
     */
    private fun collectAnonymousFunctionArguments(arguments: List<CfirExpression>): List<CfirAnonymousFunction> {
        return arguments.mapNotNull { argument ->
            argument.unwrapAnonymousFunctionExpression()?.anonymousFunction
        }
    }
}
