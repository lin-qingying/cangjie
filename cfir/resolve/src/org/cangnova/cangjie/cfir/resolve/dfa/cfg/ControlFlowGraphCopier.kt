package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.CfirElement

/**
 * 对位 Kotlin FIR `ControlFlowGraphCopier`。
 *
 * 该复制器负责为 CFG snapshot 构建真实图映射，后续 low-level partial body resolve
 * 的 graph patching、data-flow context 回滚都依赖这一层，而不是旧的手工 frame 恢复。
 */
@CfgInternals
internal class ControlFlowGraphCopier : ControlFlowGraphVisitor<CFGNode<*>, Unit>(), ControlFlowNodeMapper {
    /** 原 CFG 到复制后 CFG 的缓存。 */
    private val cachedGraphs = HashMap<ControlFlowGraph, ControlFlowGraph>()
    /** 原 CFG 节点到复制后节点的缓存。 */
    private val cachedNodes = HashMap<CFGNode<*>, CFGNode<*>>()

    /** 尚未复制附加数据的 CFG 队列。 */
    private val unprocessedGraphs = ArrayDeque<ControlFlowGraph>()
    /** 尚未复制附加数据的节点队列。 */
    private val unprocessedNodes = ArrayDeque<CFGNode<*>>()
    /** 是否已经完成所有后处理。 */
    private var isFinished = false

    /** 完成复制后的 CFG 映射表。 */
    val graphMapping: Map<ControlFlowGraph, ControlFlowGraph>
        get() {
            check(isFinished) { "Call 'finish()' first" }
            return cachedGraphs
        }

    /** 获取或创建指定 CFG 的复制对象。 */
    override operator fun get(graph: ControlFlowGraph): ControlFlowGraph {
        return getCached(graph, cachedGraphs, unprocessedGraphs) {
            ControlFlowGraph(it.declaration, it.name, it.kind)
        }
    }

    /** 获取或创建指定 CFG 节点的复制对象。 */
    override operator fun <E : CfirElement, N : CFGNode<E>> get(node: N): N {
        return getCached(node, cachedNodes, unprocessedNodes) {
            it.accept(this, Unit)
        }
    }

    /** 统一的缓存读取与复制入队 helper。 */
    private inline fun <I, E : I> getCached(
        entity: E,
        entityCache: HashMap<I, I>,
        entityQueue: ArrayDeque<I>,
        copier: (I) -> I,
    ): E {
        val cachedEntity = entityCache[entity]
        if (cachedEntity != null) {
            @Suppress("UNCHECKED_CAST")
            return cachedEntity as E
        }

        val newEntity = copier(entity)
        require(newEntity != entity)

        entityCache[entity] = newEntity
        entityQueue.addLast(entity)

        @Suppress("UNCHECKED_CAST")
        return newEntity as E
    }

    /**
     * 完成图和节点的后处理复制。
     *
     * 节点先按 visitor 复制主体，再在这里通过 [ControlFlowNodeMapper] 修复边和附加数据引用。
     */
    fun finish() {
        if (isFinished) {
            error("The copier has already finished node processing")
        } else {
            isFinished = true
        }

        while (unprocessedGraphs.isNotEmpty() || unprocessedNodes.isNotEmpty()) {
            postProcess(cachedGraphs, unprocessedGraphs, ControlFlowGraph::copyData)
            postProcess(cachedNodes, unprocessedNodes, CFGNode<*>::copyData)
        }
    }

    /** 处理一个待复制附加数据的实体队列。 */
    private fun <I> postProcess(
        entityCache: HashMap<I, I>,
        entityQueue: ArrayDeque<I>,
        processor: (newEntity: I, oldEntity: I, mapper: ControlFlowNodeMapper) -> Unit,
    ) {
        while (entityQueue.isNotEmpty()) {
            val oldEntity = entityQueue.removeFirst()
            val newEntity = entityCache[oldEntity] ?: error("Unprocessed entity must be cached")
            processor(newEntity, oldEntity, this)
        }
    }

