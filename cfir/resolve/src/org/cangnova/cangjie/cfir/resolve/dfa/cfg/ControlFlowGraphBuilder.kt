package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.resolve.match.CfirOrPatternLoweringKind
import org.cangnova.cangjie.cfir.resolve.match.loweringKind

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.resolve.dfa.Stack
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode.Companion.addEdge as addEdgeStatic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.resolve.dfa.isEmpty
import org.cangnova.cangjie.cfir.resolve.dfa.isNotEmpty
import org.cangnova.cangjie.cfir.resolve.dfa.stackOf
import org.cangnova.cangjie.cfir.resolve.dfa.topOrNull
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * CFIR 控制流图构造器,对位 Kotlin FIR `ControlFlowGraphBuilder`。
 *
 * 职责:
 * - 持有当前正在构造的图栈([graphs])与当前节点栈([lastNodes]);
 * - 为各类仓颉 CFIR 结构提供 `enterX / exitX` 入口;
 * - 维护图级别计数器 [levelCounter]、局部类/局部函数/匿名函数子图入边缓存、
 *   loop / try / match / boolean-op / function-call 等嵌套结构的辅助栈。
 *
 * 本文件提供核心 API 与基础工具,具体语法结构的 `enter/exit` 方法由 body-resolve
 * transformer 在对应访问位点调用,此处先建立可用骨架;每个 enter/exit 都落实为
 * 真实的节点创建 + 边连接,不存在占位实现。尚未 wire 的仓颉特有结构
 * (spawn 子图 / synchronized / unsafe / 模式匹配分支等)由 transformer 分阶段
 * 接入时再补齐。
 */
