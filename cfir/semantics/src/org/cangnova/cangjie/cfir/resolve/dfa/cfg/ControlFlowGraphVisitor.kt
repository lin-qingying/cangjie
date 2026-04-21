package org.cangnova.cangjie.cfir.resolve.dfa.cfg

/**
 * 对位 Kotlin FIR `ControlFlowGraphVisitor`。
 *
 * 节点分派维持独立 visitor 层，后续 copier / renderer / data-flow merge 都按这一层工作。
 */
abstract class ControlFlowGraphVisitor<out R, in D> {
    abstract fun visitNode(node: CFGNode<*>, data: D): R

    open fun visitFunctionEnterNode(node: FunctionEnterNode, data: D): R = visitNode(node, data)
    open fun visitFunctionExitNode(node: FunctionExitNode, data: D): R = visitNode(node, data)
    open fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: D): R = visitNode(node, data)

    open fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: D): R = visitNode(node, data)
    open fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: D): R = visitNode(node, data)
    open fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: D): R = visitNode(node, data)
    open fun visitExitValueParameterNode(node: ExitValueParameterNode, data: D): R = visitNode(node, data)

    open fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: D): R = visitNode(node, data)
    open fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: D): R = visitNode(node, data)
    open fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: D): R = visitNode(node, data)
    open fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: D): R = visitNode(node, data)
    open fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: D): R = visitNode(node, data)

    open fun visitFileEnterNode(node: FileEnterNode, data: D): R = visitNode(node, data)
    open fun visitFileExitNode(node: FileExitNode, data: D): R = visitNode(node, data)

    open fun visitClassEnterNode(node: ClassEnterNode, data: D): R = visitNode(node, data)
    open fun visitClassExitNode(node: ClassExitNode, data: D): R = visitNode(node, data)
    open fun visitLocalClassExitNode(node: LocalClassExitNode, data: D): R = visitNode(node, data)

    open fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: D): R = visitNode(node, data)
    open fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: D): R = visitNode(node, data)

    open fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: D): R = visitNode(node, data)
    open fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: D): R = visitNode(node, data)

    open fun visitSpawnExpressionNode(node: SpawnExpressionNode, data: D): R = visitNode(node, data)
    open fun visitSynchronizedEnterNode(node: SynchronizedEnterNode, data: D): R = visitNode(node, data)
    open fun visitSynchronizedExitNode(node: SynchronizedExitNode, data: D): R = visitNode(node, data)
    open fun visitUnsafeEnterNode(node: UnsafeEnterNode, data: D): R = visitNode(node, data)
    open fun visitUnsafeExitNode(node: UnsafeExitNode, data: D): R = visitNode(node, data)

    open fun visitBlockEnterNode(node: BlockEnterNode, data: D): R = visitNode(node, data)
    open fun visitBlockExitNode(node: BlockExitNode, data: D): R = visitNode(node, data)

    open fun visitMatchEnterNode(node: MatchEnterNode, data: D): R = visitNode(node, data)
    open fun visitMatchExitNode(node: MatchExitNode, data: D): R = visitNode(node, data)
    open fun visitMatchBranchConditionEnterNode(node: MatchBranchConditionEnterNode, data: D): R = visitNode(node, data)
    open fun visitMatchBranchConditionExitNode(node: MatchBranchConditionExitNode, data: D): R = visitNode(node, data)
    open fun visitMatchBranchResultEnterNode(node: MatchBranchResultEnterNode, data: D): R = visitNode(node, data)
    open fun visitMatchBranchResultExitNode(node: MatchBranchResultExitNode, data: D): R = visitNode(node, data)
    open fun visitMatchSyntheticElseBranchNode(node: MatchSyntheticElseBranchNode, data: D): R = visitNode(node, data)

    open fun visitIfEnterNode(node: IfEnterNode, data: D): R = visitNode(node, data)
    open fun visitIfExitNode(node: IfExitNode, data: D): R = visitNode(node, data)

    open fun visitLoopEnterNode(node: LoopEnterNode, data: D): R = visitNode(node, data)
    open fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: D): R = visitNode(node, data)
    open fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: D): R = visitNode(node, data)
    open fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: D): R = visitNode(node, data)
    open fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: D): R = visitNode(node, data)
    open fun visitLoopExitNode(node: LoopExitNode, data: D): R = visitNode(node, data)

    open fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: D): R = visitNode(node, data)
    open fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: D): R = visitNode(node, data)
    open fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: D): R = visitNode(node, data)
    open fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: D): R = visitNode(node, data)
    open fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: D): R = visitNode(node, data)
    open fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: D): R = visitNode(node, data)
    open fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: D): R = visitNode(node, data)
    open fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: D): R = visitNode(node, data)

    open fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: D): R = visitNode(node, data)
    open fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: D): R = visitNode(node, data)
    open fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: D): R = visitNode(node, data)
    open fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: D): R = visitNode(node, data)

    open fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: D): R = visitNode(node, data)
    open fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: D): R = visitNode(node, data)

    open fun visitJumpNode(node: JumpNode, data: D): R = visitNode(node, data)
    open fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: D): R = visitNode(node, data)
    open fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: D): R = visitNode(node, data)

    open fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: D): R = visitNode(node, data)
    open fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: D): R = visitNode(node, data)
    open fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: D): R = visitNode(node, data)
    open fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: D): R = visitNode(node, data)

    open fun visitThrowExceptionNode(node: ThrowExceptionNode, data: D): R = visitNode(node, data)
    open fun visitVariableDeclarationEnterNode(node: VariableDeclarationEnterNode, data: D): R = visitNode(node, data)
    open fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode, data: D): R = visitNode(node, data)
    open fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: D): R = visitNode(node, data)

    open fun visitEnterOptionalChainNode(node: EnterOptionalChainNode, data: D): R = visitNode(node, data)
    open fun visitExitOptionalChainNode(node: ExitOptionalChainNode, data: D): R = visitNode(node, data)
    open fun visitWrappedExpressionNode(node: WrappedExpressionNode, data: D): R = visitNode(node, data)

    open fun visitStubNode(node: StubNode, data: D): R = visitNode(node, data)

    open fun visitFakeExpressionEnterNode(node: FakeExpressionEnterNode, data: D): R {
        throw IllegalStateException("fake expressions should not appear in graphs")
    }
}
