package org.cangnova.cangjie.cfir.resolve.dfa.cfg

/**
 * 无返回值、无额外数据参数的 CFG visitor。
 *
 * 一参数 `visit*` 方法供子类覆盖；文件后半段的 final override 负责把双参数 visitor
 * 分派桥接到一参数方法。
 */
abstract class ControlFlowGraphVisitorVoid : ControlFlowGraphVisitor<Unit, Nothing?>() {
    /** 访问通用 CFG 节点。 */
    abstract fun visitNode(node: CFGNode<*>)

    /** 访问函数入口节点。 */
    open fun visitFunctionEnterNode(node: FunctionEnterNode) = visitNode(node)
    /** 访问函数出口节点。 */
    open fun visitFunctionExitNode(node: FunctionExitNode) = visitNode(node)
    /** 访问局部函数声明节点。 */
    open fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode) = visitNode(node)

    /** 访问值参数入口节点。 */
    open fun visitEnterValueParameterNode(node: EnterValueParameterNode) = visitNode(node)
    /** 访问默认参数入口节点。 */
    open fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode) = visitNode(node)
    /** 访问默认参数出口节点。 */
    open fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode) = visitNode(node)
    /** 访问值参数出口节点。 */
    open fun visitExitValueParameterNode(node: ExitValueParameterNode) = visitNode(node)

    /** 访问延期 lambda 拆分节点。 */
    open fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode) = visitNode(node)
    /** 访问延期 lambda 出口节点。 */
    open fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode) = visitNode(node)
    /** 访问延期 lambda 出口合并节点。 */
    open fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode) = visitNode(node)
    /** 访问匿名函数捕获节点。 */
    open fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode) = visitNode(node)
    /** 访问匿名函数表达式节点。 */
    open fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode) = visitNode(node)

    /** 访问文件入口节点。 */
    open fun visitFileEnterNode(node: FileEnterNode) = visitNode(node)
    /** 访问文件出口节点。 */
    open fun visitFileExitNode(node: FileExitNode) = visitNode(node)
    /** 访问类入口节点。 */
    open fun visitClassEnterNode(node: ClassEnterNode) = visitNode(node)
    /** 访问类出口节点。 */
    open fun visitClassExitNode(node: ClassExitNode) = visitNode(node)
    /** 访问局部类出口节点。 */
    open fun visitLocalClassExitNode(node: LocalClassExitNode) = visitNode(node)
    /** 访问代码片段入口节点。 */
    open fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode) = visitNode(node)
    /** 访问代码片段出口节点。 */
    open fun visitCodeFragmentExitNode(node: CodeFragmentExitNode) = visitNode(node)

    /** 访问字段初始化入口节点。 */
    open fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode) = visitNode(node)
    /** 访问字段初始化出口节点。 */
    open fun visitFieldInitializerExitNode(node: FieldInitializerExitNode) = visitNode(node)

    /** 访问 spawn 表达式节点。 */
    open fun visitSpawnExpressionNode(node: SpawnExpressionNode) = visitNode(node)
    /** 访问 synchronized 入口节点。 */
    open fun visitSynchronizedEnterNode(node: SynchronizedEnterNode) = visitNode(node)
    /** 访问 synchronized 出口节点。 */
    open fun visitSynchronizedExitNode(node: SynchronizedExitNode) = visitNode(node)
    /** 访问 unsafe 入口节点。 */
    open fun visitUnsafeEnterNode(node: UnsafeEnterNode) = visitNode(node)
    /** 访问 unsafe 出口节点。 */
    open fun visitUnsafeExitNode(node: UnsafeExitNode) = visitNode(node)
    /** 访问块入口节点。 */
    open fun visitBlockEnterNode(node: BlockEnterNode) = visitNode(node)
    /** 访问块出口节点。 */
    open fun visitBlockExitNode(node: BlockExitNode) = visitNode(node)

    /** 访问 match 入口节点。 */
    open fun visitMatchEnterNode(node: MatchEnterNode) = visitNode(node)
    /** 访问 match 出口节点。 */
    open fun visitMatchExitNode(node: MatchExitNode) = visitNode(node)
    /** 访问 match 分支条件入口节点。 */
    open fun visitMatchBranchConditionEnterNode(node: MatchBranchConditionEnterNode) = visitNode(node)
    /** 访问 match 分支条件出口节点。 */
    open fun visitMatchBranchConditionExitNode(node: MatchBranchConditionExitNode) = visitNode(node)
    /** 访问 match 分支结果入口节点。 */
    open fun visitMatchBranchResultEnterNode(node: MatchBranchResultEnterNode) = visitNode(node)
    /** 访问 match 分支结果出口节点。 */
    open fun visitMatchBranchResultExitNode(node: MatchBranchResultExitNode) = visitNode(node)
    /** 访问 match 合成 else 分支节点。 */
    open fun visitMatchSyntheticElseBranchNode(node: MatchSyntheticElseBranchNode) = visitNode(node)
    /** 访问 if 入口节点。 */
    open fun visitIfEnterNode(node: IfEnterNode) = visitNode(node)
    /** 访问 if 出口节点。 */
    open fun visitIfExitNode(node: IfExitNode) = visitNode(node)

    /** 访问循环入口节点。 */
    open fun visitLoopEnterNode(node: LoopEnterNode) = visitNode(node)
    /** 访问循环体入口节点。 */
    open fun visitLoopBlockEnterNode(node: LoopBlockEnterNode) = visitNode(node)
    /** 访问循环体出口节点。 */
    open fun visitLoopBlockExitNode(node: LoopBlockExitNode) = visitNode(node)
    /** 访问循环条件入口节点。 */
    open fun visitLoopConditionEnterNode(node: LoopConditionEnterNode) = visitNode(node)
    /** 访问循环条件出口节点。 */
    open fun visitLoopConditionExitNode(node: LoopConditionExitNode) = visitNode(node)
    /** 访问循环出口节点。 */
    open fun visitLoopExitNode(node: LoopExitNode) = visitNode(node)

    /** 访问 try 表达式入口节点。 */
    open fun visitTryExpressionEnterNode(node: TryExpressionEnterNode) = visitNode(node)
    /** 访问 try 主体入口节点。 */
    open fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode) = visitNode(node)
    /** 访问 try 主体出口节点。 */
    open fun visitTryMainBlockExitNode(node: TryMainBlockExitNode) = visitNode(node)
    /** 访问 catch 子句入口节点。 */
    open fun visitCatchClauseEnterNode(node: CatchClauseEnterNode) = visitNode(node)
    /** 访问 catch 子句出口节点。 */
    open fun visitCatchClauseExitNode(node: CatchClauseExitNode) = visitNode(node)
    /** 访问 handle 子句入口节点。 */
    open fun visitHandleClauseEnterNode(node: HandleClauseEnterNode) = visitNode(node)
    /** 访问 handle 子句出口节点。 */
    open fun visitHandleClauseExitNode(node: HandleClauseExitNode) = visitNode(node)
    /** 访问 finally 块入口节点。 */
    open fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode) = visitNode(node)
    /** 访问 finally 块出口节点。 */
    open fun visitFinallyBlockExitNode(node: FinallyBlockExitNode) = visitNode(node)
    /** 访问 try 表达式出口节点。 */
    open fun visitTryExpressionExitNode(node: TryExpressionExitNode) = visitNode(node)

    /** 访问布尔运算入口节点。 */
    open fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode) = visitNode(node)
    /** 访问布尔运算左操作数出口节点。 */
    open fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode) = visitNode(node)
    /** 访问布尔运算右操作数入口节点。 */
    open fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode) = visitNode(node)
    /** 访问布尔运算出口节点。 */
    open fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode) = visitNode(node)
    /** 访问类型操作节点。 */
    open fun visitTypeOperatorCallNode(node: TypeOperatorCallNode) = visitNode(node)
    /** 访问比较表达式节点。 */
    open fun visitComparisonExpressionNode(node: ComparisonExpressionNode) = visitNode(node)
    /** 访问跳转节点。 */
    open fun visitJumpNode(node: JumpNode) = visitNode(node)
    /** 访问字面量节点。 */
    open fun visitLiteralExpressionNode(node: LiteralExpressionNode) = visitNode(node)
    /** 访问限定访问节点。 */
    open fun visitQualifiedAccessNode(node: QualifiedAccessNode) = visitNode(node)
    /** 访问函数调用参数入口节点。 */
    open fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode) = visitNode(node)
    /** 访问函数调用参数出口节点。 */
    open fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode) = visitNode(node)
    /** 访问函数调用入口节点。 */
    open fun visitFunctionCallEnterNode(node: FunctionCallEnterNode) = visitNode(node)
    /** 访问函数调用出口节点。 */
    open fun visitFunctionCallExitNode(node: FunctionCallExitNode) = visitNode(node)
    /** 访问 throw 节点。 */
    open fun visitThrowExceptionNode(node: ThrowExceptionNode) = visitNode(node)
    /** 访问变量声明入口节点。 */
    open fun visitVariableDeclarationEnterNode(node: VariableDeclarationEnterNode) = visitNode(node)
    /** 访问变量声明出口节点。 */
    open fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode) = visitNode(node)
    /** 访问变量赋值节点。 */
    open fun visitVariableAssignmentNode(node: VariableAssignmentNode) = visitNode(node)
    /** 访问 optional chain 入口节点。 */
    open fun visitEnterOptionalChainNode(node: EnterOptionalChainNode) = visitNode(node)
    /** 访问 optional chain 出口节点。 */
    open fun visitExitOptionalChainNode(node: ExitOptionalChainNode) = visitNode(node)
    /** 访问包装表达式节点。 */
    open fun visitWrappedExpressionNode(node: WrappedExpressionNode) = visitNode(node)
    /** 访问 stub 节点。 */
    open fun visitStubNode(node: StubNode) = visitNode(node)

    /** 桥接双参数通用访问入口。 */
    final override fun visitNode(node: CFGNode<*>, data: Nothing?) {
        visitNode(node)
    }

    final override fun visitFunctionEnterNode(node: FunctionEnterNode, data: Nothing?) = visitFunctionEnterNode(node)
    final override fun visitFunctionExitNode(node: FunctionExitNode, data: Nothing?) = visitFunctionExitNode(node)
    final override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Nothing?) = visitLocalFunctionDeclarationNode(node)
    final override fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: Nothing?) = visitEnterValueParameterNode(node)
    final override fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: Nothing?) = visitEnterDefaultArgumentsNode(node)
    final override fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: Nothing?) = visitExitDefaultArgumentsNode(node)
    final override fun visitExitValueParameterNode(node: ExitValueParameterNode, data: Nothing?) = visitExitValueParameterNode(node)
    final override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: Nothing?) = visitSplitPostponedLambdasNode(node)
    final override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: Nothing?) = visitPostponedLambdaExitNode(node)
    final override fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: Nothing?) = visitMergePostponedLambdaExitsNode(node)
    final override fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: Nothing?) = visitAnonymousFunctionCaptureNode(node)
    final override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: Nothing?) = visitAnonymousFunctionExpressionNode(node)
    final override fun visitFileEnterNode(node: FileEnterNode, data: Nothing?) = visitFileEnterNode(node)
    final override fun visitFileExitNode(node: FileExitNode, data: Nothing?) = visitFileExitNode(node)
    final override fun visitClassEnterNode(node: ClassEnterNode, data: Nothing?) = visitClassEnterNode(node)
    final override fun visitClassExitNode(node: ClassExitNode, data: Nothing?) = visitClassExitNode(node)
    final override fun visitLocalClassExitNode(node: LocalClassExitNode, data: Nothing?) = visitLocalClassExitNode(node)
    final override fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: Nothing?) = visitCodeFragmentEnterNode(node)
    final override fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: Nothing?) = visitCodeFragmentExitNode(node)

    final override fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: Nothing?) = visitFieldInitializerEnterNode(node)
    final override fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: Nothing?) = visitFieldInitializerExitNode(node)

    final override fun visitSpawnExpressionNode(node: SpawnExpressionNode, data: Nothing?) = visitSpawnExpressionNode(node)
    final override fun visitSynchronizedEnterNode(node: SynchronizedEnterNode, data: Nothing?) = visitSynchronizedEnterNode(node)
    final override fun visitSynchronizedExitNode(node: SynchronizedExitNode, data: Nothing?) = visitSynchronizedExitNode(node)
    final override fun visitUnsafeEnterNode(node: UnsafeEnterNode, data: Nothing?) = visitUnsafeEnterNode(node)
    final override fun visitUnsafeExitNode(node: UnsafeExitNode, data: Nothing?) = visitUnsafeExitNode(node)
    final override fun visitBlockEnterNode(node: BlockEnterNode, data: Nothing?) = visitBlockEnterNode(node)
    final override fun visitBlockExitNode(node: BlockExitNode, data: Nothing?) = visitBlockExitNode(node)
    final override fun visitMatchEnterNode(node: MatchEnterNode, data: Nothing?) = visitMatchEnterNode(node)
    final override fun visitMatchExitNode(node: MatchExitNode, data: Nothing?) = visitMatchExitNode(node)
    final override fun visitMatchBranchConditionEnterNode(node: MatchBranchConditionEnterNode, data: Nothing?) = visitMatchBranchConditionEnterNode(node)
    final override fun visitMatchBranchConditionExitNode(node: MatchBranchConditionExitNode, data: Nothing?) = visitMatchBranchConditionExitNode(node)
    final override fun visitMatchBranchResultEnterNode(node: MatchBranchResultEnterNode, data: Nothing?) = visitMatchBranchResultEnterNode(node)
    final override fun visitMatchBranchResultExitNode(node: MatchBranchResultExitNode, data: Nothing?) = visitMatchBranchResultExitNode(node)
    final override fun visitMatchSyntheticElseBranchNode(node: MatchSyntheticElseBranchNode, data: Nothing?) = visitMatchSyntheticElseBranchNode(node)
    final override fun visitIfEnterNode(node: IfEnterNode, data: Nothing?) = visitIfEnterNode(node)
    final override fun visitIfExitNode(node: IfExitNode, data: Nothing?) = visitIfExitNode(node)
    final override fun visitLoopEnterNode(node: LoopEnterNode, data: Nothing?) = visitLoopEnterNode(node)
    final override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: Nothing?) = visitLoopBlockEnterNode(node)
    final override fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: Nothing?) = visitLoopBlockExitNode(node)
    final override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: Nothing?) = visitLoopConditionEnterNode(node)
    final override fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: Nothing?) = visitLoopConditionExitNode(node)
    final override fun visitLoopExitNode(node: LoopExitNode, data: Nothing?) = visitLoopExitNode(node)
    final override fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: Nothing?) = visitTryExpressionEnterNode(node)
    final override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: Nothing?) = visitTryMainBlockEnterNode(node)
    final override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: Nothing?) = visitTryMainBlockExitNode(node)
    final override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: Nothing?) = visitCatchClauseEnterNode(node)
    final override fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: Nothing?) = visitCatchClauseExitNode(node)
    final override fun visitHandleClauseEnterNode(node: HandleClauseEnterNode, data: Nothing?) = visitHandleClauseEnterNode(node)
    final override fun visitHandleClauseExitNode(node: HandleClauseExitNode, data: Nothing?) = visitHandleClauseExitNode(node)
    final override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: Nothing?) = visitFinallyBlockEnterNode(node)
    final override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: Nothing?) = visitFinallyBlockExitNode(node)
    final override fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: Nothing?) = visitTryExpressionExitNode(node)
    final override fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: Nothing?) = visitBooleanOperatorEnterNode(node)
    final override fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: Nothing?) = visitBooleanOperatorExitLeftOperandNode(node)
    final override fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: Nothing?) = visitBooleanOperatorEnterRightOperandNode(node)
    final override fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: Nothing?) = visitBooleanOperatorExitNode(node)
    final override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: Nothing?) = visitTypeOperatorCallNode(node)
    final override fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: Nothing?) = visitComparisonExpressionNode(node)
    final override fun visitJumpNode(node: JumpNode, data: Nothing?) = visitJumpNode(node)
    final override fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: Nothing?) = visitLiteralExpressionNode(node)
    final override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Nothing?) = visitQualifiedAccessNode(node)
    final override fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: Nothing?) = visitFunctionCallArgumentsEnterNode(node)
    final override fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: Nothing?) = visitFunctionCallArgumentsExitNode(node)
    final override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: Nothing?) = visitFunctionCallEnterNode(node)
    final override fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: Nothing?) = visitFunctionCallExitNode(node)
    final override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: Nothing?) = visitThrowExceptionNode(node)
    final override fun visitVariableDeclarationEnterNode(node: VariableDeclarationEnterNode, data: Nothing?) = visitVariableDeclarationEnterNode(node)
    final override fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode, data: Nothing?) = visitVariableDeclarationExitNode(node)
    final override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Nothing?) = visitVariableAssignmentNode(node)
    final override fun visitEnterOptionalChainNode(node: EnterOptionalChainNode, data: Nothing?) = visitEnterOptionalChainNode(node)
    final override fun visitExitOptionalChainNode(node: ExitOptionalChainNode, data: Nothing?) = visitExitOptionalChainNode(node)
    final override fun visitWrappedExpressionNode(node: WrappedExpressionNode, data: Nothing?) = visitWrappedExpressionNode(node)
    final override fun visitStubNode(node: StubNode, data: Nothing?) = visitStubNode(node)
}
