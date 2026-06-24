package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression

/**
 * CFG 节点工厂扩展函数集合,对位 Kotlin FIR `ControlFlowGraphNodeBuilder.kt`。
 *
 * 所有函数都基于 [ControlFlowGraphBuilder.currentGraph] + [ControlFlowGraphBuilder.levelCounter]
 * 生成节点,不改变 builder 状态。节点生成后的边连接由 builder 在具体 enter/exit 方法里完成。
 * 覆盖范围严格对齐 CFIR 语义:
 * - 仓颉的 `match` / `if` / 循环 / try / 布尔运算 / 比较 / 跳转 / 赋值 / 可选链等;
 * - 仓颉特有的 spawn / synchronized / unsafe;
 * - 统一的包装(wrapped) / 匿名函数(capture / expression / postponed);
 * - 字段初始化器(`let/var x = expr`)、值参数及默认参数子图。
 *
 * 不包含 Kotlin 独有的 `SafeCall / Elvis / SmartCast / WhenSubject / Delegate /
 * AnonymousObject / Script / REPL / StringConcatenation / CheckNotNull`,因为仓颉
 * 语义层不存在这些概念。
 */

// ----------------------------------- Generic / placeholder -----------------------------------

/** 创建占位 CFG 节点，用于暂存尚未连接到真实语义节点的位置。 */
fun ControlFlowGraphBuilder.createStubNode(): StubNode = StubNode(currentGraph, levelCounter)

/** 创建假的表达式入口节点，用于表达式子图需要统一 enter 位置但没有真实 CFIR 元素时。 */
fun ControlFlowGraphBuilder.createFakeExpressionEnterNode(): FakeExpressionEnterNode =
    FakeExpressionEnterNode(currentGraph, levelCounter)

// ----------------------------------- Declarations / graphs -----------------------------------

/** 创建文件 CFG 入口节点。 */
fun ControlFlowGraphBuilder.createFileEnterNode(fir: CfirFile): FileEnterNode =
    FileEnterNode(currentGraph, fir, levelCounter)

/** 创建文件 CFG 出口节点。 */
fun ControlFlowGraphBuilder.createFileExitNode(fir: CfirFile): FileExitNode =
    FileExitNode(currentGraph, fir, levelCounter)

/** 创建 class 声明 CFG 入口节点。 */
fun ControlFlowGraphBuilder.createClassEnterNode(fir: CfirClass): ClassEnterNode =
    ClassEnterNode(currentGraph, fir, levelCounter)

/** 创建 class 声明 CFG 出口节点。 */
fun ControlFlowGraphBuilder.createClassExitNode(fir: CfirClass): ClassExitNode =
    ClassExitNode(currentGraph, fir, levelCounter)

/** 创建局部 class 声明出口节点。 */
fun ControlFlowGraphBuilder.createLocalClassExitNode(fir: CfirClass): LocalClassExitNode =
    LocalClassExitNode(currentGraph, fir, levelCounter)

/** 创建代码片段 CFG 入口节点。 */
fun ControlFlowGraphBuilder.createCodeFragmentEnterNode(fir: CfirCodeFragment): CodeFragmentEnterNode =
    CodeFragmentEnterNode(currentGraph, fir, levelCounter)

/** 创建代码片段 CFG 出口节点。 */
fun ControlFlowGraphBuilder.createCodeFragmentExitNode(fir: CfirCodeFragment): CodeFragmentExitNode =
    CodeFragmentExitNode(currentGraph, fir, levelCounter)

/** 创建函数 CFG 入口节点。 */
fun ControlFlowGraphBuilder.createFunctionEnterNode(fir: CfirFunction): FunctionEnterNode =
    FunctionEnterNode(currentGraph, fir, levelCounter)

/** 创建函数 CFG 出口节点。 */
fun ControlFlowGraphBuilder.createFunctionExitNode(fir: CfirFunction): FunctionExitNode =
    FunctionExitNode(currentGraph, fir, levelCounter)

/** 创建局部函数声明节点，用于表达声明本身出现在局部控制流中。 */
fun ControlFlowGraphBuilder.createLocalFunctionDeclarationNode(fir: CfirFunction): LocalFunctionDeclarationNode =
    LocalFunctionDeclarationNode(currentGraph, fir, levelCounter)

