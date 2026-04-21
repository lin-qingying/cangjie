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
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
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

fun ControlFlowGraphBuilder.createStubNode(): StubNode = StubNode(currentGraph, levelCounter)

fun ControlFlowGraphBuilder.createFakeExpressionEnterNode(): FakeExpressionEnterNode =
    FakeExpressionEnterNode(currentGraph, levelCounter)

// ----------------------------------- Declarations / graphs -----------------------------------

fun ControlFlowGraphBuilder.createFileEnterNode(fir: CfirFile): FileEnterNode =
    FileEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFileExitNode(fir: CfirFile): FileExitNode =
    FileExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createClassEnterNode(fir: CfirClass): ClassEnterNode =
    ClassEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createClassExitNode(fir: CfirClass): ClassExitNode =
    ClassExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLocalClassExitNode(fir: CfirClass): LocalClassExitNode =
    LocalClassExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCodeFragmentEnterNode(fir: CfirCodeFragment): CodeFragmentEnterNode =
    CodeFragmentEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCodeFragmentExitNode(fir: CfirCodeFragment): CodeFragmentExitNode =
    CodeFragmentExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionEnterNode(fir: CfirFunction): FunctionEnterNode =
    FunctionEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionExitNode(fir: CfirFunction): FunctionExitNode =
    FunctionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLocalFunctionDeclarationNode(fir: CfirFunction): LocalFunctionDeclarationNode =
    LocalFunctionDeclarationNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFieldInitializerEnterNode(fir: CfirFieldVariable): FieldInitializerEnterNode =
    FieldInitializerEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFieldInitializerExitNode(fir: CfirFieldVariable): FieldInitializerExitNode =
    FieldInitializerExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterValueParameterNode(fir: CfirValueParameter): EnterValueParameterNode =
    EnterValueParameterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitValueParameterNode(fir: CfirValueParameter): ExitValueParameterNode =
    ExitValueParameterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createEnterDefaultArgumentsNode(fir: CfirValueParameter): EnterDefaultArgumentsNode =
    EnterDefaultArgumentsNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitDefaultArgumentsNode(fir: CfirValueParameter): ExitDefaultArgumentsNode =
    ExitDefaultArgumentsNode(currentGraph, fir, levelCounter)

// ----------------------------------- Anonymous function (仓颉 lambda) -----------------------------------

fun ControlFlowGraphBuilder.createAnonymousFunctionExpressionNode(fir: CfirAnonymousFunctionExpression): AnonymousFunctionExpressionNode =
    AnonymousFunctionExpressionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createAnonymousFunctionCaptureNode(fir: CfirAnonymousFunctionExpression): AnonymousFunctionCaptureNode =
    AnonymousFunctionCaptureNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createPostponedLambdaExitNode(fir: CfirAnonymousFunctionExpression): PostponedLambdaExitNode =
    PostponedLambdaExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createSplitPostponedLambdasNode(
    fir: CfirStatement,
    lambdas: List<CfirFunction>,
): SplitPostponedLambdasNode =
    SplitPostponedLambdasNode(currentGraph, fir, lambdas, levelCounter)

fun ControlFlowGraphBuilder.createMergePostponedLambdaExitsNode(fir: CfirElement): MergePostponedLambdaExitsNode =
    MergePostponedLambdaExitsNode(currentGraph, fir, levelCounter)

// ----------------------------------- Block -----------------------------------

fun ControlFlowGraphBuilder.createBlockEnterNode(fir: CfirBlock): BlockEnterNode =
    BlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBlockExitNode(fir: CfirBlock): BlockExitNode =
    BlockExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Match -----------------------------------