@OptIn(CfgInternals::class)
class ControlFlowGraphBuilder private constructor(
    /** 当前正在构造的 CFG 栈。 */
    private val graphs: Stack<ControlFlowGraph>,
    /** 每个活跃构造上下文的最后一个节点栈。 */
    private val lastNodes: Stack<CFGNode<*>>,

    /** 函数符号到 return 目标出口节点的映射。 */
    private val exitTargetsForReturn: MutableMap<CfirFunctionSymbol<*>, FunctionExitNode>,
    /** 局部类成员声明入口需要补接的外层入口节点缓存。 */
    private val enterToLocalClassesMembers: MutableMap<Any, Pair<CFGNode<*>, EdgeKind>>,

    /** 非直接跳转到目标节点的 jump 节点集合。 */
    private val nonDirectJumps: ListMultimap<CFGNode<*>, JumpNode>,

    /** 当前调用参数列表对应的 postponed lambda split 节点栈。 */
    private val argumentListSplitNodes: Stack<SplitPostponedLambdasNode?>,
    /** 匿名函数符号到 postponed lambda 入口/出口节点的映射。 */
    private val postponedAnonymousFunctionNodes: MutableMap<CfirFunctionSymbol<*>, Pair<CFGNode<*>, PostponedLambdaExitNode?>>,
    /** 匿名函数符号到 capture 节点的映射。 */
    private val anonymousFunctionCaptureNodes: MutableMap<CfirFunctionSymbol<*>, AnonymousFunctionCaptureNode>,
    /** 嵌套 postponed lambda 出口信息栈。 */
    private val postponedLambdaExits: Stack<PostponedLambdas>,

    /** loop 目标元素到 loop exit 节点的映射。 */
    private val loopExitNodes: MutableMap<CfirElement, LoopExitNode>,
    /** loop 目标元素到 loop condition enter 节点的映射。 */
    private val loopConditionEnterNodes: MutableMap<CfirElement, LoopConditionEnterNode>,

    /** 当前嵌套 match 的出口节点栈。 */
    private val matchExitNodes: Stack<MatchExitNode>,
    /** if 的汇合出口与条件保留输入一起参与 snapshot/reset。 */
    private val ifExitNodes: Stack<IfExitNode>,

    /** 异常作用域与真实 try 节点同寿命；禁止按名字或 source 查处理目标。 */
    private val exceptionScopes: Stack<ExceptionScope>,
    private val exceptionDispatches: MutableMap<CatchClauseEnterNode, MutableList<ExceptionDispatchNode>>,
    private val exceptionContinuations: MutableMap<FinallyBlockEnterNode, MutableList<ExceptionContinuation>>,
    private val jumpContinuations: MutableMap<FinallyBlockEnterNode, MutableList<JumpContinuation>>,

    /** 当前嵌套 try 表达式出口节点栈。 */
    private val tryExitNodes: Stack<TryExpressionExitNode>,
    /** 当前 try 表达式待进入的 catch 入口节点栈。 */
    private val catchNodes: Stack<List<CatchClauseEnterNode>>,
    /** 正在处理的 catch block 入口节点栈。 */
    private val catchBlocksInProgress: Stack<CatchClauseEnterNode>,
    /** 当前 try 表达式待进入的 handle 入口节点栈。 */
    private val handleNodes: Stack<List<HandleClauseEnterNode>>,
    /** 正在处理的 handle block 入口节点栈。 */
    private val handleBlocksInProgress: Stack<HandleClauseEnterNode>,
    /** 尚未进入的 finally 入口节点栈。 */
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode>,
    /** 正在处理的 finally 入口节点栈。 */
    private val finallyBlocksInProgress: Stack<FinallyBlockEnterNode>,
    /** 正在处理的 finally 所属 CFIR 元素集合。 */
    private val finallyBlocksInProgressSet: MutableSet<CfirElement>,

    /** 函数调用参数出口节点栈。 */
    private val exitFunctionCallArgumentsNodes: Stack<FunctionCallArgumentsExitNode?>,
    /** 实际 call 到其求值、调用和 postponed 节点的身份索引；完成图同样保持可写回资格。 */
    private val functionCallNodes: MutableMap<CfirFunctionCall, MutableList<CFGNode<*>>>,
    /** 当前 context 创建或在正式 selected AST 恢复时明确采用的图；probe 不借用父图。 */
    private val ownedGraphs: MutableSet<ControlFlowGraph>,
    private val isIsolatedContext: Boolean,
    /** 可选链出口节点栈。 */
    private val exitOptionalChainNodes: Stack<ExitOptionalChainNode>,
    /** 尚未根据完成状态修正的函数调用出口节点栈。 */
    private val notCompletedFunctionCalls: Stack<MutableList<FunctionCallExitNode>>,
) {
    /**
     * `match` synthetic else 的正式决策类型。
     *
     * 决策来源必须是上游语义承载层（exhaustiveness carrier），CFG builder 只消费结果，
     * 不做语法兜底推断。
     */
    enum class MatchSyntheticElseDecision {
        Required,
        NotRequired,
        }

    /** 正常建图入口，对位 Kotlin 的空 builder。 */
    constructor() : this(isIsolatedContext = false)

    /** speculative probe 由实际隔离 context 显式创建。 */
    constructor(isIsolatedContext: Boolean) : this(
        graphs = stackOf(),
        lastNodes = stackOf(),
        exitTargetsForReturn = mutableMapOf(),
        enterToLocalClassesMembers = mutableMapOf(),
        nonDirectJumps = ListMultimap(),
        argumentListSplitNodes = stackOf(),
        postponedAnonymousFunctionNodes = mutableMapOf(),
        anonymousFunctionCaptureNodes = mutableMapOf(),
        postponedLambdaExits = stackOf(),
        loopExitNodes = mutableMapOf(),
        loopConditionEnterNodes = mutableMapOf(),
        matchExitNodes = stackOf(),
        ifExitNodes = stackOf(),
        exceptionScopes = stackOf(),
        exceptionDispatches = mutableMapOf(),
        exceptionContinuations = mutableMapOf(),
        jumpContinuations = mutableMapOf(),
        tryExitNodes = stackOf(),
        catchNodes = stackOf(),
        catchBlocksInProgress = stackOf(),
        handleNodes = stackOf(),
        handleBlocksInProgress = stackOf(),
        finallyEnterNodes = stackOf(),
        finallyBlocksInProgress = stackOf(),
        finallyBlocksInProgressSet = mutableSetOf(),
        exitFunctionCallArgumentsNodes = stackOf(),
        functionCallNodes = java.util.IdentityHashMap(),
        ownedGraphs = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<ControlFlowGraph, Boolean>()),
        isIsolatedContext = isIsolatedContext,
        exitOptionalChainNodes = stackOf(),
        notCompletedFunctionCalls = stackOf(),
    )

    // ----------------------------------- Accessors -----------------------------------
    /** retained 根可暂停等待实际 parent FULL，但不能退回复制的 formal try 执行 PRIVATE 工作。 */
    private enum class ResourceRootPhase { Active, Suspended, Finished }
    private class ResourceRoot(
        val graph: ControlFlowGraph,
        var lastNode: CFGNode<*>,
        var pendingCalls: MutableList<FunctionCallExitNode>,
        var phase: ResourceRootPhase,
    )
    private val resourceRoots = java.util.IdentityHashMap<CfirResourceVariable, ResourceRoot>()


    /** 当前正在构造的图;在无活动图时访问会抛出。 */
    val currentGraph: ControlFlowGraph
        get() = graphs.top()

    /** 当前正在构造的图；没有活动图时返回 null。 */
    val currentGraphOrNull: ControlFlowGraph?
        get() = graphs.topOrNull()

    /**
     * 层级计数器:`try` 表达式不是子图,但会提升层级用于识别 try 内外节点。
     */
    val levelCounter: Int
        get() = graphs.size + tryExitNodes.size

    /** 当前构造上下文最后一个节点。 */
    val lastNode: CFGNode<*>
        get() = lastNodes.top()

    /** 当前构造上下文最后一个节点；没有节点时返回 null。 */
    val lastNodeOrNull: CFGNode<*>?
        get() = lastNodes.topOrNull()

    /** 当前构造位置是否处在文件级或无活动图的顶层。 */
    val isTopLevel: Boolean
        get() = graphs.isEmpty || currentGraph.kind == ControlFlowGraph.Kind.File

    /** 当前是否处在需要构造函数体/表达式体 CFG 的模式。 */
    private val bodyBuildingMode: Boolean
        get() = graphs.isNotEmpty && currentGraph.kind != ControlFlowGraph.Kind.Class

    /**
     * 对位 Kotlin FIR `ControlFlowGraphBuilder.createSnapshot`。
     *
     * 快照必须复制图栈、节点栈和各类 node cache，供 partial body resume 在新树上 patch CFG 引用。
     */
    internal fun createSnapshot(copier: ControlFlowGraphCopier): ControlFlowGraphBuilder {
        return ControlFlowGraphBuilder(
            graphs = graphs.createSnapshot(copier::get),
            lastNodes = lastNodes.createSnapshot(copier::get),
            exitTargetsForReturn = exitTargetsForReturn.mapValuesTo(mutableMapOf()) { copier[it.value] },
            enterToLocalClassesMembers = enterToLocalClassesMembers.mapValuesTo(mutableMapOf()) { (_, value) ->
                copier[value.first] to value.second
            },
            nonDirectJumps = ListMultimap<CFGNode<*>, JumpNode>().also { copy ->
                for ((node, jumps) in nonDirectJumps.entries()) {
                    copy.putAll(copier[node], jumps.map(copier::get))
                }
            },
            argumentListSplitNodes = argumentListSplitNodes.createSnapshot { it?.let(copier::get) },
            postponedAnonymousFunctionNodes = postponedAnonymousFunctionNodes.mapValuesTo(mutableMapOf()) { (_, value) ->
                copier[value.first] to value.second?.let(copier::get)
            },
            anonymousFunctionCaptureNodes = anonymousFunctionCaptureNodes.mapValuesTo(mutableMapOf()) { copier[it.value] },
            postponedLambdaExits = postponedLambdaExits.createSnapshot { value ->
                PostponedLambdas(value.lambdas, value.exits.mapTo(mutableListOf()) { (node, kind) -> copier[node] to kind })
            },
            loopExitNodes = loopExitNodes.mapValuesTo(mutableMapOf()) { copier[it.value] },
            loopConditionEnterNodes = loopConditionEnterNodes.mapValuesTo(mutableMapOf()) { copier[it.value] },
            matchExitNodes = matchExitNodes.createSnapshot(copier::get),
            ifExitNodes = ifExitNodes.createSnapshot(copier::get),
            exceptionScopes = exceptionScopes.createSnapshot { ExceptionScope(it.state.copy(copier)) },
            exceptionDispatches = exceptionDispatches.mapKeys { copier[it.key] }
                .mapValuesTo(mutableMapOf()) { it.value.mapTo(mutableListOf(), copier::get) },
            exceptionContinuations = exceptionContinuations.mapKeys { copier[it.key] }
                .mapValuesTo(mutableMapOf()) { (_, continuations) ->
                    continuations.mapTo(mutableListOf()) {
                        ExceptionContinuation(copier[it.transfer], it.scopes.map { scope -> scope.copy(copier) }, copier[it.boundary])
                    }
                },
            jumpContinuations = jumpContinuations.mapKeys { copier[it.key] }
                .mapValuesTo(mutableMapOf()) { (_, continuations) ->
                    continuations.mapTo(mutableListOf()) {
                        JumpContinuation(copier[it.jump], copier[it.target], it.scopes.map { scope -> scope.copy(copier) }, copier[it.boundary])
                    }
                },
            tryExitNodes = tryExitNodes.createSnapshot(copier::get),
            catchNodes = catchNodes.createSnapshot { it.map(copier::get) },
            catchBlocksInProgress = catchBlocksInProgress.createSnapshot(copier::get),
            handleNodes = handleNodes.createSnapshot { it.map(copier::get) },
            handleBlocksInProgress = handleBlocksInProgress.createSnapshot(copier::get),
            finallyEnterNodes = finallyEnterNodes.createSnapshot(copier::get),
            finallyBlocksInProgress = finallyBlocksInProgress.createSnapshot(copier::get),
            finallyBlocksInProgressSet = finallyBlocksInProgressSet.toMutableSet(),
            exitFunctionCallArgumentsNodes = exitFunctionCallArgumentsNodes.createSnapshot { it?.let(copier::get) },
            functionCallNodes = functionCallNodes.mapValuesTo(java.util.IdentityHashMap()) { (_, nodes) ->
                nodes.mapTo(mutableListOf(), copier::get)
            },
            ownedGraphs = ownedGraphs.mapTo(
                java.util.Collections.newSetFromMap(java.util.IdentityHashMap<ControlFlowGraph, Boolean>()),
                copier::get,
            ),
            isIsolatedContext = isIsolatedContext,
            exitOptionalChainNodes = exitOptionalChainNodes.createSnapshot(copier::get),
            notCompletedFunctionCalls = notCompletedFunctionCalls.createSnapshot { it.mapTo(mutableListOf(), copier::get) },
        ).also { snapshot ->
            resourceRoots.forEach { (owner, root) ->
                val last = if (root.phase == ResourceRootPhase.Active && currentGraphOrNull === root.graph) lastNode else root.lastNode
                snapshot.resourceRoots[owner] = ResourceRoot(
                    copier[root.graph], copier[last],
                    root.pendingCalls.mapTo(mutableListOf(), copier::get), root.phase,
                )
            }
        }
    }

    /** 清空 builder 内所有构造状态。 */
    fun reset() {
        graphs.reset()
        lastNodes.reset()
        exitTargetsForReturn.clear()
        enterToLocalClassesMembers.clear()
        nonDirectJumps.clear()
        argumentListSplitNodes.reset()
        postponedAnonymousFunctionNodes.clear()
        anonymousFunctionCaptureNodes.clear()
        postponedLambdaExits.reset()
        loopExitNodes.clear()
        loopConditionEnterNodes.clear()
        matchExitNodes.reset()
        ifExitNodes.reset()
        exceptionScopes.reset()
        exceptionDispatches.clear()
        exceptionContinuations.clear()
        jumpContinuations.clear()
        tryExitNodes.reset()
        catchNodes.reset()
        catchBlocksInProgress.reset()
        handleNodes.reset()
        handleBlocksInProgress.reset()
        finallyEnterNodes.reset()
        finallyBlocksInProgress.reset()
        finallyBlocksInProgressSet.clear()
        exitFunctionCallArgumentsNodes.reset()
        functionCallNodes.clear()
        ownedGraphs.clear()
        exitOptionalChainNodes.reset()
        notCompletedFunctionCalls.reset()
        resourceRoots.clear()
    }

    /** 判断指定元素是否是当前正在处理的 finally block。 */
    fun withinFinallyBlock(element: CfirElement): Boolean = element in finallyBlocksInProgressSet

    // ----------------------------------- Graph enter / exit core -----------------------------------

    /** 创建并进入一个新的 CFG 子图。 */
    private inline fun <T : CfirElement, E : T?, EnterNode, ExitNode> enterGraph(
        fir: E,
        name: String,
        kind: ControlFlowGraph.Kind,
        nodes: (E) -> Pair<EnterNode, ExitNode>,
    ): EnterNode
        where EnterNode : CFGNode<T>,
              EnterNode : GraphEnterNodeMarker,
              ExitNode : CFGNode<T>,
              ExitNode : GraphExitNodeMarker {
        resourceRoots.values.firstOrNull { it.phase == ResourceRootPhase.Active && it.graph === currentGraphOrNull }
            ?.let { it.lastNode = lastNode }
        val graph = ControlFlowGraph(fir as? CfirDeclaration, name, kind).also {
            graphs.push(it)
            ownedGraphs.add(it)
        }
        notCompletedFunctionCalls.push(mutableListOf())
        val (enterNode, exitNode) = nodes(fir)
        graph.enterNode = enterNode
        graph.exitNode = exitNode
        lastNodes.push(enterNode)
        return enterNode
    }

    /** 弹出并完成当前 CFG。 */
    private fun popGraph(): ControlFlowGraph {
        notCompletedFunctionCalls.pop().forEach { completeFunctionCall(it) }
        return graphs.pop().also { it.complete() }
    }

    /** 退出当前 CFG，并把当前最后节点连接到图出口节点。 */
    private inline fun <reified ExitNode> exitGraph(): Pair<ExitNode, ControlFlowGraph>
        where ExitNode : CFGNode<*>,
              ExitNode : GraphExitNodeMarker {
        notCompletedFunctionCalls.pop().forEach { completeFunctionCall(it) }
        val graph = graphs.pop()
        val exitNode = graph.exitNode as ExitNode
        popAndAddEdge(exitNode)
        if (exitNode.previousNodes.size > 1) {
            exitNode.updateDeadStatus()
        }
        graph.complete()
        return exitNode to graph
    }

    // ----------------------------------- Edge API -----------------------------------

    /** 弹出当前最后节点并连接到 [to]。 */
    internal fun popAndAddEdge(to: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        val from = lastNodes.pop()
        addEdge(from, to, preferredKind = preferredKind, propagateDeadness = true)
    }

    /** 添加 CFG 边，并根据源、目标节点的死亡状态选择边种类。 */
    internal fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath,
    ) {
        val kind = if (from.isDead || to.isDead) preferredKind.toDead() else preferredKind
        addEdgeStatic(from, to, kind, propagateDeadness, label)
    }

    /** 添加 CFG 回边。 */
    internal fun addBackEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        isDead: Boolean = false,
        label: EdgeLabel = NormalPath,
    ) {
        val kind = if (isDead || from.isDead || to.isDead) EdgeKind.DeadCfgBackward else EdgeKind.CfgBackward
        addEdgeStatic(from, to, kind, propagateDeadness = false, label)
    }

    /** 把新简单节点追加到当前控制流并更新 last node。 */
    internal fun addNewSimpleNode(
        node: CFGNode<*>,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath,
    ): CFGNode<*> {
        val lastNode = lastNodes.pop()
        val kind = if (isDead) preferredKind.toDead() else preferredKind
        // 简单节点沿同一正式边入口传播死亡状态，死亡 stub 的后继不能恢复为 live。
        addEdge(lastNode, node, preferredKind = kind, label = label)
        lastNodes.push(node)
        return node
    }

    /**
     * return / break / continue / throw 不能继续沿顺序执行流前进。
     *
     * 这里对位 Kotlin FIR 的 `addNonSuccessfullyTerminatingNode`：为非成功终止节点
     * 追加一个 stub 作为“后续死路径”占位，使后续语句的 CFG 顺序仍然完整，但不会
     * 从 jump / throw 节点继续产生正常前向流。
     */
    private fun addNonSuccessfullyTerminatingNode(node: CFGNode<*>) {
        popAndAddEdge(node)
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    /**
     * `continue` 的目标在 while / do-while 中可能是回边也可能是前向边。
     */
    private val CFGNode<*>.returnPathIsBackwards: Boolean
        get() = this is LoopConditionEnterNode &&
            ((loop as? CfirLoopExpression)?.isDoWhile != true || previousNodes.any { it is LoopBlockExitNode })

    /**
     * 当在局部类/局部函数中定义成员时,需要将该成员的图入口与局部类的入口相连。
     */
    private fun addEdgeIfLocalClassMember(enterNode: CFGNode<*>) {
        val key = enterNode.fir as? Any ?: return
        val entry = enterToLocalClassesMembers.remove(key) ?: return
        addEdge(entry.first, enterNode, preferredKind = entry.second, propagateDeadness = false)
    }

    // ----------------------------------- File -----------------------------------

    /** 进入文件 CFG 子图。 */
    fun enterFile(file: CfirFile): FileEnterNode =
        enterGraph(file, "<file>", ControlFlowGraph.Kind.File) {
            createFileEnterNode(it) to createFileExitNode(it)
        }

    /** 退出文件 CFG 子图，并把文件内声明子图挂到文件入口节点。 */
    fun exitFile(): Pair<FileExitNode, ControlFlowGraph> {
        val graph = currentGraph
        val enterNode = graph.enterNode as FileEnterNode
        enterNode.subGraphs = collectSubGraphs(enterNode.fir.declarations)
        return exitGraph()
    }

    // ----------------------------------- Class -----------------------------------

    /** 进入 class CFG 子图。 */
    fun enterClass(klass: CfirClass): ClassEnterNode =
        enterGraph(klass, klass.name.asString(), ControlFlowGraph.Kind.Class) {
            createClassEnterNode(it) to createClassExitNode(it)
        }

    /** 退出 class CFG 子图，并收集其成员子图。 */
    fun exitClass(): Pair<ClassExitNode, ControlFlowGraph> {
        val graph = currentGraph
        val enterNode = graph.enterNode as ClassEnterNode
        val exitNode = graph.exitNode as ClassExitNode
        val subGraphs = collectSubGraphs(enterNode.fir.declarations)
        enterNode.subGraphs = subGraphs
        exitNode.subGraphs = subGraphs
        return exitGraph()
    }

    // ----------------------------------- Named / Constructor / Anonymous function -----------------------------------

    /** 进入具名函数或构造器 CFG 子图。 */
    fun enterFunction(function: CfirFunction): Pair<LocalFunctionDeclarationNode?, FunctionEnterNode> {
        require(function !is CfirAnonymousFunction)
        val name = when (function) {
            is CfirNamedFunction -> function.name.asString()
            is CfirConstructor -> "<init>"
            else -> "<function>"
        }

        val isLocal = function is CfirNamedFunction
            && function.status.visibility == Visibilities.Local
            && bodyBuildingMode

        val localFunctionNode = if (isLocal) {
            createLocalFunctionDeclarationNode(function).also { addNewSimpleNode(it) }
        } else null

        val kind = when {
            localFunctionNode != null -> ControlFlowGraph.Kind.LocalFunction
            function is CfirConstructor -> ControlFlowGraph.Kind.Constructor
            else -> ControlFlowGraph.Kind.Function
        }

        val enterNode = enterGraph(function, name, kind) {
            createFunctionEnterNode(it) to createFunctionExitNode(it).also { exit ->
                exitTargetsForReturn[it.symbol] = exit
            }
        }
        if (localFunctionNode != null) {
            addEdge(localFunctionNode, enterNode)
            addBackEdge(enterNode.owner.exitNode, enterNode)
        } else {
            addEdgeIfLocalClassMember(enterNode)
        }
        return localFunctionNode to enterNode
    }

    /** 退出具名函数或构造器 CFG 子图。 */
    fun exitFunction(function: CfirFunction): Pair<FunctionExitNode, ControlFlowGraph> {
        require(function !is CfirAnonymousFunction)
        exitTargetsForReturn.remove(function.symbol)
        return exitGraph()
    }

    // ----------------------------------- Anonymous function -----------------------------------

    /** 进入匿名函数 CFG 子图，并连接 capture/postponed lambda 入口。 */
    @JvmOverloads
    fun enterAnonymousFunction(
        anonymousFunction: CfirAnonymousFunction,
        calledInPlace: Boolean = false,
    ): FunctionEnterNode {
        // 只消费调用方给出的执行合同，不由 callee 名称推测 lambda 是否内联执行。
        val graphKind = if (calledInPlace) ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace
        else ControlFlowGraph.Kind.AnonymousFunction
        return enterGraph(anonymousFunction, "<anonymous>", graphKind) {
            createFunctionEnterNode(it) to createFunctionExitNode(it).also { exit ->
                exitTargetsForReturn[anonymousFunction.symbol] = exit
            }
        }.also {
            val captureNode = anonymousFunctionCaptureNodes[anonymousFunction.symbol]
            if (captureNode != null) {
                addEdge(captureNode, it, preferredKind = EdgeKind.CfgForward, label = CapturedByValue)
            }
            val postponed = postponedAnonymousFunctionNodes[anonymousFunction.symbol]?.first
            if (postponed != null) addEdge(postponed, it)
        }
    }

    /** 退出匿名函数 CFG 子图，并连接 postponed lambda 出口。 */
    fun exitAnonymousFunction(anonymousFunction: CfirAnonymousFunction): Triple<FunctionExitNode, PostponedLambdaExitNode?, ControlFlowGraph> {
        exitTargetsForReturn.remove(anonymousFunction.symbol)
        val (exitNode, graph) = exitGraph<FunctionExitNode>()
        val postponed = postponedAnonymousFunctionNodes.remove(anonymousFunction.symbol)
        val postponedExit = postponed?.second
        if (postponedExit != null) {
            addEdge(exitNode, postponedExit)
        } else {
            addBackEdge(graph.exitNode, graph.enterNode)
        }
        anonymousFunctionCaptureNodes.remove(anonymousFunction.symbol)
        return Triple(exitNode, postponedExit, graph)
    }

    /** 进入匿名函数表达式，并按是否已有 postponed 入口创建表达式或 capture 节点。 */
    fun enterAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
    ): Pair<AnonymousFunctionExpressionNode?, AnonymousFunctionCaptureNode?> {
        val symbol = anonymousFunctionExpression.anonymousFunction.symbol
        val existing = postponedAnonymousFunctionNodes[symbol]?.first
        if (existing == null) {
            val expressionNode = createAnonymousFunctionExpressionNode(anonymousFunctionExpression).also {
                addNewSimpleNode(it)
                postponedAnonymousFunctionNodes[symbol] = it to null
            }
            return expressionNode to null
        }
        val captureNode = createAnonymousFunctionCaptureNode(anonymousFunctionExpression).also {
            addNewSimpleNode(it)
            anonymousFunctionCaptureNodes[symbol] = it
        }
        val exitNode = createPostponedLambdaExitNode(anonymousFunctionExpression)
        addEdge(existing, exitNode)
        postponedAnonymousFunctionNodes[symbol] = existing to exitNode
        postponedLambdaExits.topOrNull()?.exits?.add(exitNode to EdgeKind.Forward)
        return null to captureNode
    }

    // ----------------------------------- Field initializer (let x = ... / var x = ...) -----------------------------------

    /** 进入字段初始化器 CFG 子图。 */
    fun enterFieldInitializer(field: CfirFieldVariable): FieldInitializerEnterNode =
        enterGraph(field, field.name.asString(), ControlFlowGraph.Kind.FieldInitializer) {
            createFieldInitializerEnterNode(it) to createFieldInitializerExitNode(it)
        }

    /** 退出字段初始化器 CFG 子图。 */
    fun exitFieldInitializer(): Pair<FieldInitializerExitNode, ControlFlowGraph> = exitGraph()

    /** 建立真正 PRIVATE 根，继承父流而不接入父图的运行时 CFA 顺序。 */
    fun enterResourceInitializer(resource: CfirResourceVariable) {
        check(!resourceRoots.containsKey(resource)) { "A resource initializer root was allocated twice" }
        val incoming = lastNodeOrNull
        val enter = enterGraph(resource, "<resource-initializer>", ControlFlowGraph.Kind.ResourceInitializer) {
            createResourceInitializerEnterNode(it) to createResourceInitializerExitNode(it)
        }
        if (incoming != null) {
            addEdge(incoming, enter, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
            if (incoming.flowInitialized) enter.flow = incoming.flow
            incoming.alternateFlowPaths.forEach { path ->
                enter.addAlternateFlow(path, checkNotNull(incoming.getAlternateFlow(path)))
            }
        }
        resourceRoots[resource] = ResourceRoot(currentGraph, enter, notCompletedFunctionCalls.top(), ResourceRootPhase.Active)
    }

    /** 暂停实际根，保留其 cursor/未完成调用；祖先 snapshot 不被写入或清空。 */
    fun suspendResourceInitializer(resource: CfirResourceVariable) {
        val root = checkNotNull(resourceRoots[resource])
        check(root.phase == ResourceRootPhase.Active && currentGraph === root.graph)
        root.lastNode = lastNodes.pop()
        root.pendingCalls = notCompletedFunctionCalls.pop()
        check(graphs.pop() === root.graph)
        root.phase = ResourceRootPhase.Suspended
    }

    /** 首轮及晚 PRIVATE producer/writer 始终在这个确切资源根中执行。 */
    fun <T> withResourceInitializer(resource: CfirResourceVariable, action: () -> T): T {
        val root = checkNotNull(resourceRoots[resource]) { "PRIVATE work has no resource initializer root" }
        check(root.graph.declaration === resource && root.graph in ownedGraphs)
        return when (root.phase) {
            ResourceRootPhase.Active -> {
                check(graphs.all().firstOrNull { it.kind == ControlFlowGraph.Kind.ResourceInitializer } === root.graph) {
                    "PRIVATE work belongs to another active resource initializer root"
                }
                action()
            }
            ResourceRootPhase.Suspended -> {
                graphs.push(root.graph)
                lastNodes.push(root.lastNode)
                notCompletedFunctionCalls.push(root.pendingCalls)
                root.phase = ResourceRootPhase.Active
                try { action() } finally { suspendResourceInitializer(resource) }
            }
            ResourceRootPhase.Finished -> error("PRIVATE producer executed after its resource root was closed")
        }
    }

    /** 全部实际 PRIVATE 任务完成后才关闭根；关闭不等于 Source 退休。 */
    fun finishResourceInitializer(resource: CfirResourceVariable): ControlFlowGraph {
        val root = checkNotNull(resourceRoots[resource])
        check(root.graph.declaration === resource && root.graph in ownedGraphs)
        if (root.phase == ResourceRootPhase.Finished) return root.graph
        check(root.phase == ResourceRootPhase.Suspended)
        graphs.push(root.graph)
        lastNodes.push(root.lastNode)
        notCompletedFunctionCalls.push(root.pendingCalls)
        root.phase = ResourceRootPhase.Active
        val (_, graph) = exitGraph<ResourceInitializerExitNode>()
        check(graph === root.graph)
        root.phase = ResourceRootPhase.Finished
        return graph
    }

    // ----------------------------------- Value parameter / default argument -----------------------------------

    /** 进入 value parameter 节点。 */
    fun enterValueParameter(parameter: CfirValueParameter): EnterValueParameterNode =
        createEnterValueParameterNode(parameter).also { addNewSimpleNode(it) }

    /** 进入默认参数 CFG 子图。 */
    fun enterDefaultArguments(parameter: CfirValueParameter): EnterDefaultArgumentsNode =
        enterGraph(parameter, "<default>", ControlFlowGraph.Kind.DefaultArgument) {
            createEnterDefaultArgumentsNode(it) to createExitDefaultArgumentsNode(it)
        }

    /** 退出默认参数 CFG 子图。 */
    fun exitDefaultArguments(): Pair<ExitDefaultArgumentsNode, ControlFlowGraph> = exitGraph()

    /** 退出 value parameter 节点。 */
    fun exitValueParameter(parameter: CfirValueParameter): ExitValueParameterNode =
        createExitValueParameterNode(parameter).also { addNewSimpleNode(it) }

    // ----------------------------------- Code fragment -----------------------------------

    /** 进入代码片段 CFG 子图。 */
    fun enterCodeFragment(fragment: CfirCodeFragment): CodeFragmentEnterNode =
        enterGraph(fragment, "<code-fragment>", ControlFlowGraph.Kind.CodeFragment) {
            createCodeFragmentEnterNode(it) to createCodeFragmentExitNode(it)
        }

    /** 退出代码片段 CFG 子图。 */
    fun exitCodeFragment(): Pair<CodeFragmentExitNode, ControlFlowGraph> = exitGraph()

    // ----------------------------------- Return expressions -----------------------------------

    /**
     * 从函数 CFG 中提取返回类型推断使用的结果集合。
     *
     * 收集来源统一为：
     * - 直接连到函数 exit 的 block 尾表达式；
     * - 通过 non-direct jump 连到函数 exit 的显式 `return expr`。
     *
     * 这样既能覆盖“最后一条表达式即返回值”，也能覆盖显式 return。
     * 仓颉语义下，显式 return 后面的 block 尾表达式仍参与隐式返回类型推断，
     * 因而不能只按 CFG 可达性丢弃该尾表达式。
     */
    fun returnExpressionsOfFunction(function: CfirFunction): Collection<CfirExpression>? {
        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode ?: return null
        val result = linkedSetOf<CfirExpression>()

        fun CFGNode<*>.returnExpression(): CfirExpression? = when (this) {
            is BlockExitNode -> {
                if (previousNodes.all { it is StubNode }) {
                    null
                } else {
                    (fir.statements.lastOrNull() as? CfirExpression)?.takeUnless { it is CfirReturnExpression }
                }
            }
            is JumpNode -> (fir as? CfirReturnExpression)?.result
            else -> null
        }

        val reachable = ControlFlowGraphCompletionTraversal(exitNode.owner).returnPredecessors()
        // 保留既有实际 predecessor/nonDirectJumps 顺序，只过滤不能完成返回的路径。
        exitNode.previousNodes.filter { it in reachable }.mapNotNullTo(result) { it.returnExpression() }
        nonDirectJumps[exitNode].filter { it in reachable }.mapNotNullTo(result) { it.returnExpression() }
        return result
    }

    /** 对位 Kotlin 返回信息：直接从实际 CFG predecessor 保存 expression 与其 owning slot。 */
    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction):
        Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>? {
        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode ?: return null
        val result = linkedSetOf<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>()
        fun CFGNode<*>.returnInfo(): CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo? = when (this) {
            is BlockExitNode -> {
                val expression = (fir.statements.lastOrNull() as? CfirExpression)?.takeUnless { it is CfirReturnExpression }
                if (expression == null || previousNodes.all { it is StubNode }) null
                else CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo(expression, isExplicit = false, containingStatement = fir)
            }
            is JumpNode -> {
                val statement = (fir as? CfirReturnExpression)?.takeIf { it.target.labeledElement.symbol === function.symbol }
                statement?.let { CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo(it.result, isExplicit = true, containingStatement = it) }
            }
            else -> null
        }
        val reachable = ControlFlowGraphCompletionTraversal(exitNode.owner).returnPredecessors()
        exitNode.previousNodes.filter { it in reachable }.mapNotNullTo(result) { it.returnInfo() }
        nonDirectJumps[exitNode].filter { it in reachable }.mapNotNullTo(result) { it.returnInfo() }
        return result
    }

    // ----------------------------------- Block -----------------------------------

    /** 进入 block 节点。 */
    fun enterBlock(block: CfirBlock): BlockEnterNode =
        createBlockEnterNode(block).also { addNewSimpleNode(it) }

    /** 退出 block 节点。 */
    fun exitBlock(block: CfirBlock): BlockExitNode =
        createBlockExitNode(block).also { addNewSimpleNode(it) }

    // ----------------------------------- If -----------------------------------

    /** 对位 when 的分支生命周期；条件 false 输入不会被 then 的终止状态覆盖。 */
    fun enterIfExpression(expression: CfirIfExpression): IfEnterNode {
        val enter = createIfEnterNode(expression).also { addNewSimpleNode(it) }
        ifExitNodes.push(createIfExitNode(expression))
        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return enter
    }

    /** 条件只求值一次；保留一份节点作为 else 或无 else 的正常输入。 */
    fun exitIfCondition(expression: CfirIfExpression): Pair<IfConditionExitNode, IfBranchResultEnterNode> {
        check(ifExitNodes.top().fir === expression)
        val conditionExit = createIfConditionExitNode(expression).also { addNewSimpleNode(it) }
        lastNodes.push(conditionExit)
        val thenEnter = createIfBranchResultEnterNode(expression, isThen = true)
        addNewSimpleNode(thenEnter, isDead = expression.condition.ifBooleanLiteralValue() == false, label = IfBranchTrue)
        return conditionExit to thenEnter
    }

    /** else 从保存的 false 输入开始；不会从 then 的最后节点继续。 */
    fun enterIfElseBranch(expression: CfirIfExpression): IfBranchResultEnterNode {
        check(ifExitNodes.top().fir === expression && expression.elseBranch != null)
        val conditionExit = lastNodes.top() as IfConditionExitNode
        check(conditionExit.fir === expression)
        val elseEnter = createIfBranchResultEnterNode(expression, isThen = false)
        addNewSimpleNode(elseEnter, isDead = expression.condition.ifBooleanLiteralValue() == true, label = IfBranchFalse)
        return elseEnter
    }

    /** 一条分支独立汇入出口；先到达的 dead 输入不改变后续分支的资格。 */
    fun exitIfBranchResult(expression: CfirIfExpression, isThen: Boolean): IfBranchResultExitNode {
        val exit = ifExitNodes.top()
        check(exit.fir === expression)
        val branchExit = createIfBranchResultExitNode(expression, isThen)
        popAndAddEdge(branchExit)
        addEdge(branchExit, exit, propagateDeadness = false)
        return branchExit
    }

    /** 结束真实分支和 postponed 层后才汇总 deadness，完成状态由实际解析模式提供。 */
    fun exitIfExpression(expression: CfirIfExpression, callCompleted: Boolean): IfExitNode {
        val exit = ifExitNodes.pop()
        check(exit.fir === expression)
        if (expression.elseBranch == null) {
            val conditionExit = lastNodes.pop() as IfConditionExitNode
            check(conditionExit.fir === expression)
            val kind = if (expression.condition.ifBooleanLiteralValue() == true) EdgeKind.DeadForward else EdgeKind.Forward
            addEdge(conditionExit, exit, preferredKind = kind, label = IfBranchFalse, propagateDeadness = false)
        }
        // late Nothing 只能在全部 alternative 输入接齐后传播，不能先把汇合出口判死。
        notCompletedFunctionCalls.pop().forEach { completeFunctionCall(it) }
        mergeDataFlowFromPostponedLambdas(exit, callCompleted)
        exit.updateDeadStatus()
        lastNodes.push(exit)
        return exit
    }

    /** 只有真实 Bool 字面量可以在建图时确定条件；其它条件留给正式 CFA。 */
    private fun CfirExpression.ifBooleanLiteralValue(): Boolean? =
        (this as? CfirLiteralExpression)?.takeIf { it.kind == CfirLiteralKind.BOOLEAN }?.value as? Boolean

    // ----------------------------------- Match -----------------------------------

    /** 进入 match 表达式并准备分支出口与 postponed lambda 数据流。 */
    fun enterMatchExpression(matchExpression: CfirMatchExpression): MatchEnterNode {
        val node = createMatchEnterNode(matchExpression)
        addNewSimpleNode(node)
        matchExitNodes.push(createMatchExitNode(matchExpression))
        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return node
    }

    /** 进入 match 分支条件；前一分支的失败汇合节点会顺序连接到这里。 */
    fun enterMatchBranchCondition(branch: CfirMatchBranch): MatchBranchConditionEnterNode =
        createMatchBranchConditionEnterNode(branch, matchExitNodes.top().fir).also(::addNewSimpleNode)

    /**
     * 退出 match 分支条件并把完整模式下沉为原子判定 CFG。
     *
     * CHIR 会把一个嵌套模式翻译成 tag / payload / 常量判定的控制流图；若继续只保留一条
     * case condition，后端常量分析无法区分被拒绝的 payload successor 与整个分支。这里的
     * [MatchPatternDecisionNode] 保留该结构，而 [MatchBranchFailureNode] 统一承接每个原子
     * 判定的 failure 边，供下一 case 或 synthetic else 连接。
     */
    fun exitMatchBranchCondition(
        branch: CfirMatchBranch,
        resolveGuard: () -> Unit,
    ): Pair<MatchBranchConditionExitNode, MatchBranchResultEnterNode> {
        val matchExpression = matchExitNodes.top().fir
        val conditionEnter = lastNodes.pop()
        val conditionExit = createMatchBranchConditionExitNode(branch, matchExpression)
        val failureNode = createMatchBranchFailureNode(branch, matchExpression)
        val guardEnter = branch.guard?.let { createMatchBranchGuardEnterNode(branch, matchExpression) }
        val decisionEntry = createPatternDecisionGraph(
            branch = branch,
            pattern = branch.pattern,
            subjectPath = emptyList(),
            reportSource = branch.pattern,
            successTarget = guardEnter ?: conditionExit,
            failureTarget = failureNode,
            matchExpression = matchExpression,
        )
        addEdge(conditionEnter, decisionEntry)
        completePatternDecisionGraph(decisionEntry, branch)
        // success 入口的全部 pattern 入边已经齐全；failure 还要等待 guard 的另一条入边。
        (guardEnter ?: conditionExit).updateDeadStatus()

        lastNodes.push(failureNode)
        if (guardEnter != null) {
            lastNodes.push(guardEnter)
            // guard 的普通表达式节点只能挂在模式成功路径，不能提前产生赋值/调用效应。
            resolveGuard()
            val guard = checkNotNull(branch.guard)
            val guardDecision = createMatchGuardDecisionNode(
                branch, guard, branch.pattern.takeUnless { it is CfirOrPattern }, matchExpression,
            )
            addEdge(lastNodes.pop(), guardDecision)
            addEdge(guardDecision, conditionExit, propagateDeadness = false, label = MatchBranchSuccess)
            addEdge(guardDecision, failureNode, propagateDeadness = false, label = MatchBranchFailure)
        }

        conditionExit.updateDeadStatus()
        failureNode.updateDeadStatus()
        val resultEnter = createMatchBranchResultEnterNode(branch)
        addEdge(conditionExit, resultEnter, label = MatchBranchSuccess)
        // 分支体解析期间 resultEnter 必须是当前 last node；退出分支体后留下 failureNode，
        // 后续 case 会从该汇合点继续构图。
        lastNodes.push(resultEnter)
        return conditionExit to resultEnter
    }

    /**
     * 原子模式图按 continuation 反向建造，共享 success/failure 入边只能在全图闭合后合并。
     *
     * 对位 Kotlin CFG 的多入边节点：先用 propagateDeadness=false 收集全部输入，再按
     * updateDeadStatus/killEdge 传播。这里只更新本次实际 pattern decision 节点；guard 和
     * failure 等开放汇合点由 caller 在各自输入齐全后更新，不能让一个绑定的死 failure
     * 提前污染外层 enum tag 或 guard 的真实失败边。
     */
    private fun completePatternDecisionGraph(entry: CFGNode<*>, branch: CfirMatchBranch) {
        val decisions = linkedSetOf<MatchPatternDecisionNode>()
        val traversal = ArrayDeque<CFGNode<*>>()
        traversal.addLast(entry)
        while (traversal.isNotEmpty()) {
            val node = traversal.removeFirst() as? MatchPatternDecisionNode ?: continue
            if (node.branch !== branch || !decisions.add(node)) continue
            traversal.addAll(node.followingNodes)
        }

        val pending = ArrayDeque<MatchPatternDecisionNode>()
        pending.addAll(decisions)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            node.updateDeadStatus()
            if (!node.isDead) continue
            for (next in node.followingNodes) {
                if (CFGNode.killEdge(node, next, propagateDeadness = false) && next in decisions) {
                    pending.addLast(next as MatchPatternDecisionNode)
                }
            }
        }
    }

    /**
     * 为一个模式递归建立判定图，返回其独立入口节点。不失败的绑定/通配模式也保留入口，
     * 并用死亡 failure 边连接后续 alternative；因此任意 OR 顺序和嵌套层次都保有完整的
     * 图结构。所有原子节点保留外层 [reportSource]，对应 CHIR 的模式 debug location。
     */
    private fun createPatternDecisionGraph(
        branch: CfirMatchBranch,
        pattern: CfirPattern,
        subjectPath: List<Int>,
        reportSource: CfirPattern,
        successTarget: CFGNode<*>,
        failureTarget: CFGNode<*>,
        matchExpression: CfirMatchExpression,
    ): CFGNode<*> = when (pattern) {
        is CfirWildcardPattern, is CfirBindingPattern -> {
            val nestedPattern = (pattern as? CfirBindingPattern)?.nestedPattern
            if (nestedPattern != null) {
                createPatternDecisionGraph(
                    branch,
                    nestedPattern,
                    subjectPath,
                    reportSource,
                    successTarget,
                    failureTarget,
                    matchExpression,
                )
            } else {
                createMatchPatternDecisionNode(branch, pattern, subjectPath, reportSource, matchExpression).also { decision ->
                    addEdge(decision, successTarget, propagateDeadness = false, label = MatchBranchSuccess)
                    addEdge(decision, failureTarget, preferredKind = EdgeKind.DeadForward,
                        propagateDeadness = false, label = MatchBranchFailure)
                }
            }
        }

        is CfirTuplePattern -> {
            var continuation = successTarget
            for (index in pattern.elements.indices.reversed()) {
                continuation = createPatternDecisionGraph(
                    branch,
                    pattern.elements[index],
                    subjectPath + index,
                    reportSource,
                    continuation,
                    failureTarget,
                    matchExpression,
                )
            }
            continuation
        }

        is CfirEnumPattern -> {
            var continuation = successTarget
            for (index in pattern.arguments.indices.reversed()) {
                continuation = createPatternDecisionGraph(
                    branch,
                    pattern.arguments[index],
                    subjectPath + index,
                    reportSource,
                    continuation,
                    failureTarget,
                    matchExpression,
                )
            }
            createMatchPatternDecisionNode(branch, pattern, subjectPath, reportSource, matchExpression).also { decision ->
                addEdge(decision, continuation, propagateDeadness = false, label = MatchBranchSuccess)
                addEdge(decision, failureTarget, propagateDeadness = false, label = MatchBranchFailure)
            }
        }

        is CfirOrPattern -> {
            val loweringKind = pattern.loweringKind()
            if (loweringKind == CfirOrPatternLoweringKind.SEQUENTIAL) {
                var continuation = failureTarget
                for (alternative in pattern.alternatives.asReversed()) {
                    continuation = createPatternDecisionGraph(
                        branch, alternative, subjectPath, alternative, successTarget, continuation, matchExpression,
                    )
                }
                continuation
            } else {
                // 官方 MultiBranch/GoTo 共用无模式位置的成功块，不产生逐 alternative 的 CFA 警告。
                createMatchPatternDecisionNode(branch, pattern, subjectPath, null, matchExpression).also { decision ->
                    addEdge(decision, successTarget, propagateDeadness = false, label = MatchBranchSuccess)
                    addEdge(
                        decision, failureTarget,
                        preferredKind = if (loweringKind == CfirOrPatternLoweringKind.UNCONDITIONAL) EdgeKind.DeadForward else EdgeKind.Forward,
                        propagateDeadness = false,
                        label = MatchBranchFailure,
                    )
                }
            }
        }

        else -> createMatchPatternDecisionNode(branch, pattern, subjectPath, reportSource, matchExpression).also { decision ->
            addEdge(decision, successTarget, propagateDeadness = false, label = MatchBranchSuccess)
            addEdge(
                decision, failureTarget,
                preferredKind = if (decision.isAlwaysSuccessful) EdgeKind.DeadForward else EdgeKind.Forward,
                propagateDeadness = false,
                label = MatchBranchFailure,
            )
        }
    }

    /** 退出 match 分支结果并连接到当前 match 出口。 */
    fun exitMatchBranchResult(branch: CfirMatchBranch): MatchBranchResultExitNode {
        val resultExit = createMatchBranchResultExitNode(branch)
        popAndAddEdge(resultExit)
        addEdge(resultExit, matchExitNodes.top(), propagateDeadness = false)
        return resultExit
    }

    /** 退出 match 表达式，并按穷尽性决策补 synthetic else 分支。 */
    fun exitMatchExpression(
        matchExpression: CfirMatchExpression,
        syntheticElseDecision: MatchSyntheticElseDecision,
        callCompleted: Boolean,
    ): Pair<MatchExitNode, MatchSyntheticElseBranchNode?> {
        notCompletedFunctionCalls.pop().forEach(::completeFunctionCall)
        val exitNode = matchExitNodes.pop()
        val lastConditionExit = lastNodes.pop()
        val syntheticElse = if (syntheticElseDecision == MatchSyntheticElseDecision.Required) {
            createMatchSyntheticElseBranchNode(matchExpression).also {
                addEdge(lastConditionExit, it, label = MatchBranchFailure)
                addEdge(it, exitNode, propagateDeadness = false)
            }
        } else {
            // 官方 TranslateMatchWithSelector 将穷尽 match 的最后失败块标为 UNREACHABLE。
            // 它不能把未写入结果的状态带到出口，也不能参与后续确定赋值/常量事实的汇合。
            addEdge(
                lastConditionExit, exitNode,
                preferredKind = EdgeKind.DeadForward,
                propagateDeadness = false,
                label = MatchBranchFailure,
            )
            null
        }
        mergeDataFlowFromPostponedLambdas(exitNode, callCompleted)
        exitNode.updateDeadStatus()
        lastNodes.push(exitNode)
        return exitNode to syntheticElse
    }

    // ----------------------------------- Loop -----------------------------------

    /** 进入 while/for-in 形态循环，并创建条件入口。 */
    fun enterWhileLoop(loop: CfirLoopExpression): Pair<LoopEnterNode, LoopConditionEnterNode> {
        val loopEnter = createLoopEnterNode(loop).also { addNewSimpleNode(it) }
        loopExitNodes[loop] = createLoopExitNode(loop)
        val conditionEnter = createLoopConditionEnterNode(loop.condition, loop).also { addNewSimpleNode(it) }
        loopConditionEnterNodes[loop] = conditionEnter
        return loopEnter to conditionEnter
    }

    /** 退出 while/for-in 条件，并进入循环体。 */
    fun exitWhileLoopCondition(loop: CfirLoopExpression): Pair<LoopConditionExitNode, LoopBlockEnterNode> {
        val conditionExit = createLoopConditionExitNode(loop.condition, loop).also { addNewSimpleNode(it) }
        addEdge(conditionExit, loopExitNodes.getValue(loop), propagateDeadness = false)
        val blockEnter = createLoopBlockEnterNode(loop).also { addNewSimpleNode(it) }
        return conditionExit to blockEnter
    }

    /** 退出 while/for-in 循环体并连接回条件入口。 */
    fun exitWhileLoop(loop: CfirLoopExpression): Triple<LoopConditionEnterNode, LoopBlockExitNode, LoopExitNode> {
        val blockExit = createLoopBlockExitNode(loop)
        popAndAddEdge(blockExit)
        val conditionEnter = loopConditionEnterNodes.remove(loop) ?: error("Missing while-loop condition entry")
        addBackEdge(blockExit, conditionEnter)
        val loopExit = loopExitNodes.remove(loop) ?: error("Missing while-loop exit")
        loopExit.updateDeadStatus()
        lastNodes.push(loopExit)
        return Triple(conditionEnter, blockExit, loopExit)
    }

    /** 进入 do-while 循环，并先进入循环体。 */
    fun enterDoWhileLoop(loop: CfirLoopExpression): Pair<LoopEnterNode, LoopBlockEnterNode> {
        val loopEnter = createLoopEnterNode(loop).also { addNewSimpleNode(it) }
        loopExitNodes[loop] = createLoopExitNode(loop)
        val blockEnter = createLoopBlockEnterNode(loop).also { addNewSimpleNode(it) }
        lastNodes.push(blockEnter)
        loopConditionEnterNodes[loop] = createLoopConditionEnterNode(loop.condition, loop)
        return loopEnter to blockEnter
    }

    /** 退出 do-while 循环体并进入条件。 */
    fun enterDoWhileLoopCondition(loop: CfirLoopExpression): Pair<LoopBlockExitNode, LoopConditionEnterNode> {
        val blockExit = createLoopBlockExitNode(loop).also { addNewSimpleNode(it) }
        val conditionEnter = loopConditionEnterNodes[loop] ?: error("Missing do-while condition entry")
        addNewSimpleNode(conditionEnter)
        conditionEnter.updateDeadStatus()
        return blockExit to conditionEnter
    }

    /** 退出 do-while 循环并连接条件回边和循环出口。 */
    fun exitDoWhileLoop(loop: CfirLoopExpression): Pair<LoopConditionExitNode, LoopExitNode> {
        loopConditionEnterNodes.remove(loop)
        val conditionExit = createLoopConditionExitNode(loop.condition, loop)
        popAndAddEdge(conditionExit)
        val blockEnter = lastNodes.pop()
        require(blockEnter is LoopBlockEnterNode) { "Expected loop block entry before exiting do-while" }
        addBackEdge(conditionExit, blockEnter)
        val loopExit = loopExitNodes.remove(loop) ?: error("Missing do-while exit")
        addEdge(conditionExit, loopExit, propagateDeadness = false)
        loopExit.updateDeadStatus()
        lastNodes.push(loopExit)
        return conditionExit to loopExit
    }

    // ----------------------------------- Jump / throw -----------------------------------

    /** 进入 jump 表达式，必要时拆分匿名函数 postponed 数据流。 */
    fun enterJump(jump: CfirJump<*>) {
        if (jump is CfirReturnExpression && jump.target.labeledElement is CfirAnonymousFunction) {
            splitDataFlowForPostponedLambdas()
        }
    }