/** 创建字段初始化器入口节点。 */
fun ControlFlowGraphBuilder.createFieldInitializerEnterNode(fir: CfirFieldVariable): FieldInitializerEnterNode =
    FieldInitializerEnterNode(currentGraph, fir, levelCounter)

/** 创建字段初始化器出口节点。 */
fun ControlFlowGraphBuilder.createFieldInitializerExitNode(fir: CfirFieldVariable): FieldInitializerExitNode =
    FieldInitializerExitNode(currentGraph, fir, levelCounter)

/** 创建 value parameter 解析入口节点。 */
fun ControlFlowGraphBuilder.createEnterValueParameterNode(fir: CfirValueParameter): EnterValueParameterNode =
    EnterValueParameterNode(currentGraph, fir, levelCounter)

/** 创建 value parameter 解析出口节点。 */
fun ControlFlowGraphBuilder.createExitValueParameterNode(fir: CfirValueParameter): ExitValueParameterNode =
    ExitValueParameterNode(currentGraph, fir, levelCounter)

/** 创建默认参数表达式入口节点。 */
fun ControlFlowGraphBuilder.createEnterDefaultArgumentsNode(fir: CfirValueParameter): EnterDefaultArgumentsNode =
    EnterDefaultArgumentsNode(currentGraph, fir, levelCounter)

/** 创建默认参数表达式出口节点。 */
fun ControlFlowGraphBuilder.createExitDefaultArgumentsNode(fir: CfirValueParameter): ExitDefaultArgumentsNode =
    ExitDefaultArgumentsNode(currentGraph, fir, levelCounter)

// ----------------------------------- Anonymous function (仓颉 lambda) -----------------------------------

/** 创建匿名函数表达式节点，表示 lambda 表达式本身进入控制流。 */
fun ControlFlowGraphBuilder.createAnonymousFunctionExpressionNode(fir: CfirAnonymousFunctionExpression): AnonymousFunctionExpressionNode =
    AnonymousFunctionExpressionNode(currentGraph, fir, levelCounter)

/** 创建匿名函数捕获节点，用于记录 lambda 捕获上下文。 */
fun ControlFlowGraphBuilder.createAnonymousFunctionCaptureNode(fir: CfirAnonymousFunctionExpression): AnonymousFunctionCaptureNode =
    AnonymousFunctionCaptureNode(currentGraph, fir, levelCounter)

/** 创建 postponed lambda 出口节点。 */
fun ControlFlowGraphBuilder.createPostponedLambdaExitNode(fir: CfirAnonymousFunctionExpression): PostponedLambdaExitNode =
    PostponedLambdaExitNode(currentGraph, fir, levelCounter)

/** 创建 postponed lambda 分裂节点，用于把多个待分析 lambda 子图拆入控制流。 */
fun ControlFlowGraphBuilder.createSplitPostponedLambdasNode(
    fir: CfirStatement,
    lambdas: List<CfirFunction>,
): SplitPostponedLambdasNode =
    SplitPostponedLambdasNode(currentGraph, fir, lambdas, levelCounter)

/** 创建 postponed lambda 出口合并节点。 */
fun ControlFlowGraphBuilder.createMergePostponedLambdaExitsNode(fir: CfirElement): MergePostponedLambdaExitsNode =
    MergePostponedLambdaExitsNode(currentGraph, fir, levelCounter)

// ----------------------------------- Block -----------------------------------

/** 创建 block 入口节点。 */
fun ControlFlowGraphBuilder.createBlockEnterNode(fir: CfirBlock): BlockEnterNode =
    BlockEnterNode(currentGraph, fir, levelCounter)

/** 创建 block 出口节点。 */
fun ControlFlowGraphBuilder.createBlockExitNode(fir: CfirBlock): BlockExitNode =
    BlockExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Match -----------------------------------

/** 创建 match 表达式入口节点。 */
fun ControlFlowGraphBuilder.createMatchEnterNode(fir: CfirMatchExpression): MatchEnterNode =
    MatchEnterNode(currentGraph, fir, levelCounter)

/** 创建 match 表达式出口节点。 */
fun ControlFlowGraphBuilder.createMatchExitNode(fir: CfirMatchExpression): MatchExitNode =
    MatchExitNode(currentGraph, fir, levelCounter)

/** 创建 match 分支条件入口节点。 */
fun ControlFlowGraphBuilder.createMatchBranchConditionEnterNode(fir: CfirMatchBranch): MatchBranchConditionEnterNode =
    MatchBranchConditionEnterNode(currentGraph, fir, levelCounter)

