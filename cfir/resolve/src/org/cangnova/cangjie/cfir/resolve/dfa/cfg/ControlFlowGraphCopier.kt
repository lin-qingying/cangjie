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
    private val cachedGraphs = HashMap<ControlFlowGraph, ControlFlowGraph>()
    private val cachedNodes = HashMap<CFGNode<*>, CFGNode<*>>()

    private val unprocessedGraphs = ArrayDeque<ControlFlowGraph>()
    private val unprocessedNodes = ArrayDeque<CFGNode<*>>()
    private var isFinished = false

    val graphMapping: Map<ControlFlowGraph, ControlFlowGraph>
        get() {
            check(isFinished) { "Call 'finish()' first" }
            return cachedGraphs
        }

    override operator fun get(graph: ControlFlowGraph): ControlFlowGraph {
        return getCached(graph, cachedGraphs, unprocessedGraphs) {
            ControlFlowGraph(it.declaration, it.name, it.kind)
        }
    }

    override operator fun <E : CfirElement, N : CFGNode<E>> get(node: N): N {
        return getCached(node, cachedNodes, unprocessedNodes) {
            it.accept(this, Unit)
        }
    }

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

    override fun visitNode(node: CFGNode<*>, data: Unit): CFGNode<*> {
        error("Copying is not implemented for ${node::class.simpleName}")
    }

    override fun visitFunctionEnterNode(node: FunctionEnterNode, data: Unit): CFGNode<*> =
        FunctionEnterNode(get(node.owner), node.fir, node.level)

    override fun visitFunctionExitNode(node: FunctionExitNode, data: Unit): CFGNode<*> =
        FunctionExitNode(get(node.owner), node.fir, node.level)

    override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Unit): CFGNode<*> =
        LocalFunctionDeclarationNode(get(node.owner), node.fir, node.level)

    override fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: Unit): CFGNode<*> =
        EnterValueParameterNode(get(node.owner), node.fir, node.level)

    override fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: Unit): CFGNode<*> =
        EnterDefaultArgumentsNode(get(node.owner), node.fir, node.level)

    override fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: Unit): CFGNode<*> =
        ExitDefaultArgumentsNode(get(node.owner), node.fir, node.level)

    override fun visitExitValueParameterNode(node: ExitValueParameterNode, data: Unit): CFGNode<*> =
        ExitValueParameterNode(get(node.owner), node.fir, node.level)

    override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: Unit): CFGNode<*> =
        SplitPostponedLambdasNode(get(node.owner), node.fir, node.lambdas, node.level)

    override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: Unit): CFGNode<*> =
        PostponedLambdaExitNode(get(node.owner), node.fir, node.level)

    override fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: Unit): CFGNode<*> =
        MergePostponedLambdaExitsNode(get(node.owner), node.fir, node.level)

    override fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: Unit): CFGNode<*> =
        AnonymousFunctionCaptureNode(get(node.owner), node.fir, node.level)

    override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: Unit): CFGNode<*> =
        AnonymousFunctionExpressionNode(get(node.owner), node.fir, node.level)

    override fun visitFileEnterNode(node: FileEnterNode, data: Unit): CFGNode<*> =
        FileEnterNode(get(node.owner), node.fir, node.level)

    override fun visitFileExitNode(node: FileExitNode, data: Unit): CFGNode<*> =
        FileExitNode(get(node.owner), node.fir, node.level)

    override fun visitClassEnterNode(node: ClassEnterNode, data: Unit): CFGNode<*> =
        ClassEnterNode(get(node.owner), node.fir, node.level)

    override fun visitClassExitNode(node: ClassExitNode, data: Unit): CFGNode<*> =
        ClassExitNode(get(node.owner), node.fir, node.level)

    override fun visitLocalClassExitNode(node: LocalClassExitNode, data: Unit): CFGNode<*> =
        LocalClassExitNode(get(node.owner), node.fir, node.level)

    override fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: Unit): CFGNode<*> =
        CodeFragmentEnterNode(get(node.owner), node.fir, node.level)

    override fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: Unit): CFGNode<*> =
        CodeFragmentExitNode(get(node.owner), node.fir, node.level)

    override fun visitBlockEnterNode(node: BlockEnterNode, data: Unit): CFGNode<*> =
        BlockEnterNode(get(node.owner), node.fir, node.level)

    override fun visitBlockExitNode(node: BlockExitNode, data: Unit): CFGNode<*> =
        BlockExitNode(get(node.owner), node.fir, node.level)

    override fun visitMatchEnterNode(node: MatchEnterNode, data: Unit): CFGNode<*> =
        MatchEnterNode(get(node.owner), node.fir, node.level)

    override fun visitMatchExitNode(node: MatchExitNode, data: Unit): CFGNode<*> =
        MatchExitNode(get(node.owner), node.fir, node.level)

    override fun visitMatchBranchConditionEnterNode(node: MatchBranchConditionEnterNode, data: Unit): CFGNode<*> =
        MatchBranchConditionEnterNode(get(node.owner), node.fir, node.level)

    override fun visitMatchBranchConditionExitNode(node: MatchBranchConditionExitNode, data: Unit): CFGNode<*> =
        MatchBranchConditionExitNode(get(node.owner), node.fir, node.level)

    override fun visitMatchBranchResultEnterNode(node: MatchBranchResultEnterNode, data: Unit): CFGNode<*> =
        MatchBranchResultEnterNode(get(node.owner), node.fir, node.level)

    override fun visitMatchBranchResultExitNode(node: MatchBranchResultExitNode, data: Unit): CFGNode<*> =
        MatchBranchResultExitNode(get(node.owner), node.fir, node.level)

    override fun visitMatchSyntheticElseBranchNode(node: MatchSyntheticElseBranchNode, data: Unit): CFGNode<*> =
        MatchSyntheticElseBranchNode(get(node.owner), node.fir, node.level)

    override fun visitIfEnterNode(node: IfEnterNode, data: Unit): CFGNode<*> =
        IfEnterNode(get(node.owner), node.fir, node.level)

    override fun visitIfExitNode(node: IfExitNode, data: Unit): CFGNode<*> =
        IfExitNode(get(node.owner), node.fir, node.level)

    override fun visitLoopEnterNode(node: LoopEnterNode, data: Unit): CFGNode<*> =
        LoopEnterNode(get(node.owner), node.fir, node.level)

    override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: Unit): CFGNode<*> =
        LoopBlockEnterNode(get(node.owner), node.fir, node.level)

    override fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: Unit): CFGNode<*> =
        LoopBlockExitNode(get(node.owner), node.fir, node.level)

    override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: Unit): CFGNode<*> =
        LoopConditionEnterNode(get(node.owner), node.fir, node.loop, node.level)

    override fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: Unit): CFGNode<*> =
        LoopConditionExitNode(get(node.owner), node.fir, node.loop, node.level)

    override fun visitLoopExitNode(node: LoopExitNode, data: Unit): CFGNode<*> =
        LoopExitNode(get(node.owner), node.fir, node.level)

    override fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: Unit): CFGNode<*> =
        TryExpressionEnterNode(get(node.owner), node.fir, node.level)

    override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: Unit): CFGNode<*> =
        TryMainBlockEnterNode(get(node.owner), node.fir, node.level)

    override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: Unit): CFGNode<*> =
        TryMainBlockExitNode(get(node.owner), node.fir, node.level)

    override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: Unit): CFGNode<*> =
        CatchClauseEnterNode(get(node.owner), node.fir, node.level)

    override fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: Unit): CFGNode<*> =
        CatchClauseExitNode(get(node.owner), node.fir, node.level)

    override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: Unit): CFGNode<*> =
        FinallyBlockEnterNode(get(node.owner), node.fir, node.level)

    override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: Unit): CFGNode<*> =
        FinallyBlockExitNode(get(node.owner), node.fir, get(node.enterNode), node.level)

    override fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: Unit): CFGNode<*> =
        TryExpressionExitNode(get(node.owner), node.fir, node.level)

    override fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: Unit): CFGNode<*> =
        BooleanOperatorEnterNode(get(node.owner), node.fir, node.level)

    override fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: Unit): CFGNode<*> =
        BooleanOperatorExitLeftOperandNode(get(node.owner), node.fir, node.level)

    override fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: Unit): CFGNode<*> =
        BooleanOperatorEnterRightOperandNode(get(node.owner), node.fir, node.level)

    override fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: Unit): CFGNode<*> =
        BooleanOperatorExitNode(get(node.owner), node.fir, get(node.leftOperandNode), get(node.rightOperandNode), node.level)

    override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: Unit): CFGNode<*> =
        TypeOperatorCallNode(get(node.owner), node.fir, node.level)

    override fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: Unit): CFGNode<*> =
        ComparisonExpressionNode(get(node.owner), node.fir, node.level)

    override fun visitJumpNode(node: JumpNode, data: Unit): CFGNode<*> =
        JumpNode(get(node.owner), node.fir, node.level)

    override fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: Unit): CFGNode<*> =
        LiteralExpressionNode(get(node.owner), node.fir, node.level)

    override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Unit): CFGNode<*> =
        QualifiedAccessNode(get(node.owner), node.fir, node.level)

    override fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: Unit): CFGNode<*> =
        FunctionCallArgumentsEnterNode(get(node.owner), node.fir, node.level)

    override fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: Unit): CFGNode<*> =
        FunctionCallArgumentsExitNode(get(node.owner), node.fir, get(node.explicitReceiverExitNode), node.level)

    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: Unit): CFGNode<*> =
        FunctionCallEnterNode(get(node.owner), node.fir, node.level)

    override fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: Unit): CFGNode<*> =
        FunctionCallExitNode(get(node.owner), node.fir, node.level)

    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: Unit): CFGNode<*> =
        ThrowExceptionNode(get(node.owner), node.fir, node.level)

    override fun visitVariableDeclarationEnterNode(node: VariableDeclarationEnterNode, data: Unit): CFGNode<*> =
        VariableDeclarationEnterNode(get(node.owner), node.fir, node.level)

    override fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode, data: Unit): CFGNode<*> =
        VariableDeclarationExitNode(get(node.owner), node.fir, node.level)

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Unit): CFGNode<*> =
        VariableAssignmentNode(get(node.owner), node.fir, node.level)

    override fun visitEnterOptionalChainNode(node: EnterOptionalChainNode, data: Unit): CFGNode<*> =
        EnterOptionalChainNode(get(node.owner), node.fir, node.level)

    override fun visitExitOptionalChainNode(node: ExitOptionalChainNode, data: Unit): CFGNode<*> =
        ExitOptionalChainNode(get(node.owner), node.fir, node.level)

    override fun visitWrappedExpressionNode(node: WrappedExpressionNode, data: Unit): CFGNode<*> =
        WrappedExpressionNode(get(node.owner), node.fir, node.level)

    override fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: Unit): CFGNode<*> =
        FieldInitializerEnterNode(get(node.owner), node.fir, node.level)

    override fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: Unit): CFGNode<*> =
        FieldInitializerExitNode(get(node.owner), node.fir, node.level)

    override fun visitSpawnExpressionNode(node: SpawnExpressionNode, data: Unit): CFGNode<*> =
        SpawnExpressionNode(get(node.owner), node.fir, node.level)

    override fun visitSynchronizedEnterNode(node: SynchronizedEnterNode, data: Unit): CFGNode<*> =
        SynchronizedEnterNode(get(node.owner), node.fir, node.level)

    override fun visitSynchronizedExitNode(node: SynchronizedExitNode, data: Unit): CFGNode<*> =
        SynchronizedExitNode(get(node.owner), node.fir, node.level)

    override fun visitUnsafeEnterNode(node: UnsafeEnterNode, data: Unit): CFGNode<*> =
        UnsafeEnterNode(get(node.owner), node.fir, node.level)

    override fun visitUnsafeExitNode(node: UnsafeExitNode, data: Unit): CFGNode<*> =
        UnsafeExitNode(get(node.owner), node.fir, node.level)

    override fun visitStubNode(node: StubNode, data: Unit): CFGNode<*> =
        StubNode(get(node.owner), node.level)

    override fun visitFakeExpressionEnterNode(node: FakeExpressionEnterNode, data: Unit): CFGNode<*> =
        FakeExpressionEnterNode(get(node.owner), node.level)
}