fun ControlFlowGraphBuilder.createMatchEnterNode(fir: CfirMatchExpression): MatchEnterNode =
    MatchEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createMatchExitNode(fir: CfirMatchExpression): MatchExitNode =
    MatchExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createMatchBranchConditionEnterNode(fir: CfirMatchBranch): MatchBranchConditionEnterNode =
    MatchBranchConditionEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createMatchBranchConditionExitNode(fir: CfirMatchBranch): MatchBranchConditionExitNode =
    MatchBranchConditionExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createMatchBranchResultEnterNode(fir: CfirMatchBranch): MatchBranchResultEnterNode =
    MatchBranchResultEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createMatchBranchResultExitNode(fir: CfirMatchBranch): MatchBranchResultExitNode =
    MatchBranchResultExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createMatchSyntheticElseBranchNode(fir: CfirMatchExpression): MatchSyntheticElseBranchNode =
    MatchSyntheticElseBranchNode(currentGraph, fir, levelCounter)

// ----------------------------------- If -----------------------------------

fun ControlFlowGraphBuilder.createIfEnterNode(fir: CfirIfExpression): IfEnterNode =
    IfEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createIfExitNode(fir: CfirIfExpression): IfExitNode =
    IfExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Loop (while / do-while / for-in) -----------------------------------

fun ControlFlowGraphBuilder.createLoopEnterNode(fir: CfirExpression): LoopEnterNode =
    LoopEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopBlockEnterNode(fir: CfirExpression): LoopBlockEnterNode =
    LoopBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopBlockExitNode(fir: CfirExpression): LoopBlockExitNode =
    LoopBlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLoopConditionEnterNode(fir: CfirExpression, loop: CfirExpression): LoopConditionEnterNode =
    LoopConditionEnterNode(currentGraph, fir, loop, levelCounter)

fun ControlFlowGraphBuilder.createLoopConditionExitNode(fir: CfirExpression, loop: CfirExpression): LoopConditionExitNode =
    LoopConditionExitNode(currentGraph, fir, loop, levelCounter)

fun ControlFlowGraphBuilder.createLoopExitNode(fir: CfirExpression): LoopExitNode =
    LoopExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Try / catch / finally -----------------------------------

fun ControlFlowGraphBuilder.createTryExpressionEnterNode(fir: CfirTryExpression): TryExpressionEnterNode =
    TryExpressionEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryMainBlockEnterNode(fir: CfirTryExpression): TryMainBlockEnterNode =
    TryMainBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createTryMainBlockExitNode(fir: CfirTryExpression): TryMainBlockExitNode =
    TryMainBlockExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCatchClauseEnterNode(fir: CfirCatch): CatchClauseEnterNode =
    CatchClauseEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createCatchClauseExitNode(fir: CfirCatch): CatchClauseExitNode =
    CatchClauseExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyBlockEnterNode(fir: CfirTryExpression): FinallyBlockEnterNode =
    FinallyBlockEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFinallyBlockExitNode(enterNode: FinallyBlockEnterNode): FinallyBlockExitNode =
    FinallyBlockExitNode(currentGraph, enterNode.fir, enterNode, levelCounter)

fun ControlFlowGraphBuilder.createTryExpressionExitNode(fir: CfirTryExpression): TryExpressionExitNode =
    TryExpressionExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Boolean operators (&&, ||) -----------------------------------

fun ControlFlowGraphBuilder.createBooleanOperatorEnterNode(fir: CfirBinaryOp): BooleanOperatorEnterNode =
    BooleanOperatorEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBooleanOperatorExitLeftOperandNode(fir: CfirBinaryOp): BooleanOperatorExitLeftOperandNode =
    BooleanOperatorExitLeftOperandNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBooleanOperatorEnterRightOperandNode(fir: CfirBinaryOp): BooleanOperatorEnterRightOperandNode =
    BooleanOperatorEnterRightOperandNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createBooleanOperatorExitNode(
    fir: CfirBinaryOp,
    leftOperandNode: CFGNode<*>,
    rightOperandNode: CFGNode<*>,
): BooleanOperatorExitNode =
    BooleanOperatorExitNode(currentGraph, fir, leftOperandNode, rightOperandNode, levelCounter)