/** 创建 match 分支条件出口节点。 */
fun ControlFlowGraphBuilder.createMatchBranchConditionExitNode(fir: CfirMatchBranch): MatchBranchConditionExitNode =
    MatchBranchConditionExitNode(currentGraph, fir, levelCounter)

/** 创建 match 分支结果入口节点。 */
fun ControlFlowGraphBuilder.createMatchBranchResultEnterNode(fir: CfirMatchBranch): MatchBranchResultEnterNode =
    MatchBranchResultEnterNode(currentGraph, fir, levelCounter)

/** 创建 match 分支结果出口节点。 */
fun ControlFlowGraphBuilder.createMatchBranchResultExitNode(fir: CfirMatchBranch): MatchBranchResultExitNode =
    MatchBranchResultExitNode(currentGraph, fir, levelCounter)

/** 创建 match 缺省 synthetic else 分支节点。 */
fun ControlFlowGraphBuilder.createMatchSyntheticElseBranchNode(fir: CfirMatchExpression): MatchSyntheticElseBranchNode =
    MatchSyntheticElseBranchNode(currentGraph, fir, levelCounter)

// ----------------------------------- If -----------------------------------

/** 创建 if 表达式入口节点。 */
fun ControlFlowGraphBuilder.createIfEnterNode(fir: CfirIfExpression): IfEnterNode =
    IfEnterNode(currentGraph, fir, levelCounter)

/** 创建 if 表达式出口节点。 */
fun ControlFlowGraphBuilder.createIfExitNode(fir: CfirIfExpression): IfExitNode =
    IfExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Loop (while / do-while / for-in) -----------------------------------

/** 创建循环表达式入口节点。 */
fun ControlFlowGraphBuilder.createLoopEnterNode(fir: CfirLoopExpression): LoopEnterNode =
    LoopEnterNode(currentGraph, fir, levelCounter)

/** 创建循环体入口节点。 */
fun ControlFlowGraphBuilder.createLoopBlockEnterNode(fir: CfirLoopExpression): LoopBlockEnterNode =
    LoopBlockEnterNode(currentGraph, fir, levelCounter)

/** 创建循环体出口节点。 */
fun ControlFlowGraphBuilder.createLoopBlockExitNode(fir: CfirLoopExpression): LoopBlockExitNode =
    LoopBlockExitNode(currentGraph, fir, levelCounter)

/** 创建循环条件入口节点。 */
fun ControlFlowGraphBuilder.createLoopConditionEnterNode(fir: CfirExpression, loop: CfirLoopExpression): LoopConditionEnterNode =
    LoopConditionEnterNode(currentGraph, fir, loop, levelCounter)

/** 创建循环条件出口节点。 */
fun ControlFlowGraphBuilder.createLoopConditionExitNode(fir: CfirExpression, loop: CfirLoopExpression): LoopConditionExitNode =
    LoopConditionExitNode(currentGraph, fir, loop, levelCounter)

/** 创建循环表达式出口节点。 */
fun ControlFlowGraphBuilder.createLoopExitNode(fir: CfirLoopExpression): LoopExitNode =
    LoopExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Try / catch / finally -----------------------------------

/** 创建 try 表达式入口节点。 */
fun ControlFlowGraphBuilder.createTryExpressionEnterNode(fir: CfirTryExpression): TryExpressionEnterNode =
    TryExpressionEnterNode(currentGraph, fir, levelCounter)

/** 创建 try 主体入口节点。 */
fun ControlFlowGraphBuilder.createTryMainBlockEnterNode(fir: CfirTryExpression): TryMainBlockEnterNode =
    TryMainBlockEnterNode(currentGraph, fir, levelCounter)

/** 创建 try 主体出口节点。 */
fun ControlFlowGraphBuilder.createTryMainBlockExitNode(fir: CfirTryExpression): TryMainBlockExitNode =
    TryMainBlockExitNode(currentGraph, fir, levelCounter)

/** 创建 catch 子句入口节点。 */
fun ControlFlowGraphBuilder.createCatchClauseEnterNode(fir: CfirCatch): CatchClauseEnterNode =
    CatchClauseEnterNode(currentGraph, fir, levelCounter)

/** 创建 catch 子句出口节点。 */
fun ControlFlowGraphBuilder.createCatchClauseExitNode(fir: CfirCatch): CatchClauseExitNode =
    CatchClauseExitNode(currentGraph, fir, levelCounter)