/**
     * 退出 jump 表达式，并连接 return/break/continue 的非直接跳转边。
     *
     * 带错误类型（如 `JumpOutsideLoop`）的 break/continue 没有合法目标，其 target
     * 在 resolve 阶段不会绑定，这里跳过目标查找直接返回，等价于旧的错误 loop 行为。
     */
    fun exitJump(jump: CfirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        addNonSuccessfullyTerminatingNode(node)

        if (jump is CfirReturnExpression && jump.target.labeledElement is CfirAnonymousFunction) {
            jumpDataFlowFromPostponedLambdas(jump.target.labeledElement.symbol)
        }

        val nextNode = when {
            jump.coneTypeOrNull is ConeErrorType -> null
            jump is CfirReturnExpression -> exitTargetsForReturn[jump.target.labeledElement.symbol]
            jump is CfirContinueExpression -> loopConditionEnterNodes[jump.target.labeledElement]
            jump is CfirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> null
        } ?: return node

        routeJump(node, node, nextNode, activeExceptionScopes(), exceptionBoundary())

        return node
    }

    /** throw 终止普通顺序流，并从真实抛出点分派异常。 */
    fun exitThrowExceptionNode(throwExpression: CfirThrowExpression): ThrowExceptionNode =
        createThrowExceptionNode(throwExpression).also {
            addNonSuccessfullyTerminatingNode(it)
            routeException(it, it, activeExceptionScopes(), exceptionBoundary())
        }

    // ----------------------------------- Try / catch / finally -----------------------------------

    /** 进入 try 表达式，并预创建 catch/handle/finally 入口。 */
    fun enterTryExpression(tryExpression: CfirTryExpression): Pair<TryExpressionEnterNode, TryMainBlockEnterNode> {
        val tryEnter = createTryExpressionEnterNode(tryExpression).also { addNewSimpleNode(it) }
        tryExitNodes.push(createTryExpressionExitNode(tryExpression))

        val tryMainEnter = createTryMainBlockEnterNode(tryExpression).also { addNewSimpleNode(it) }
        catchNodes.push(tryExpression.catches.map(::createCatchClauseEnterNode))
        handleNodes.push(tryExpression.handlers.map(::createHandleClauseEnterNode))
        if (tryExpression.finallyBlock != null) {
            finallyEnterNodes.push(createFinallyBlockEnterNode(tryExpression))
        }

        for (catchEnter in catchNodes.top()) {
            addEdge(tryEnter, catchEnter, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
        }
        for (handleEnter in handleNodes.top()) {
            addEdge(tryEnter, handleEnter, propagateDeadness = false)
        }
        finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExpression }?.let {
            addEdge(tryEnter, it, preferredKind = EdgeKind.DfgForward, propagateDeadness = false, label = UncaughtExceptionPath)
        }

        exceptionScopes.push(ExceptionScope(ExceptionScopeState(
            tryExitNodes.top(), catchNodes.top(),
            finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExpression }, ExceptionPhase.Main, emptyList(),
        )))
        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return tryEnter to tryMainEnter
    }

    /** 退出 try 主体，并把控制流连接到 catch/handle/finally 或 try exit。 */
    fun exitTryMainBlock(): TryMainBlockExitNode {
        val tryExitNode = tryExitNodes.top()
        val node = createTryMainBlockExitNode(tryExitNode.fir)
        popAndAddEdge(node)
        val nextNode = finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNode.fir } ?: tryExitNode
        val scopes = activeExceptionScopes()
        val cleaned = cleanupNormal(node, scopes.first(), scopes.drop(1), exceptionBoundary())
        addEdge(cleaned, nextNode, propagateDeadness = false)
        exceptionScopes.top().state = exceptionScopes.top().state.copy(phase = ExceptionPhase.Catch, resources = emptyList())
        for (catchEnter in catchNodes.pop().asReversed()) {
            catchBlocksInProgress.push(catchEnter)
            addEdge(node, catchEnter, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
        }
        for (handleEnter in handleNodes.pop().asReversed()) {
            handleBlocksInProgress.push(handleEnter)
            addEdge(node, handleEnter, propagateDeadness = false)
        }
        return node
    }

    /** 进入 catch 子句。 */
    fun enterCatchClause(catch: CfirCatch): CatchClauseEnterNode {
        val catchEnter = catchBlocksInProgress.pop()
        require(catchEnter.fir === catch) { "Catch stack out of sync" }
        catchEnter.updateDeadStatus()
        lastNodes.push(catchEnter)
        return catchEnter
    }

    /** 退出 catch 子句并连接到 finally 或 try exit。 */
    fun exitCatchClause(catch: CfirCatch): CatchClauseExitNode {
        val tryExitNode = tryExitNodes.top()
        val catchExit = createCatchClauseExitNode(catch)
        popAndAddEdge(catchExit)
        val nextNode = finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNode.fir } ?: tryExitNode
        addEdge(catchExit, nextNode, propagateDeadness = false)
        return catchExit
    }

    /** 进入 effect handle 子句。 */
    fun enterHandleClause(handleClause: CfirHandleClause): HandleClauseEnterNode {
        val handleEnter = handleBlocksInProgress.pop()
        require(handleEnter.fir === handleClause) { "Handle stack out of sync" }
        finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNodes.top().fir }?.let {
            addEdge(handleEnter, it, propagateDeadness = false, label = UncaughtExceptionPath)
        }
        lastNodes.push(handleEnter)
        return handleEnter
    }

    /** 退出 effect handle 子句并连接到 finally 或 try exit。 */
    fun exitHandleClause(handleClause: CfirHandleClause): HandleClauseExitNode {
        val tryExitNode = tryExitNodes.top()
        val handleExit = createHandleClauseExitNode(handleClause)
        popAndAddEdge(handleExit)
        val nextNode = finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNode.fir } ?: tryExitNode
        addEdge(handleExit, nextNode, propagateDeadness = false)
        return handleExit
    }

    /** 进入 finally block。 */
    fun enterFinallyBlock(): FinallyBlockEnterNode {
        val node = finallyEnterNodes.pop()
        node.updateDeadStatus()
        exceptionScopes.top().state = exceptionScopes.top().state.copy(phase = ExceptionPhase.Finally)
        lastNodes.push(node)
        finallyBlocksInProgress.push(node)
        finallyBlocksInProgressSet.add(node.fir)
        return node
    }

    /** 退出 finally block，并重连 return/break/continue 等穿越 finally 的边。 */
    fun exitFinallyBlock(): FinallyBlockExitNode {
        val enterNode = finallyBlocksInProgress.top()
        val tryExitNode = tryExitNodes.top()
        val exitNode = createFinallyBlockExitNode(enterNode)
        popAndAddEdge(exitNode)
        addEdge(exitNode, tryExitNode, propagateDeadness = false, preferredKind = if (enterNode.allNormalInputsAreDead) EdgeKind.DeadForward else EdgeKind.Forward)

        for (continuation in exceptionContinuations.remove(enterNode).orEmpty()) {
            val inputIsDead = enterNode.previousNodes.none {
                val edge = it.edgeTo(enterNode)
                edge.label === continuation.transfer && !edge.kind.isDead && edge.kind.usedInCfa
            }
            addEdge(exitNode, continuation.transfer, propagateDeadness = false,
                preferredKind = if (inputIsDead) EdgeKind.DeadForward else EdgeKind.Forward, label = continuation.transfer)
            continuation.transfer.updateDeadStatus()
            routeException(continuation.transfer, continuation.transfer.sourceNode, continuation.scopes, continuation.boundary)
        }
        for (continuation in jumpContinuations.remove(enterNode).orEmpty().distinctBy { it.target }) {
            routeJump(exitNode, continuation.jump, continuation.target, continuation.scopes, continuation.boundary)
        }
        return exitNode
    }

    /** 退出 try 表达式，并合并 postponed lambda 数据流。 */
    fun exitTryExpression(callCompleted: Boolean): TryExpressionExitNode {
        var haveNothingReturnCall = false
        notCompletedFunctionCalls.pop().forEach { haveNothingReturnCall = completeFunctionCall(it) || haveNothingReturnCall }
        val node = tryExitNodes.pop()
        val scope = exceptionScopes.pop().state
        check(scope.tryExit === node) { "Exception scope stack out of sync" }
        scope.catches.forEach { exceptionDispatches.remove(it) }
        if (node.fir.finallyBlock != null) {
            val enterFinallyNode = finallyBlocksInProgress.pop()
            finallyBlocksInProgressSet.remove(enterFinallyNode.fir)
            if (haveNothingReturnCall && enterFinallyNode.allNormalInputsAreDead) {
                val exitFinallyNode = node.previousNodes.singleOrNull() as? FinallyBlockExitNode
                if (exitFinallyNode != null) {
                    CFGNode.removeAllIncomingEdges(node)
                    addEdge(exitFinallyNode, node, preferredKind = EdgeKind.DeadForward, propagateDeadness = false)
                }
            }
        }
        mergeDataFlowFromPostponedLambdas(node, callCompleted)
        node.updateDeadStatus()
        lastNodes.push(node)
        return node
    }

    // ----------------------------------- Call / postponed lambda topology -----------------------------------

    /** 由六个实际 node factory 登记，保存未接入图的 exit 和完成图的真实节点。 */
    @CfgInternals
    fun registerFunctionCallNode(node: CFGNode<*>) {
        if (node !is RevisableFunctionCallNode || node.fir !is CfirFunctionCall) return
        check(currentGraphOrNull === node.owner && node.owner in ownedGraphs) {
            "Call CFG node must be registered by its actual graph factory"
        }
        val call = node.fir as CfirFunctionCall
        val nodes = functionCallNodes.getOrPut(call) { mutableListOf() }
        if (nodes.none { it === node }) nodes += node
        node.owner.registerFunctionCallNode(node)
    }

    /** active owner 优先使用其正在构建的真实图；完成 owner 由已发布 reference 给出。 */
    fun graphInProgress(owner: CfirControlFlowGraphOwner): ControlFlowGraph? =
        graphs.all().firstOrNull { it.declaration === owner }

    @CfgInternals
    fun validateControlFlowGraphAdoption(graphs: Collection<Pair<CfirControlFlowGraphOwner, ControlFlowGraph>>) {
        graphs.forEach { (owner, graph) ->
            check(graph.declaration == null || graph.declaration === owner) {
                "Published CFG differs from its actual AST declaration owner"
            }
        }
    }

    /** selected AST/cache 完整恢复后登记真实 read dependency；不登记 probe 中仍共享的父引用。 */
    @CfgInternals
    fun adoptControlFlowGraphs(graphs: Collection<Pair<CfirControlFlowGraphOwner, ControlFlowGraph>>) {
        validateControlFlowGraphAdoption(graphs)
        graphs.forEach { (owner, graph) ->
            check(owner.controlFlowGraphReference?.controlFlowGraph === graph) {
                "CFG adoption must follow publication to its actual AST owner"
            }
        }
        ownedGraphs.addAll(graphs.map { it.second })
    }

    /** 正式图必须由实际 context 创建/采用；隔离 probe 的父图读取不产生 fir 写回。 */
    @CfgInternals
    fun updateFunctionCallNodes(original: CfirFunctionCall, replacement: CfirFunctionCall, graph: ControlFlowGraph) {
        if (original === replacement) return
        if (graph !in ownedGraphs) {
            check(isIsolatedContext) { "Formal call CFG has not been adopted by the actual context" }
            return
        }
        graph.updateFunctionCallNodes(original, replacement)
    }

    /** 仅观察当前实际拥有的图索引，不按 source/name/offset 找节点。 */
    fun describeFunctionCallNodes(call: CfirFunctionCall): String =
        ownedGraphs.map { it.describeFunctionCallNodes(call) }.filter { it.isNotEmpty() }.joinToString()

    /** 进入调用参数列表，并为参数中的匿名函数准备 postponed lambda 拓扑。 */
    fun enterCallArguments(call: CfirCall, anonymousFunctions: List<CfirAnonymousFunction>): FunctionCallArgumentsEnterNode? {
        val lambdaSymbols = anonymousFunctions.mapTo(linkedSetOf()) { it.symbol }
        postponedLambdaExits.push(PostponedLambdas(lambdaSymbols))

        val splitNode = if (anonymousFunctions.isEmpty()) {
            null
        } else {
            createSplitPostponedLambdasNode(call, anonymousFunctions).also { split ->
                anonymousFunctions.associateTo(postponedAnonymousFunctionNodes) { it.symbol to (split to null) }
            }
        }
        argumentListSplitNodes.push(splitNode)

        if (call !is CfirFunctionCall) {
            exitFunctionCallArgumentsNodes.push(null)
            return null
        }

        val enterNode = createFunctionCallArgumentsEnterNode(call)
        val exitNode = createFunctionCallArgumentsExitNode(call, explicitReceiverExitNode = enterNode)
        exitFunctionCallArgumentsNodes.push(exitNode)
        addNewSimpleNode(enterNode)
        return enterNode
    }

    /** 退出调用参数列表，并补接 postponed lambda split 和调用参数出口节点。 */
    fun exitCallArguments(): Pair<SplitPostponedLambdasNode?, FunctionCallArgumentsExitNode?> {
        val splitNode = argumentListSplitNodes.pop()?.also { addNewSimpleNode(it) }
        val exitNode = exitFunctionCallArgumentsNodes.pop()?.also { addNewSimpleNode(it) }
        return splitNode to exitNode
    }

    /** 记录函数调用显式接收者当前出口节点。 */
    fun exitCallExplicitReceiver() {
        exitFunctionCallArgumentsNodes.topOrNull()?.explicitReceiverExitNode = lastNode
    }

    /** 进入函数调用 callee 节点。 */
    fun enterFunctionCall(functionCall: CfirFunctionCall): FunctionCallEnterNode =
        createFunctionCallEnterNode(functionCall).also { addNewSimpleNode(it) }

    /** 退出函数调用，并根据调用完成状态处理 postponed lambda 数据流。 */
    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean): FunctionCallExitNode {
        val exitNode = createFunctionCallExitNode(functionCall)
        unifyDataFlowFromPostponedLambdas(exitNode, callCompleted)
        addNewSimpleNode(exitNode)
        // 调用的异常属于调用点；callee/lambda 定义图不连到定义处的 catch。
        routeException(exitNode, exitNode, activeExceptionScopes(), exceptionBoundary())
        if (callCompleted && functionCall.coneTypeOrNull?.isNothing == true) {
            val stub = createStubNode()
            addEdge(exitNode, stub)
            check(lastNodes.pop() === exitNode)
            lastNodes.push(stub)
        }
        if (!callCompleted) {
            notCompletedFunctionCalls.topOrNull()?.add(exitNode)
        }
        return exitNode
    }

    /**
     * 进入局部变量声明。
     *
     * initializer 的 CFG 节点必须位于 declaration enter/exit 之间，保证后续 CFA 在
     * declaration exit 后才能把已求值的 initializer 作为该变量的流事实写入状态。
     */
    fun enterVariableDeclaration(variable: CfirVariable): VariableDeclarationEnterNode =
        createVariableDeclarationEnterNode(variable).also { addNewSimpleNode(it) }

    /**
     * 退出局部变量声明。
     *
     * 此节点是所有变量流分析（初始化、常量传播、captured write）的统一事实写入点，
     * 不能由后续 match checker 从 tree 回溯替代。
     */
    fun exitVariableDeclaration(variable: CfirVariable): VariableDeclarationExitNode =
        createVariableDeclarationExitNode(variable).also {
            addNewSimpleNode(it)
            if (variable is CfirResourceVariable) {
                val scope = exceptionScopes.topOrNull()?.takeIf {
                    it.state.tryExit.owner === currentGraph && it.state.phase == ExceptionPhase.Main
                }
                if (scope != null) {
                    check(scope.state.tryExit.fir.resources[scope.state.resources.size] === variable) {
                        "Resource completion differs from the formal declaration order"
                    }
                    scope.state = scope.state.copy(resources = scope.state.resources + variable)
                }
            }
        }

    // ----------------------------------- Simple terminal nodes -----------------------------------

    /** 退出限定访问表达式并创建终端节点。 */
    fun exitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression): QualifiedAccessNode =
        createQualifiedAccessNode(qualifiedAccessExpression).also { addNewSimpleNode(it) }

    /** 退出字面量表达式并创建终端节点。 */
    fun exitLiteralExpression(literalExpression: CfirLiteralExpression): LiteralExpressionNode =
        createLiteralExpressionNode(literalExpression).also { addNewSimpleNode(it) }

    /** 退出变量赋值表达式，并补接可能抛异常的 catch/finally 边。 */
    fun exitVariableAssignment(assignment: CfirAssignment): VariableAssignmentNode =
        createVariableAssignmentNode(assignment).also { node ->
            addNewSimpleNode(node)

            val nextCatchNodes = catchNodes.topOrNull()
            if (!nextCatchNodes.isNullOrEmpty()) {
                for (catchEnter in nextCatchNodes) {
                    addEdge(node, catchEnter, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
                }
            }
            finallyEnterNodes.topOrNull()?.let { finallyEnter ->
                addEdge(node, finallyEnter, preferredKind = EdgeKind.DfgForward, propagateDeadness = false, label = UncaughtExceptionPath)
            }
        }

    /**
     * 退出自增/自减表达式并记录其对左值的写入。
     *
     * 该语法节点不经过 [CfirAssignment]，但和赋值一样必须占据一个稳定的 CFG 程序点；
     * 否则任何以局部状态为输入的后续分析都会错误保留旧事实。
     */
    fun exitIncrementDecrementExpression(expression: CfirIncrementDecrementExpression): IncrementDecrementNode =
        createIncrementDecrementNode(expression).also(::addNewSimpleNode)

    /** 进入可选链表达式。 */
    fun enterOptionalChain(optionalChainExpression: CfirOptionalChainExpression): EnterOptionalChainNode =
        createEnterOptionalChainNode(optionalChainExpression).also { addNewSimpleNode(it) }

    /** 退出可选链表达式。 */
    fun exitOptionalChain(optionalChainExpression: CfirOptionalChainExpression): ExitOptionalChainNode =
        createExitOptionalChainNode(optionalChainExpression).also { addNewSimpleNode(it) }

    /** 退出 wrapped expression。 */
    fun exitWrappedExpression(wrappedExpression: CfirWrappedExpression): WrappedExpressionNode =
        createWrappedExpressionNode(wrappedExpression).also { addNewSimpleNode(it) }

    // ----------------------------------- Helpers -----------------------------------

    /** 收集声明列表中已经构建的子 CFG。 */
    private fun collectSubGraphs(declarations: List<CfirDeclaration>): List<ControlFlowGraph> {
        return declarations
            .filterIsInstance<CfirControlFlowGraphOwner>()
            .mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }
    }

    // ----------------------------------- Postponed lambda data-flow -----------------------------------

    /** 开启一层 postponed lambda 数据流收集。 */
    private fun splitDataFlowForPostponedLambdas(lambdas: Set<CfirFunctionSymbol<*>> = emptySet()) {
        postponedLambdaExits.push(PostponedLambdas(lambdas))
    }

    /** 把 return 跳出的 postponed lambda 出口移动到目标 lambda 所在层。 */
    private fun jumpDataFlowFromPostponedLambdas(symbol: CfirFunctionSymbol<*>) {
        val currentLevelExits = postponedLambdaExits.pop().exits
        if (currentLevelExits.isEmpty()) return

        for (postponed in postponedLambdaExits.all()) {
            if (symbol in postponed.lambdas) {
                postponed.exits.addAll(currentLevelExits)
                break
            }
        }
    }

    /** 把当前层 postponed lambda 出口统一接入指定节点。 */
    private fun unifyDataFlowFromPostponedLambdas(node: CFGNode<*>, callCompleted: Boolean) {
        val currentLevelExits = postponedLambdaExits.pop().exits
        if (currentLevelExits.isEmpty()) return

        val nextLevelExits = postponedLambdaExits.topOrNull()?.exits.takeIf { !callCompleted }
        if (nextLevelExits != null) {
            for ((exit, kind) in currentLevelExits) {
                if (kind.usedInCfa) {
                    addEdge(exit, node, preferredKind = EdgeKind.CfgForward)
                }
                nextLevelExits.add(exit to EdgeKind.DfgForward)
            }
            return
        }

        for ((exit, kind) in currentLevelExits) {
            if (kind.usedInCfa || !exit.isDead) {
                addEdge(exit, node, preferredKind = kind, label = PostponedPath)
            }
        }
    }

    /** 在控制结构出口处合并 postponed lambda 出口。 */
    private fun mergeDataFlowFromPostponedLambdas(node: CFGNode<*>, callCompleted: Boolean) {
        val currentLevelExits = postponedLambdaExits.pop().exits
        if (currentLevelExits.isEmpty()) return

        val nextLevelExits = postponedLambdaExits.topOrNull()?.exits.takeIf { !callCompleted }
        if (nextLevelExits != null) {
            node.updateDeadStatus()
            nextLevelExits += createMergePostponedLambdaExitsNode(node.fir).also { mergeNode ->
                addEdge(node, mergeNode)
                for ((exit, kind) in currentLevelExits) {
                    if (kind.usedInCfa) {
                        addEdge(exit, node, preferredKind = EdgeKind.CfgForward, propagateDeadness = false)
                    }
                    addEdge(exit, mergeNode, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
                }
            } to EdgeKind.DfgForward
            return
        }

        for ((exit, kind) in currentLevelExits) {
            addEdge(exit, node, preferredKind = kind, label = PostponedPath, propagateDeadness = false)
        }
    }

    // ----------------------------------- Ordered exceptional completion -----------------------------------

    /** Main 捕获本 try 的异常；Catch 只执行本 finally；Finally 不再处理本 try。 */
    private enum class ExceptionPhase { Main, Catch, Finally }
    private class ExceptionScope(var state: ExceptionScopeState)
    private data class ExceptionScopeState(
        val tryExit: TryExpressionExitNode,
        val catches: List<CatchClauseEnterNode>,
        val finallyEnter: FinallyBlockEnterNode?,
        val phase: ExceptionPhase,
        val resources: List<CfirResourceVariable>,
    ) {
        fun copy(copier: ControlFlowGraphCopier): ExceptionScopeState = copy(
            tryExit = copier[tryExit], catches = catches.map(copier::get),
            finallyEnter = finallyEnter?.let(copier::get),
        )
    }
    private data class ExceptionContinuation(
        val transfer: ExceptionTransferNode,
        val scopes: List<ExceptionScopeState>,
        val boundary: ControlFlowGraph,
    )
    private data class JumpContinuation(
        val jump: JumpNode,
        val target: CFGNode<*>,
        val scopes: List<ExceptionScopeState>,
        val boundary: ControlFlowGraph,
    )

    /** 仅真实 called-in-place graph 可以向外传播；普通 lambda/函数在自身出口截断。 */
    private fun exceptionBoundary(): ControlFlowGraph =
        graphs.all().first { it.kind != ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace }

    private fun activeExceptionScopes(): List<ExceptionScopeState> {
        val permittedGraphs = graphs.all().takeWhile { it !== exceptionBoundary() } + exceptionBoundary()
        return exceptionScopes.all().map { it.state }.filter { it.tryExit.owner in permittedGraphs }
    }

    /** 异常出口属于执行边界图；本图的非 CFA 入边保证它能独立排序。 */
    private fun exceptionExit(graph: ControlFlowGraph): ExceptionExitNode =
        graph.exceptionExitNode ?: ExceptionExitNode(graph, graph.enterNode.fir, graph.enterNode.level).also {
            graph.exceptionExitNode = it
            // called-in-place transfer 可以全部来自子图，不能依赖跨图前驱完成本图排序。
            // 排序专用的结构死入边不代表一次异常，不贡献 live DFA 或任何 CFA 输入；
            // 出口 deadness 仍由所属图 complete 时的全部真实 CFA 输入决定。
            addEdge(graph.enterNode, it, preferredKind = EdgeKind.DeadDfgForward,
                propagateDeadness = false, label = UncaughtExceptionPath)
        }

    /** 从真实程序点保存异常值，按当前词法处理栈有序分派。 */
    private fun routeException(
        from: CFGNode<*>, source: CFGNode<*>, scopes: List<ExceptionScopeState>, boundary: ControlFlowGraph,
        incomingLabel: EdgeLabel = UncaughtExceptionPath,
    ) {
        val transfer = ExceptionTransferNode(from.owner, source, from.level)
        addEdge(from, transfer, propagateDeadness = false, label = incomingLabel)
        transfer.updateDeadStatus()
        val scope = scopes.firstOrNull()
        if (scope == null) {
            addEdge(transfer, exceptionExit(boundary), propagateDeadness = false, label = UncaughtExceptionPath)
            return
        }
        if (scope.phase == ExceptionPhase.Finally) {
            routeException(transfer, source, scopes.drop(1), boundary, checkNotNull(scope.finallyEnter))
            return
        }
        cleanupException(transfer, source, scope.resources.asReversed()) { cleaned, cleanedSource ->
            dispatchException(cleaned, cleanedSource, scope.copy(resources = emptyList()), scopes.drop(1), boundary)
        }
    }

    /**
     * 官方资源降糖保存最先产生的异常。已有异常时，isClosed/close 的异常不替换它；
     * 两种结果都继续逆序释放剩余资源，因此共享同一条携带原异常的清理尾链。
     */
    private fun cleanupException(
        from: CFGNode<*>, source: CFGNode<*>, resources: List<CfirResourceVariable>,
        done: (CFGNode<*>, CFGNode<*>) -> Unit,
    ) {
        var tail = from
        for (resource in resources) {
            val cleanup = ResourceCleanupNode(tail.owner, resource, tail.level)
            addEdge(tail, cleanup, propagateDeadness = false, label = UncaughtExceptionPath)
            cleanup.updateDeadStatus()
            tail = cleanup
        }
        done(tail, source)
    }

    /** 正常完成和 jump 的清理保留原 completion；每个释放点可产生新的动态异常。 */
    private fun cleanupNormal(
        from: CFGNode<*>, scope: ExceptionScopeState, outer: List<ExceptionScopeState>, boundary: ControlFlowGraph,
        label: EdgeLabel = NormalPath, isDead: Boolean = from.isDead,
    ): CFGNode<*> {
        var tail = from
        for (index in scope.resources.indices.reversed()) {
            val cleanup = ResourceCleanupNode(tail.owner, scope.resources[index], tail.level)
            addEdge(tail, cleanup, propagateDeadness = false,
                preferredKind = if (isDead) EdgeKind.DeadForward else EdgeKind.Forward, label = label)
            cleanup.updateDeadStatus()
            routeException(cleanup, cleanup, listOf(scope.copy(resources = scope.resources.take(index))) + outer, boundary)
            tail = cleanup
        }
        return tail
    }

    private fun dispatchException(
        from: CFGNode<*>, source: CFGNode<*>, scope: ExceptionScopeState,
        outer: List<ExceptionScopeState>, boundary: ControlFlowGraph,
    ) {
        val transfer = ExceptionTransferNode(from.owner, source, from.level)
        addEdge(from, transfer, propagateDeadness = false, label = UncaughtExceptionPath)
        transfer.updateDeadStatus()
        var failed: CFGNode<*> = transfer
        if (scope.phase == ExceptionPhase.Main) {
            for ((index, catchEnter) in scope.catches.withIndex()) {
                val decision = ExceptionDispatchNode(transfer.owner, catchEnter.fir, transfer, scope.catches.take(index), transfer.level)
                addEdge(failed, decision, propagateDeadness = false,
                    label = if (failed === transfer) UncaughtExceptionPath else ExceptionNotMatched)
                decision.updateDeadStatus()
                addEdge(decision, catchEnter, propagateDeadness = false, label = catchEnter)
                exceptionDispatches.getOrPut(catchEnter) { mutableListOf() }.add(decision)
                failed = decision
            }
        }
        val finallyEnter = scope.finallyEnter
        if (finallyEnter != null) {
            val resumed = ExceptionTransferNode(finallyEnter.owner, source, finallyEnter.level)
            addEdge(failed, finallyEnter, propagateDeadness = false, label = resumed)
            exceptionContinuations.getOrPut(finallyEnter) { mutableListOf() }
                .add(ExceptionContinuation(resumed, outer, boundary))
        } else {
            val uncaught = ExceptionTransferNode(from.owner, source, from.level)
            addEdge(failed, uncaught, propagateDeadness = false, label = ExceptionNotMatched)
            uncaught.updateDeadStatus()
            routeException(uncaught, source, outer, boundary)
        }
    }

    /**
     * catch 类型引用全部解析后再收紧有序 dispatch。
     * 只有确定覆盖的上界才删除失败分支；不相交判定不靠名字或错误类型。
     */
    fun refineExceptionDispatch(
        catchEnter: CatchClauseEnterNode,
        isSubtypeOf: (ConeCangJieType, ConeCangJieType) -> Boolean,
    ) {
        fun typesOf(catch: CfirCatch): List<ConeCangJieType> =
            if (catch.pattern.typeRefs.isEmpty()) listOf(checkNotNull(catch.pattern.resolvedTypeRef).coneType)
            else catch.pattern.typeRefs.map { checkNotNull(it.coneTypeOrNull) { "Catch type not resolved" } }
        val types = typesOf(catchEnter.fir)
        for (decision in exceptionDispatches[catchEnter].orEmpty()) {
            val preceding = decision.precedingCatches.flatMap { typesOf(it.fir) }
            val shadowed = types.all { type ->
                type !is ConeErrorType && preceding.any { it !is ConeErrorType && isSubtypeOf(type, it) }
            }
            val actual = decision.transfer.exceptionType
            val alwaysMatches = actual != null && actual !is ConeErrorType &&
                types.any { it !is ConeErrorType && isSubtypeOf(actual, it) }
            // catch 合法类型为 Exception/Error 类；单继承类树中两个互不为子类型的分支不相交。
            val disjoint = actual is org.cangnova.cangjie.cfir.types.ConeClassLikeType &&
                types.all {
                    it is org.cangnova.cangjie.cfir.types.ConeClassLikeType &&
                        !isSubtypeOf(actual, it) && !isSubtypeOf(it, actual)
                }
            for (next in decision.followingNodes) {
                if ((shadowed || disjoint) && next === catchEnter || alwaysMatches && next !== catchEnter) {
                    CFGNode.killEdge(decision, next, propagateDeadness = false)
                    propagateExceptionDeadness(next)
                }
            }
        }
        catchEnter.updateDeadStatus()
    }

    /** 预创建的 catch/finally/异常出口仍可能获得后续输入，只传播到本次独有的分派链。 */
    private fun propagateExceptionDeadness(node: CFGNode<*>) {
        if (node !is ExceptionTransferNode && node !is ExceptionDispatchNode) return
        node.updateDeadStatus()
        if (!node.isDead) return
        for (next in node.followingNodes) {
            CFGNode.killEdge(node, next, propagateDeadness = false)
            propagateExceptionDeadness(next)
        }
    }

    /** jump 穿越资源与 finally 时仍使用实际目标节点标签，并记录实际 return 表达式。 */
    private fun routeJump(
        from: CFGNode<*>, jump: JumpNode, target: CFGNode<*>,
        scopes: List<ExceptionScopeState>, boundary: ControlFlowGraph,
    ) {
        val scope = scopes.firstOrNull { it.tryExit.level >= target.level }
        val deadInput = from.isDead || from is FinallyBlockExitNode && from.enterNode.previousNodes.none {
            val edge = it.edgeTo(from.enterNode)
            edge.label === target && edge.kind.usedInCfa && !edge.kind.isDead
        }
        if (scope == null) {
            if (from !== jump) nonDirectJumps.put(target, jump)
            if (target.returnPathIsBackwards) addBackEdge(from, target, isDead = deadInput,
                label = if (from === jump) NormalPath else target as EdgeLabel)
            else addEdge(from, target, propagateDeadness = false,
                preferredKind = if (deadInput) EdgeKind.DeadForward else EdgeKind.Forward,
                label = if (from === jump) NormalPath else target as EdgeLabel)
            return
        }
        val outer = scopes.drop(scopes.indexOf(scope) + 1)
        if (scope.phase == ExceptionPhase.Finally) {
            val overridden = CompletionOverrideNode(from.owner, jump, from.level)
            addEdge(from, overridden, propagateDeadness = false,
                preferredKind = if (deadInput) EdgeKind.DeadForward else EdgeKind.Forward,
                label = checkNotNull(scope.finallyEnter))
            overridden.updateDeadStatus()
            routeJump(overridden, jump, target, outer, boundary)
            return
        }
        val cleaned = cleanupNormal(from, scope, outer, boundary, target as EdgeLabel, deadInput)
        val finallyEnter = scope.finallyEnter
        if (finallyEnter != null) {
            addEdge(cleaned, finallyEnter, propagateDeadness = false, label = target)
            nonDirectJumps.put(target, jump)
            jumpContinuations.getOrPut(finallyEnter) { mutableListOf() }.add(JumpContinuation(jump, target, outer, boundary))
        } else {
            routeJump(cleaned, jump, target, outer, boundary)
        }
    }

    // ----------------------------------- Try / finally helpers -----------------------------------

    /** finally 入口的所有普通输入是否都是死路径。 */
    private val FinallyBlockEnterNode.allNormalInputsAreDead: Boolean
        get() = previousNodes.all { previous ->
            val edge = edgeFrom(previous)
            edge.kind.isDead || edge.label != NormalPath
        }

    /** 为穿越 finally 的 return/break/continue 重新补边。 */
    private fun <T> CFGNode<*>.addReturnEdges(nodes: Iterable<T>, minLevel: Int)
        where T : CFGNode<*>, T : EdgeLabel {
        for (node in nodes) {
            when {
                node.level < minLevel || nonDirectJumps[node].isEmpty() -> continue
                node.returnPathIsBackwards -> addBackEdge(this, node, label = node)
                else -> addEdge(this, node, propagateDeadness = false, label = node)
            }
        }
    }

    /** 返回下一层能捕获异常的图出口层级。 */
    private fun levelOfNextExceptionCatchingGraph(): Int =
        graphs.all().first { it.kind != ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace }.exitNode.level

    /** 完成 Nothing 返回调用后，把后续边改成死路径。 */
    private fun completeFunctionCall(node: FunctionCallExitNode): Boolean {
        if (node.fir.coneTypeOrNull?.isNothing != true) return false

        // Nothing 只杀成功 continuation；调用异常边和已有重复调用回边必须保留。
        val edges = node.followingNodes.filter { node.edgeTo(it).label != UncaughtExceptionPath }
            .map { it to node.edgeTo(it) }
        if (edges.singleOrNull()?.first is StubNode) return true
        val exceptionalEdges = node.followingNodes.filter { node.edgeTo(it).label == UncaughtExceptionPath }
            .map { it to node.edgeTo(it) }
        val stub = StubNode(node.owner, node.level)
        CFGNode.removeAllOutgoingEdges(node)
        exceptionalEdges.forEach { (to, edge) ->
            CFGNode.addEdge(node, to, edge.kind, propagateDeadness = false, label = edge.label)
        }
        CFGNode.addEdge(node, stub, EdgeKind.DeadForward, propagateDeadness = false)
        // 图级队列可在终端调用尚无成功后继时完成；图出口必须从同一 dead stub 追加。
        if (lastNodes.topOrNull() === node) {
            lastNodes.pop()
            lastNodes.push(stub)
        }
        for ((to, edge) in edges) {
            val kind = edge.kind.toDead()
            CFGNode.addEdge(stub, to, kind, propagateDeadness = false, label = edge.label)
            to.updateDeadStatus()
            propagateDeadnessForward(to)
        }
        return true
    }

    /** 沿后继节点传播 deadness。 */
    private fun propagateDeadnessForward(node: CFGNode<*>) {
        if (!node.isDead) return
        for (next in node.followingNodes) {
            val kind = node.edgeTo(next).kind
            if (CFGNode.killEdge(node, next, propagateDeadness = false) && !kind.isBack && kind.usedInCfa) {
                next.updateDeadStatus()
                propagateDeadnessForward(next)
            }
        }
    }

    // ----------------------------------- Postponed lambdas helper -----------------------------------

    /** 一层 postponed lambda 的符号集合与出口边集合。 */
    internal class PostponedLambdas(
        /** 该层管理的 lambda 符号集合。 */
        val lambdas: Set<CfirFunctionSymbol<*>>,
        /** 该层已经收集到的 lambda 出口节点和边类型。 */
        val exits: MutableList<Pair<CFGNode<*>, EdgeKind>> = mutableListOf(),
    )

    /**
     * 简化版 multimap,用于 `nonDirectJumps`。
     * 对位 Kotlin FIR `ListMultimap`,但对仓颉侧足够用不依赖 util-listMultimap。
     */
    internal class ListMultimap<K, V> {
        /** 实际存储 key 到 value 列表的映射。 */
        private val backing: MutableMap<K, MutableList<V>> = mutableMapOf()

        /** 返回 key 对应的值列表，缺失时返回空列表。 */
        operator fun get(key: K): List<V> = backing[key] ?: emptyList()

        /** 向 key 追加单个值。 */
        fun put(key: K, value: V) {
            backing.getOrPut(key) { mutableListOf() }.add(value)
        }

        /** 向 key 批量追加值。 */
        fun putAll(key: K, values: Collection<V>) {
            backing.getOrPut(key) { mutableListOf() }.addAll(values)
        }

        /** 清空 multimap。 */
        fun clear() {
            backing.clear()
        }

        /** 返回不可变列表视图形式的条目集合。 */
        fun entries(): Set<Map.Entry<K, List<V>>> = backing.mapValues { it.value.toList() }.entries
    }
}

/**
 * 沿用 Kotlin FIR 的 CfgInternals opt-in 约定:该注解标记"CFG 内部"API,供 builder 与 DFA
 * 核心使用,外部不应直接调用。
 */
