package org.cangnova.cangjie.cfir.resolve.dfa.cfg

/**
 * 对位 Kotlin FIR `ControlFlowGraphVisitor`。
 *
 * 节点分派维持独立 visitor 层，后续 copier / renderer / data-flow merge 都按这一层工作。
 */
abstract class ControlFlowGraphVisitor<out R, in D> {
    /** 访问通用 CFG 节点。 */
    abstract fun visitNode(node: CFGNode<*>, data: D): R

    /** 访问函数入口节点。 */
    open fun visitFunctionEnterNode(node: FunctionEnterNode, data: D): R = visitNode(node, data)
    /** 访问函数出口节点。 */
    open fun visitFunctionExitNode(node: FunctionExitNode, data: D): R = visitNode(node, data)
    /** 访问局部函数声明节点。 */
    open fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: D): R = visitNode(node, data)

    /** 访问值参数入口节点。 */
    open fun visitEnterValueParameterNode(node: EnterValueParameterNode, data: D): R = visitNode(node, data)
    /** 访问默认参数入口节点。 */
    open fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: D): R = visitNode(node, data)
    /** 访问默认参数出口节点。 */
    open fun visitExitDefaultArgumentsNode(node: ExitDefaultArgumentsNode, data: D): R = visitNode(node, data)
    /** 访问值参数出口节点。 */
    open fun visitExitValueParameterNode(node: ExitValueParameterNode, data: D): R = visitNode(node, data)

    /** 访问延期 lambda 拆分节点。 */
    open fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode, data: D): R = visitNode(node, data)
    /** 访问延期 lambda 出口节点。 */
    open fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode, data: D): R = visitNode(node, data)
    /** 访问延期 lambda 出口合并节点。 */
    open fun visitMergePostponedLambdaExitsNode(node: MergePostponedLambdaExitsNode, data: D): R = visitNode(node, data)
    /** 访问匿名函数捕获节点。 */
    open fun visitAnonymousFunctionCaptureNode(node: AnonymousFunctionCaptureNode, data: D): R = visitNode(node, data)
    /** 访问匿名函数表达式节点。 */
    open fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode, data: D): R = visitNode(node, data)

    /** 访问文件入口节点。 */
    open fun visitFileEnterNode(node: FileEnterNode, data: D): R = visitNode(node, data)
    /** 访问文件出口节点。 */
    open fun visitFileExitNode(node: FileExitNode, data: D): R = visitNode(node, data)

    /** 访问类入口节点。 */
    open fun visitClassEnterNode(node: ClassEnterNode, data: D): R = visitNode(node, data)
    /** 访问类出口节点。 */
    open fun visitClassExitNode(node: ClassExitNode, data: D): R = visitNode(node, data)
    /** 访问局部类出口节点。 */
    open fun visitLocalClassExitNode(node: LocalClassExitNode, data: D): R = visitNode(node, data)

    /** 访问代码片段入口节点。 */
    open fun visitCodeFragmentEnterNode(node: CodeFragmentEnterNode, data: D): R = visitNode(node, data)
    /** 访问代码片段出口节点。 */
    open fun visitCodeFragmentExitNode(node: CodeFragmentExitNode, data: D): R = visitNode(node, data)

    /** 访问字段初始化入口节点。 */
    open fun visitFieldInitializerEnterNode(node: FieldInitializerEnterNode, data: D): R = visitNode(node, data)
    /** 访问字段初始化出口节点。 */
    open fun visitFieldInitializerExitNode(node: FieldInitializerExitNode, data: D): R = visitNode(node, data)

    /** 访问 spawn 表达式节点。 */
    open fun visitSpawnExpressionNode(node: SpawnExpressionNode, data: D): R = visitNode(node, data)
    /** 访问 synchronized 入口节点。 */
    open fun visitSynchronizedEnterNode(node: SynchronizedEnterNode, data: D): R = visitNode(node, data)
    /** 访问 synchronized 出口节点。 */
    open fun visitSynchronizedExitNode(node: SynchronizedExitNode, data: D): R = visitNode(node, data)
    /** 访问 unsafe 入口节点。 */
    open fun visitUnsafeEnterNode(node: UnsafeEnterNode, data: D): R = visitNode(node, data)
    /** 访问 unsafe 出口节点。 */
    open fun visitUnsafeExitNode(node: UnsafeExitNode, data: D): R = visitNode(node, data)

    /** 访问块入口节点。 */
    open fun visitBlockEnterNode(node: BlockEnterNode, data: D): R = visitNode(node, data)
    /** 访问块出口节点。 */
    open fun visitBlockExitNode(node: BlockExitNode, data: D): R = visitNode(node, data)

    /** 访问 match 入口节点。 */
    open fun visitMatchEnterNode(node: MatchEnterNode, data: D): R = visitNode(node, data)
    /** 访问 match 出口节点。 */
    open fun visitMatchExitNode(node: MatchExitNode, data: D): R = visitNode(node, data)
    /** 访问 match 分支条件入口节点。 */
    open fun visitMatchBranchConditionEnterNode(node: MatchBranchConditionEnterNode, data: D): R = visitNode(node, data)
    /** 访问 match 分支条件出口节点。 */
    open fun visitMatchBranchConditionExitNode(node: MatchBranchConditionExitNode, data: D): R = visitNode(node, data)
    /** 访问 match 分支结果入口节点。 */
    open fun visitMatchBranchResultEnterNode(node: MatchBranchResultEnterNode, data: D): R = visitNode(node, data)
    /** 访问 match 分支结果出口节点。 */
    open fun visitMatchBranchResultExitNode(node: MatchBranchResultExitNode, data: D): R = visitNode(node, data)
    /** 访问 match 合成 else 分支节点。 */
    open fun visitMatchSyntheticElseBranchNode(node: MatchSyntheticElseBranchNode, data: D): R = visitNode(node, data)

    /** 访问 if 入口节点。 */
    open fun visitIfEnterNode(node: IfEnterNode, data: D): R = visitNode(node, data)
    /** 访问 if 出口节点。 */
    open fun visitIfExitNode(node: IfExitNode, data: D): R = visitNode(node, data)

    /** 访问循环入口节点。 */
    open fun visitLoopEnterNode(node: LoopEnterNode, data: D): R = visitNode(node, data)
    /** 访问循环体入口节点。 */
    open fun visitLoopBlockEnterNode(node: LoopBlockEnterNode, data: D): R = visitNode(node, data)
    /** 访问循环体出口节点。 */
    open fun visitLoopBlockExitNode(node: LoopBlockExitNode, data: D): R = visitNode(node, data)
    /** 访问循环条件入口节点。 */
    open fun visitLoopConditionEnterNode(node: LoopConditionEnterNode, data: D): R = visitNode(node, data)
    /** 访问循环条件出口节点。 */
    open fun visitLoopConditionExitNode(node: LoopConditionExitNode, data: D): R = visitNode(node, data)
    /** 访问循环出口节点。 */
    open fun visitLoopExitNode(node: LoopExitNode, data: D): R = visitNode(node, data)

    /** 访问 try 表达式入口节点。 */
    open fun visitTryExpressionEnterNode(node: TryExpressionEnterNode, data: D): R = visitNode(node, data)
    /** 访问 try 主体入口节点。 */
    open fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: D): R = visitNode(node, data)
    /** 访问 try 主体出口节点。 */
    open fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: D): R = visitNode(node, data)
    /** 访问 catch 子句入口节点。 */
    open fun visitCatchClauseEnterNode(node: CatchClauseEnterNode, data: D): R = visitNode(node, data)
    /** 访问 catch 子句出口节点。 */
    open fun visitCatchClauseExitNode(node: CatchClauseExitNode, data: D): R = visitNode(node, data)
    /** 访问 handle 子句入口节点。 */
    open fun visitHandleClauseEnterNode(node: HandleClauseEnterNode, data: D): R = visitNode(node, data)
    /** 访问 handle 子句出口节点。 */
    open fun visitHandleClauseExitNode(node: HandleClauseExitNode, data: D): R = visitNode(node, data)
    /** 访问 finally 块入口节点。 */
    open fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode, data: D): R = visitNode(node, data)
    /** 访问 finally 块出口节点。 */
    open fun visitFinallyBlockExitNode(node: FinallyBlockExitNode, data: D): R = visitNode(node, data)
    /** 访问 try 表达式出口节点。 */
    open fun visitTryExpressionExitNode(node: TryExpressionExitNode, data: D): R = visitNode(node, data)

    /** 访问布尔运算入口节点。 */
    open fun visitBooleanOperatorEnterNode(node: BooleanOperatorEnterNode, data: D): R = visitNode(node, data)
    /** 访问布尔运算左操作数出口节点。 */
    open fun visitBooleanOperatorExitLeftOperandNode(node: BooleanOperatorExitLeftOperandNode, data: D): R = visitNode(node, data)
    /** 访问布尔运算右操作数入口节点。 */
    open fun visitBooleanOperatorEnterRightOperandNode(node: BooleanOperatorEnterRightOperandNode, data: D): R = visitNode(node, data)
    /** 访问布尔运算出口节点。 */
    open fun visitBooleanOperatorExitNode(node: BooleanOperatorExitNode, data: D): R = visitNode(node, data)

    /** 访问类型操作节点。 */
    open fun visitTypeOperatorCallNode(node: TypeOperatorCallNode, data: D): R = visitNode(node, data)
    /** 访问比较表达式节点。 */
    open fun visitComparisonExpressionNode(node: ComparisonExpressionNode, data: D): R = visitNode(node, data)

    /** 访问跳转节点。 */
    open fun visitJumpNode(node: JumpNode, data: D): R = visitNode(node, data)
    /** 访问字面量节点。 */
    open fun visitLiteralExpressionNode(node: LiteralExpressionNode, data: D): R = visitNode(node, data)
    /** 访问限定访问节点。 */
    open fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: D): R = visitNode(node, data)

    /** 访问函数调用参数入口节点。 */
    open fun visitFunctionCallArgumentsEnterNode(node: FunctionCallArgumentsEnterNode, data: D): R = visitNode(node, data)
    /** 访问函数调用参数出口节点。 */
    open fun visitFunctionCallArgumentsExitNode(node: FunctionCallArgumentsExitNode, data: D): R = visitNode(node, data)
    /** 访问函数调用入口节点。 */
    open fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: D): R = visitNode(node, data)
    /** 访问函数调用出口节点。 */
    open fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: D): R = visitNode(node, data)

    /** 访问 throw 节点。 */
    open fun visitThrowExceptionNode(node: ThrowExceptionNode, data: D): R = visitNode(node, data)
    /** 访问变量声明入口节点。 */
    open fun visitVariableDeclarationEnterNode(node: VariableDeclarationEnterNode, data: D): R = visitNode(node, data)
    /** 访问变量声明出口节点。 */
    open fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode, data: D): R = visitNode(node, data)
    /** 访问变量赋值节点。 */
    open fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: D): R = visitNode(node, data)

    /** 访问 optional chain 入口节点。 */
    open fun visitEnterOptionalChainNode(node: EnterOptionalChainNode, data: D): R = visitNode(node, data)
    /** 访问 optional chain 出口节点。 */
    open fun visitExitOptionalChainNode(node: ExitOptionalChainNode, data: D): R = visitNode(node, data)
    /** 访问包装表达式节点。 */
    open fun visitWrappedExpressionNode(node: WrappedExpressionNode, data: D): R = visitNode(node, data)

    /** 访问 stub 节点。 */
    open fun visitStubNode(node: StubNode, data: D): R = visitNode(node, data)

    /** 访问假表达式节点；该节点不应出现在完成后的图中。 */
    open fun visitFakeExpressionEnterNode(node: FakeExpressionEnterNode, data: D): R {
        throw IllegalStateException("fake expressions should not appear in graphs")
    }
}