/** 创建 effect handle 子句入口节点。 */
fun ControlFlowGraphBuilder.createHandleClauseEnterNode(fir: CfirHandleClause): HandleClauseEnterNode =
    HandleClauseEnterNode(currentGraph, fir, levelCounter)

/** 创建 effect handle 子句出口节点。 */
fun ControlFlowGraphBuilder.createHandleClauseExitNode(fir: CfirHandleClause): HandleClauseExitNode =
    HandleClauseExitNode(currentGraph, fir, levelCounter)

/** 创建 finally block 入口节点。 */
fun ControlFlowGraphBuilder.createFinallyBlockEnterNode(fir: CfirTryExpression): FinallyBlockEnterNode =
    FinallyBlockEnterNode(currentGraph, fir, levelCounter)

/** 创建 finally block 出口节点，并保留入口节点用于成对建图。 */
fun ControlFlowGraphBuilder.createFinallyBlockExitNode(enterNode: FinallyBlockEnterNode): FinallyBlockExitNode =
    FinallyBlockExitNode(currentGraph, enterNode.fir, enterNode, levelCounter)

/** 创建 try 表达式出口节点。 */
fun ControlFlowGraphBuilder.createTryExpressionExitNode(fir: CfirTryExpression): TryExpressionExitNode =
    TryExpressionExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Boolean operators (&&, ||) -----------------------------------

/** 创建布尔短路操作入口节点。 */
fun ControlFlowGraphBuilder.createBooleanOperatorEnterNode(fir: CfirBinaryOp): BooleanOperatorEnterNode =
    BooleanOperatorEnterNode(currentGraph, fir, levelCounter)

/** 创建布尔短路操作左操作数出口节点。 */
fun ControlFlowGraphBuilder.createBooleanOperatorExitLeftOperandNode(fir: CfirBinaryOp): BooleanOperatorExitLeftOperandNode =
    BooleanOperatorExitLeftOperandNode(currentGraph, fir, levelCounter)

/** 创建布尔短路操作右操作数入口节点。 */
fun ControlFlowGraphBuilder.createBooleanOperatorEnterRightOperandNode(fir: CfirBinaryOp): BooleanOperatorEnterRightOperandNode =
    BooleanOperatorEnterRightOperandNode(currentGraph, fir, levelCounter)

/** 创建布尔短路操作出口节点，并记录左右操作数出口节点。 */
fun ControlFlowGraphBuilder.createBooleanOperatorExitNode(
    fir: CfirBinaryOp,
    leftOperandNode: CFGNode<*>,
    rightOperandNode: CFGNode<*>,
): BooleanOperatorExitNode =
    BooleanOperatorExitNode(currentGraph, fir, leftOperandNode, rightOperandNode, levelCounter)

// ----------------------------------- Operator calls / expressions -----------------------------------

/** 创建类型操作调用节点。 */
fun ControlFlowGraphBuilder.createTypeOperatorCallNode(fir: CfirTypeOperator): TypeOperatorCallNode =
    TypeOperatorCallNode(currentGraph, fir, levelCounter)

/** 创建比较表达式节点。 */
fun ControlFlowGraphBuilder.createComparisonExpressionNode(fir: CfirComparisonExpression): ComparisonExpressionNode =
    ComparisonExpressionNode(currentGraph, fir, levelCounter)

// ----------------------------------- Jump / literals / throw -----------------------------------

/** 创建跳转表达式节点。 */
fun ControlFlowGraphBuilder.createJumpNode(fir: CfirJump<*>): JumpNode =
    JumpNode(currentGraph, fir, levelCounter)

/** 创建字面量表达式节点。 */
fun ControlFlowGraphBuilder.createLiteralExpressionNode(fir: CfirLiteralExpression): LiteralExpressionNode =
    LiteralExpressionNode(currentGraph, fir, levelCounter)

/** 创建 throw 表达式节点。 */
fun ControlFlowGraphBuilder.createThrowExceptionNode(fir: CfirThrowExpression): ThrowExceptionNode =
    ThrowExceptionNode(currentGraph, fir, levelCounter)

// ----------------------------------- Call / access -----------------------------------

/** 创建限定访问表达式节点。 */
fun ControlFlowGraphBuilder.createQualifiedAccessNode(fir: CfirQualifiedAccessExpression): QualifiedAccessNode =
    QualifiedAccessNode(currentGraph, fir, levelCounter)