    /** 未显式支持的节点类型不能被复制。 */
    override fun visitNode(node: CFGNode<*>, data: Unit): CFGNode<*> {
        error("Copying is not implemented for ${node::class.simpleName}")
    }

    /** 复制函数入口节点。 */
    override fun visitFunctionEnterNode(node: FunctionEnterNode, data: Unit): CFGNode<*> =
        FunctionEnterNode(get(node.owner), node.fir, node.level)

    /** 复制函数出口节点。 */
    override fun visitFunctionExitNode(node: FunctionExitNode, data: Unit): CFGNode<*> =
        FunctionExitNode(get(node.owner), node.fir, node.level)

    /** 复制局部函数声明节点。 */
    override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Unit): CFGNode<*> =
        LocalFunctionDeclarationNode(get(node.owner), node.fir, node.level)

    /** 复制 value parameter 入口节点。 */
    override fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: Unit): CFGNode<*> =
        EnterValueParameterNode(get(node.owner), node.fir, node.level)

    /** 复制默认参数入口节点。 */
    override fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: Unit): CFGNode<*> =
        EnterDefaultArgumentsNode(get(node.owner), node.fir, node.level)

    /** 复制默认参数出口节点。 */
    override fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: Unit): CFGNode<*> =
        ExitDefaultArgumentsNode(get(node.owner), node.fir, node.level)

    /** 复制 value parameter 出口节点。 */
    override fun visitExitValueParameterNode(node: ExitValueParameterNode, data: Unit): CFGNode<*> =
        ExitValueParameterNode(get(node.owner), node.fir, node.level)

    /** 复制 postponed lambda 分裂节点。 */
    override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: Unit): CFGNode<*> =
        SplitPostponedLambdasNode(get(node.owner), node.fir, node.lambdas, node.level)

    /** 复制 postponed lambda 出口节点。 */
    override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: Unit): CFGNode<*> =
        PostponedLambdaExitNode(get(node.owner), node.fir, node.level)

    /** 复制 postponed lambda 出口合并节点。 */
    override fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: Unit): CFGNode<*> =
        MergePostponedLambdaExitsNode(get(node.owner), node.fir, node.level)

    /** 复制匿名函数捕获节点。 */
    override fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: Unit): CFGNode<*> =
        AnonymousFunctionCaptureNode(get(node.owner), node.fir, node.level)

    /** 复制匿名函数表达式节点。 */
    override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: Unit): CFGNode<*> =
        AnonymousFunctionExpressionNode(get(node.owner), node.fir, node.level)

    /** 复制文件入口节点。 */
    override fun visitFileEnterNode(node: FileEnterNode, data: Unit): CFGNode<*> =
        FileEnterNode(get(node.owner), node.fir, node.level)

    /** 复制文件出口节点。 */
    override fun visitFileExitNode(node: FileExitNode, data: Unit): CFGNode<*> =
        FileExitNode(get(node.owner), node.fir, node.level)

    /** 复制 class 入口节点。 */
    override fun visitClassEnterNode(node: ClassEnterNode, data: Unit): CFGNode<*> =
        ClassEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 class 出口节点。 */
    override fun visitClassExitNode(node: ClassExitNode, data: Unit): CFGNode<*> =
        ClassExitNode(get(node.owner), node.fir, node.level)

    /** 复制局部 class 出口节点。 */
    override fun visitLocalClassExitNode(node: LocalClassExitNode, data: Unit): CFGNode<*> =
        LocalClassExitNode(get(node.owner), node.fir, node.level)

    /** 复制代码片段入口节点。 */
    override fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: Unit): CFGNode<*> =
        CodeFragmentEnterNode(get(node.owner), node.fir, node.level)

    /** 复制代码片段出口节点。 */
    override fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: Unit): CFGNode<*> =
        CodeFragmentExitNode(get(node.owner), node.fir, node.level)

    /** 复制 block 入口节点。 */
    override fun visitBlockEnterNode(node: BlockEnterNode, data: Unit): CFGNode<*> =
        BlockEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 block 出口节点。 */
    override fun visitBlockExitNode(node: BlockExitNode, data: Unit): CFGNode<*> =
        BlockExitNode(get(node.owner), node.fir, node.level)

    /** 复制 match 入口节点。 */
    override fun visitMatchEnterNode(node: MatchEnterNode, data: Unit): CFGNode<*> =
        MatchEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 match 出口节点。 */
    override fun visitMatchExitNode(node: MatchExitNode, data: Unit): CFGNode<*> =
        MatchExitNode(get(node.owner), node.fir, node.level)

    /** 复制 match 分支条件入口节点。 */
    override fun visitMatchBranchConditionEnterNode(node: MatchBranchConditionEnterNode, data: Unit): CFGNode<*> =
        MatchBranchConditionEnterNode(get(node.owner), node.fir, node.matchExpression, node.level)

    /** 复制 match 分支条件出口节点。 */
    override fun visitMatchBranchConditionExitNode(node: MatchBranchConditionExitNode, data: Unit): CFGNode<*> =
        MatchBranchConditionExitNode(get(node.owner), node.fir, node.matchExpression, node.level)

    /** 复制 match 原子模式判定节点。 */
    override fun visitMatchPatternDecisionNode(node: MatchPatternDecisionNode, data: Unit): CFGNode<*> =
        MatchPatternDecisionNode(
            get(node.owner),
            node.branch,
            node.pattern,
            node.guard,
            node.subjectPath,
            node.reportSource,
            node.matchExpression,
            node.level,
        )

    /** 复制 match 分支失败汇合节点。 */
    override fun visitMatchBranchFailureNode(node: MatchBranchFailureNode, data: Unit): CFGNode<*> =
        MatchBranchFailureNode(get(node.owner), node.fir, node.matchExpression, node.level)

    /** 复制 match 分支结果入口节点。 */
    override fun visitMatchBranchResultEnterNode(node: MatchBranchResultEnterNode, data: Unit): CFGNode<*> =
        MatchBranchResultEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 match 分支结果出口节点。 */
    override fun visitMatchBranchResultExitNode(node: MatchBranchResultExitNode, data: Unit): CFGNode<*> =
        MatchBranchResultExitNode(get(node.owner), node.fir, node.level)

    /** 复制 match synthetic else 分支节点。 */
    override fun visitMatchSyntheticElseBranchNode(node: MatchSyntheticElseBranchNode, data: Unit): CFGNode<*> =
        MatchSyntheticElseBranchNode(get(node.owner), node.fir, node.level)

    /** 复制 if 入口节点。 */
    override fun visitIfEnterNode(node: IfEnterNode, data: Unit): CFGNode<*> =
        IfEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 if 出口节点。 */
    override fun visitIfExitNode(node: IfExitNode, data: Unit): CFGNode<*> =
        IfExitNode(get(node.owner), node.fir, node.level)

    /** 复制循环入口节点。 */
    override fun visitLoopEnterNode(node: LoopEnterNode, data: Unit): CFGNode<*> =
        LoopEnterNode(get(node.owner), node.fir, node.level)

    /** 复制循环体入口节点。 */
    override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: Unit): CFGNode<*> =
        LoopBlockEnterNode(get(node.owner), node.fir, node.level)

    /** 复制循环体出口节点。 */
    override fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: Unit): CFGNode<*> =
        LoopBlockExitNode(get(node.owner), node.fir, node.level)

    /** 复制循环条件入口节点。 */
    override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: Unit): CFGNode<*> =
        LoopConditionEnterNode(get(node.owner), node.fir, node.loop, node.level)

    /** 复制循环条件出口节点。 */
    override fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: Unit): CFGNode<*> =
        LoopConditionExitNode(get(node.owner), node.fir, node.loop, node.level)

    /** 复制循环出口节点。 */
    override fun visitLoopExitNode(node: LoopExitNode, data: Unit): CFGNode<*> =
        LoopExitNode(get(node.owner), node.fir, node.level)

    /** 复制 try 表达式入口节点。 */
    override fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: Unit): CFGNode<*> =
        TryExpressionEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 try 主体入口节点。 */
    override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: Unit): CFGNode<*> =
        TryMainBlockEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 try 主体出口节点。 */
    override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: Unit): CFGNode<*> =
        TryMainBlockExitNode(get(node.owner), node.fir, node.level)

    /** 复制 catch 子句入口节点。 */
    override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: Unit): CFGNode<*> =
        CatchClauseEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 catch 子句出口节点。 */
    override fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: Unit): CFGNode<*> =
        CatchClauseExitNode(get(node.owner), node.fir, node.level)

    /** 复制 handle 子句入口节点。 */
    override fun visitHandleClauseEnterNode(node: HandleClauseEnterNode, data: Unit): CFGNode<*> =
        HandleClauseEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 handle 子句出口节点。 */
    override fun visitHandleClauseExitNode(node: HandleClauseExitNode, data: Unit): CFGNode<*> =
        HandleClauseExitNode(get(node.owner), node.fir, node.level)

    /** 复制 finally block 入口节点。 */
    override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: Unit): CFGNode<*> =
        FinallyBlockEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 finally block 出口节点，并映射其入口节点引用。 */
    override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: Unit): CFGNode<*> =
        FinallyBlockExitNode(get(node.owner), node.fir, get(node.enterNode), node.level)

    /** 复制 try 表达式出口节点。 */
    override fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: Unit): CFGNode<*> =
        TryExpressionExitNode(get(node.owner), node.fir, node.level)

    /** 复制布尔短路操作入口节点。 */
    override fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: Unit): CFGNode<*> =
        BooleanOperatorEnterNode(get(node.owner), node.fir, node.level)

    /** 复制布尔短路操作左操作数出口节点。 */
    override fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: Unit): CFGNode<*> =
        BooleanOperatorExitLeftOperandNode(get(node.owner), node.fir, node.level)

    /** 复制布尔短路操作右操作数入口节点。 */
    override fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: Unit): CFGNode<*> =
        BooleanOperatorEnterRightOperandNode(get(node.owner), node.fir, node.level)

    /** 复制布尔短路操作出口节点，并映射左右操作数节点引用。 */
    override fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: Unit): CFGNode<*> =
        BooleanOperatorExitNode(get(node.owner), node.fir, get(node.leftOperandNode), get(node.rightOperandNode), node.level)

    /** 复制类型操作调用节点。 */
    override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: Unit): CFGNode<*> =
        TypeOperatorCallNode(get(node.owner), node.fir, node.level)

    /** 复制比较表达式节点。 */
    override fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: Unit): CFGNode<*> =
        ComparisonExpressionNode(get(node.owner), node.fir, node.level)

    /** 复制跳转表达式节点。 */
    override fun visitJumpNode(node: JumpNode, data: Unit): CFGNode<*> =
        JumpNode(get(node.owner), node.fir, node.level)

    /** 复制字面量表达式节点。 */
    override fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: Unit): CFGNode<*> =
        LiteralExpressionNode(get(node.owner), node.fir, node.level)

    /** 复制限定访问表达式节点。 */
    override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Unit): CFGNode<*> =
        QualifiedAccessNode(get(node.owner), node.fir, node.level)

    /** 复制函数调用参数入口节点。 */
    override fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: Unit): CFGNode<*> =
        FunctionCallArgumentsEnterNode(get(node.owner), node.fir, node.level)

    /** 复制函数调用参数出口节点，并映射显式接收者出口节点引用。 */
    override fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: Unit): CFGNode<*> =
        FunctionCallArgumentsExitNode(get(node.owner), node.fir, get(node.explicitReceiverExitNode), node.level)

    /** 复制函数调用入口节点。 */
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: Unit): CFGNode<*> =
        FunctionCallEnterNode(get(node.owner), node.fir, node.level)

    /** 复制函数调用出口节点。 */
    override fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: Unit): CFGNode<*> =
        FunctionCallExitNode(get(node.owner), node.fir, node.level)

    /** 复制 throw 表达式节点。 */
    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: Unit): CFGNode<*> =
        ThrowExceptionNode(get(node.owner), node.fir, node.level)

    /** 复制变量声明入口节点。 */
    override fun visitVariableDeclarationEnterNode(node: VariableDeclarationEnterNode, data: Unit): CFGNode<*> =
        VariableDeclarationEnterNode(get(node.owner), node.fir, node.level)

    /** 复制变量声明出口节点。 */
    override fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode, data: Unit): CFGNode<*> =
        VariableDeclarationExitNode(get(node.owner), node.fir, node.level)

    /** 复制变量赋值节点。 */
    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Unit): CFGNode<*> =
        VariableAssignmentNode(get(node.owner), node.fir, node.level)

    /** 复制自增/自减写入节点。 */
    override fun visitIncrementDecrementNode(node: IncrementDecrementNode, data: Unit): CFGNode<*> =
        IncrementDecrementNode(get(node.owner), node.fir, node.level)

    /** 复制可选链入口节点。 */
    override fun visitEnterOptionalChainNode(node: EnterOptionalChainNode, data: Unit): CFGNode<*> =
        EnterOptionalChainNode(get(node.owner), node.fir, node.level)

    /** 复制可选链出口节点。 */
    override fun visitExitOptionalChainNode(node: ExitOptionalChainNode, data: Unit): CFGNode<*> =
        ExitOptionalChainNode(get(node.owner), node.fir, node.level)

    /** 复制 wrapped expression 节点。 */
    override fun visitWrappedExpressionNode(node: WrappedExpressionNode, data: Unit): CFGNode<*> =
        WrappedExpressionNode(get(node.owner), node.fir, node.level)

    /** 复制字段初始化器入口节点。 */
    override fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: Unit): CFGNode<*> =
        FieldInitializerEnterNode(get(node.owner), node.fir, node.level)

    /** 复制字段初始化器出口节点。 */
    override fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: Unit): CFGNode<*> =
        FieldInitializerExitNode(get(node.owner), node.fir, node.level)

    /** 复制 spawn 表达式节点。 */
    override fun visitSpawnExpressionNode(node: SpawnExpressionNode, data: Unit): CFGNode<*> =
        SpawnExpressionNode(get(node.owner), node.fir, node.level)

    /** 复制 synchronized 入口节点。 */
    override fun visitSynchronizedEnterNode(node: SynchronizedEnterNode, data: Unit): CFGNode<*> =
        SynchronizedEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 synchronized 出口节点。 */
    override fun visitSynchronizedExitNode(node: SynchronizedExitNode, data: Unit): CFGNode<*> =
        SynchronizedExitNode(get(node.owner), node.fir, node.level)

    /** 复制 unsafe 入口节点。 */
    override fun visitUnsafeEnterNode(node: UnsafeEnterNode, data: Unit): CFGNode<*> =
        UnsafeEnterNode(get(node.owner), node.fir, node.level)

    /** 复制 unsafe 出口节点。 */
    override fun visitUnsafeExitNode(node: UnsafeExitNode, data: Unit): CFGNode<*> =
        UnsafeExitNode(get(node.owner), node.fir, node.level)

    /** 复制占位节点。 */
    override fun visitStubNode(node: StubNode, data: Unit): CFGNode<*> =
        StubNode(get(node.owner), node.level)

    /** 复制 fake expression enter 节点。 */
    override fun visitFakeExpressionEnterNode(node: FakeExpressionEnterNode, data: Unit): CFGNode<*> =
        FakeExpressionEnterNode(get(node.owner), node.level)
}