// ----------------------------------- Operator calls / expressions -----------------------------------

fun ControlFlowGraphBuilder.createTypeOperatorCallNode(fir: CfirTypeOperator): TypeOperatorCallNode =
    TypeOperatorCallNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createComparisonExpressionNode(fir: CfirComparisonExpression): ComparisonExpressionNode =
    ComparisonExpressionNode(currentGraph, fir, levelCounter)

// ----------------------------------- Jump / literals / throw -----------------------------------

fun ControlFlowGraphBuilder.createJumpNode(fir: CfirJump<*>): JumpNode =
    JumpNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createLiteralExpressionNode(fir: CfirLiteralExpression): LiteralExpressionNode =
    LiteralExpressionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createThrowExceptionNode(fir: CfirThrowExpression): ThrowExceptionNode =
    ThrowExceptionNode(currentGraph, fir, levelCounter)

// ----------------------------------- Call / access -----------------------------------

fun ControlFlowGraphBuilder.createQualifiedAccessNode(fir: CfirQualifiedAccessExpression): QualifiedAccessNode =
    QualifiedAccessNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionCallArgumentsEnterNode(fir: CfirFunctionCall): FunctionCallArgumentsEnterNode =
    FunctionCallArgumentsEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionCallArgumentsExitNode(
    fir: CfirFunctionCall,
    explicitReceiverExitNode: CFGNode<*>,
): FunctionCallArgumentsExitNode =
    FunctionCallArgumentsExitNode(currentGraph, fir, explicitReceiverExitNode, levelCounter)

fun ControlFlowGraphBuilder.createFunctionCallEnterNode(fir: CfirFunctionCall): FunctionCallEnterNode =
    FunctionCallEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createFunctionCallExitNode(fir: CfirFunctionCall): FunctionCallExitNode =
    FunctionCallExitNode(currentGraph, fir, levelCounter)

// ----------------------------------- Variable (local let/var) -----------------------------------

fun ControlFlowGraphBuilder.createVariableDeclarationEnterNode(fir: CfirVariable): VariableDeclarationEnterNode =
    VariableDeclarationEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createVariableDeclarationExitNode(fir: CfirVariable): VariableDeclarationExitNode =
    VariableDeclarationExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createVariableAssignmentNode(fir: CfirAssignment): VariableAssignmentNode =
    VariableAssignmentNode(currentGraph, fir, levelCounter)

// ----------------------------------- Optional chain (仓颉 ?.) -----------------------------------

fun ControlFlowGraphBuilder.createEnterOptionalChainNode(fir: CfirOptionalChainExpression): EnterOptionalChainNode =
    EnterOptionalChainNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createExitOptionalChainNode(fir: CfirOptionalChainExpression): ExitOptionalChainNode =
    ExitOptionalChainNode(currentGraph, fir, levelCounter)

// ----------------------------------- Wrapped expression -----------------------------------

fun ControlFlowGraphBuilder.createWrappedExpressionNode(fir: CfirWrappedExpression): WrappedExpressionNode =
    WrappedExpressionNode(currentGraph, fir, levelCounter)

// ----------------------------------- 仓颉特有:spawn / synchronized / unsafe -----------------------------------

fun ControlFlowGraphBuilder.createSpawnExpressionNode(fir: CfirSpawnExpression): SpawnExpressionNode =
    SpawnExpressionNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createSynchronizedEnterNode(fir: CfirSynchronizedExpression): SynchronizedEnterNode =
    SynchronizedEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createSynchronizedExitNode(fir: CfirSynchronizedExpression): SynchronizedExitNode =
    SynchronizedExitNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createUnsafeEnterNode(fir: CfirUnsafeExpression): UnsafeEnterNode =
    UnsafeEnterNode(currentGraph, fir, levelCounter)

fun ControlFlowGraphBuilder.createUnsafeExitNode(fir: CfirUnsafeExpression): UnsafeExitNode =
    UnsafeExitNode(currentGraph, fir, levelCounter)