/** 创建函数调用参数列表入口节点。 */
fun ControlFlowGraphBuilder.createFunctionCallArgumentsEnterNode(fir: CfirFunctionCall): FunctionCallArgumentsEnterNode =
    FunctionCallArgumentsEnterNode(currentGraph, fir, levelCounter)

/** 创建函数调用参数列表出口节点，并记录显式接收者的出口节点。 */
fun ControlFlowGraphBuilder.createFunctionCallArgumentsExitNode(
    fir: CfirFunctionCall,
    explicitReceiverExitNode: CFGNode<*>,
): FunctionCallArgumentsExitNode =
    FunctionCallArgumentsExitNode(currentGraph, fir, explicitReceiverExitNode, levelCounter)

/** 创建函数调用 callee 入口节点。 */
fun ControlFlowGraphBuilder.createFunctionCallEnterNode(fir: CfirFunctionCall): FunctionCallEnterNode =
    FunctionCallEnterNode(currentGraph, fir, levelCounter)

/** 创建函数调用出口节点。 */
fun ControlFlowGraphBuilder.createFunctionCallExitNode(fir: CfirFunctionCall): FunctionCallExitNode =
    FunctionCallExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Variable (local let/var) -----------------------------------

/** 创建变量声明入口节点。 */
fun ControlFlowGraphBuilder.createVariableDeclarationEnterNode(fir: CfirVariable): VariableDeclarationEnterNode =
    VariableDeclarationEnterNode(currentGraph, fir, levelCounter)

/** 创建变量声明出口节点。 */
fun ControlFlowGraphBuilder.createVariableDeclarationExitNode(fir: CfirVariable): VariableDeclarationExitNode =
    VariableDeclarationExitNode(currentGraph, fir, levelCounter)

/** 创建变量赋值节点。 */
fun ControlFlowGraphBuilder.createVariableAssignmentNode(fir: CfirAssignment): VariableAssignmentNode =
    VariableAssignmentNode(currentGraph, fir, levelCounter)

// ----------------------------------- Optional chain (仓颉 ?.) -----------------------------------

/** 创建可选链入口节点。 */
fun ControlFlowGraphBuilder.createEnterOptionalChainNode(fir: CfirOptionalChainExpression): EnterOptionalChainNode =
    EnterOptionalChainNode(currentGraph, fir, levelCounter)

/** 创建可选链出口节点。 */
fun ControlFlowGraphBuilder.createExitOptionalChainNode(fir: CfirOptionalChainExpression): ExitOptionalChainNode =
    ExitOptionalChainNode(currentGraph, fir, levelCounter)

// ----------------------------------- Wrapped expression -----------------------------------

/** 创建 wrapped expression 节点。 */
fun ControlFlowGraphBuilder.createWrappedExpressionNode(fir: CfirWrappedExpression): WrappedExpressionNode =
    WrappedExpressionNode(currentGraph, fir, levelCounter)

// ----------------------------------- 仓颉特有:spawn / synchronized / unsafe -----------------------------------

/** 创建 spawn 表达式节点。 */
fun ControlFlowGraphBuilder.createSpawnExpressionNode(fir: CfirSpawnExpression): SpawnExpressionNode =
    SpawnExpressionNode(currentGraph, fir, levelCounter)

/** 创建 synchronized 表达式入口节点。 */
fun ControlFlowGraphBuilder.createSynchronizedEnterNode(fir: CfirSynchronizedExpression): SynchronizedEnterNode =
    SynchronizedEnterNode(currentGraph, fir, levelCounter)

/** 创建 synchronized 表达式出口节点。 */
fun ControlFlowGraphBuilder.createSynchronizedExitNode(fir: CfirSynchronizedExpression): SynchronizedExitNode =
    SynchronizedExitNode(currentGraph, fir, levelCounter)

/** 创建 unsafe 表达式入口节点。 */
fun ControlFlowGraphBuilder.createUnsafeEnterNode(fir: CfirUnsafeExpression): UnsafeEnterNode =
    UnsafeEnterNode(currentGraph, fir, levelCounter)

/** 创建 unsafe 表达式出口节点。 */
fun ControlFlowGraphBuilder.createUnsafeExitNode(fir: CfirUnsafeExpression): UnsafeExitNode =
    UnsafeExitNode(currentGraph, fir, levelCounter)
