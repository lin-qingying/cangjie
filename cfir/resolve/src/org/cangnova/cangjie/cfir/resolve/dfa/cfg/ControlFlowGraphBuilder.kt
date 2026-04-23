package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.CfirElement
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
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
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
import org.cangnova.cangjie.cfir.resolve.dfa.Stack
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode.Companion.addEdge as addEdgeStatic
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
    private val graphs: Stack<ControlFlowGraph>,
    private val lastNodes: Stack<CFGNode<*>>,

    private val exitTargetsForReturn: MutableMap<CfirFunctionSymbol<*>, FunctionExitNode>,
    private val enterToLocalClassesMembers: MutableMap<Any, Pair<CFGNode<*>, EdgeKind>>,

    private val nonDirectJumps: ListMultimap<CFGNode<*>, JumpNode>,

    private val argumentListSplitNodes: Stack<SplitPostponedLambdasNode?>,
    private val postponedAnonymousFunctionNodes: MutableMap<CfirFunctionSymbol<*>, Pair<CFGNode<*>, PostponedLambdaExitNode?>>,
    private val anonymousFunctionCaptureNodes: MutableMap<CfirFunctionSymbol<*>, AnonymousFunctionCaptureNode>,
    private val postponedLambdaExits: Stack<PostponedLambdas>,

    private val loopExitNodes: MutableMap<CfirElement, LoopExitNode>,
    private val loopConditionEnterNodes: MutableMap<CfirElement, LoopConditionEnterNode>,

    private val matchExitNodes: Stack<MatchExitNode>,

    private val tryExitNodes: Stack<TryExpressionExitNode>,
    private val catchNodes: Stack<List<CatchClauseEnterNode>>,
    private val catchBlocksInProgress: Stack<CatchClauseEnterNode>,
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode>,
    private val finallyBlocksInProgress: Stack<FinallyBlockEnterNode>,
    private val finallyBlocksInProgressSet: MutableSet<CfirElement>,

    private val exitFunctionCallArgumentsNodes: Stack<FunctionCallArgumentsExitNode?>,
    private val exitOptionalChainNodes: Stack<ExitOptionalChainNode>,
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

    constructor() : this(
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
        tryExitNodes = stackOf(),
        catchNodes = stackOf(),
        catchBlocksInProgress = stackOf(),
        finallyEnterNodes = stackOf(),
        finallyBlocksInProgress = stackOf(),
        finallyBlocksInProgressSet = mutableSetOf(),
        exitFunctionCallArgumentsNodes = stackOf(),
        exitOptionalChainNodes = stackOf(),
        notCompletedFunctionCalls = stackOf(),
    )

    // ----------------------------------- Accessors -----------------------------------

    /** 当前正在构造的图;在无活动图时访问会抛出。 */
    val currentGraph: ControlFlowGraph
        get() = graphs.top()

    val currentGraphOrNull: ControlFlowGraph?
        get() = graphs.topOrNull()

    /**
     * 层级计数器:`try` 表达式不是子图,但会提升层级用于识别 try 内外节点。
     */
    val levelCounter: Int
        get() = graphs.size + tryExitNodes.size

    val lastNode: CFGNode<*>
        get() = lastNodes.top()

    val lastNodeOrNull: CFGNode<*>?
        get() = lastNodes.topOrNull()

    val isTopLevel: Boolean
        get() = graphs.isEmpty || currentGraph.kind == ControlFlowGraph.Kind.File

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
            tryExitNodes = tryExitNodes.createSnapshot(copier::get),
            catchNodes = catchNodes.createSnapshot { it.map(copier::get) },
            catchBlocksInProgress = catchBlocksInProgress.createSnapshot(copier::get),
            finallyEnterNodes = finallyEnterNodes.createSnapshot(copier::get),
            finallyBlocksInProgress = finallyBlocksInProgress.createSnapshot(copier::get),
            finallyBlocksInProgressSet = finallyBlocksInProgressSet.toMutableSet(),
            exitFunctionCallArgumentsNodes = exitFunctionCallArgumentsNodes.createSnapshot { it?.let(copier::get) },
            exitOptionalChainNodes = exitOptionalChainNodes.createSnapshot(copier::get),
            notCompletedFunctionCalls = notCompletedFunctionCalls.createSnapshot { it.mapTo(mutableListOf(), copier::get) },
        )
    }

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
        tryExitNodes.reset()
        catchNodes.reset()
        catchBlocksInProgress.reset()
        finallyEnterNodes.reset()
        finallyBlocksInProgress.reset()
        finallyBlocksInProgressSet.clear()
        exitFunctionCallArgumentsNodes.reset()
        exitOptionalChainNodes.reset()
        notCompletedFunctionCalls.reset()
    }

    fun withinFinallyBlock(element: CfirElement): Boolean = element in finallyBlocksInProgressSet

    // ----------------------------------- Graph enter / exit core -----------------------------------

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
        val graph = ControlFlowGraph(fir as? CfirDeclaration, name, kind).also { graphs.push(it) }
        val (enterNode, exitNode) = nodes(fir)
        graph.enterNode = enterNode
        graph.exitNode = exitNode
        lastNodes.push(enterNode)
        return enterNode
    }

    private fun popGraph(): ControlFlowGraph {
        return graphs.pop().also { it.complete() }
    }

    private inline fun <reified ExitNode> exitGraph(): Pair<ExitNode, ControlFlowGraph>
        where ExitNode : CFGNode<*>,
              ExitNode : GraphExitNodeMarker {
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

    internal fun popAndAddEdge(to: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        val from = lastNodes.pop()
        addEdge(from, to, preferredKind = preferredKind, propagateDeadness = true)
    }

    internal fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath,
    ) {
        val kind = if (from.isDead && preferredKind != EdgeKind.DeadForward) preferredKind.toDead() else preferredKind
        addEdgeStatic(from, to, kind, propagateDeadness, label)
    }

    internal fun addBackEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        isDead: Boolean = false,
        label: EdgeLabel = NormalPath,
    ) {
        val kind = if (isDead || from.isDead || to.isDead) EdgeKind.DeadCfgBackward else EdgeKind.CfgBackward
        addEdgeStatic(from, to, kind, propagateDeadness = false, label)
    }

    internal fun addNewSimpleNode(
        node: CFGNode<*>,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
    ): CFGNode<*> {
        val lastNode = lastNodes.pop()
        val kind = if (isDead) preferredKind.toDead() else preferredKind
        addEdgeStatic(lastNode, node, kind, propagateDeadness = true)
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

    fun enterFile(file: CfirFile): FileEnterNode =
        enterGraph(file, "<file>", ControlFlowGraph.Kind.File) {
            createFileEnterNode(it) to createFileExitNode(it)
        }

    fun exitFile(): Pair<FileExitNode, ControlFlowGraph> {
        val graph = currentGraph
        val enterNode = graph.enterNode as FileEnterNode
        enterNode.subGraphs = collectSubGraphs(enterNode.fir.declarations)
        return exitGraph()
    }

    // ----------------------------------- Class -----------------------------------

    fun enterClass(klass: CfirClass): ClassEnterNode =
        enterGraph(klass, klass.name.asString(), ControlFlowGraph.Kind.Class) {
            createClassEnterNode(it) to createClassExitNode(it)
        }

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

    fun exitFunction(function: CfirFunction): Pair<FunctionExitNode, ControlFlowGraph> {
        require(function !is CfirAnonymousFunction)
        exitTargetsForReturn.remove(function.symbol)
        return exitGraph()
    }

    // ----------------------------------- Anonymous function -----------------------------------

    fun enterAnonymousFunction(anonymousFunction: CfirAnonymousFunction): FunctionEnterNode {
        val graphKind = ControlFlowGraph.Kind.AnonymousFunction
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

    fun enterFieldInitializer(field: CfirFieldVariable): FieldInitializerEnterNode =
        enterGraph(field, field.name.asString(), ControlFlowGraph.Kind.FieldInitializer) {
            createFieldInitializerEnterNode(it) to createFieldInitializerExitNode(it)
        }

    fun exitFieldInitializer(): Pair<FieldInitializerExitNode, ControlFlowGraph> = exitGraph()

    // ----------------------------------- Value parameter / default argument -----------------------------------

    fun enterValueParameter(parameter: CfirValueParameter): EnterValueParameterNode =
        createEnterValueParameterNode(parameter).also { addNewSimpleNode(it) }

    fun enterDefaultArguments(parameter: CfirValueParameter): EnterDefaultArgumentsNode =
        enterGraph(parameter, "<default>", ControlFlowGraph.Kind.DefaultArgument) {
            createEnterDefaultArgumentsNode(it) to createExitDefaultArgumentsNode(it)
        }

    fun exitDefaultArguments(): Pair<ExitDefaultArgumentsNode, ControlFlowGraph> = exitGraph()

    fun exitValueParameter(parameter: CfirValueParameter): ExitValueParameterNode =
        createExitValueParameterNode(parameter).also { addNewSimpleNode(it) }

    // ----------------------------------- Code fragment -----------------------------------

    fun enterCodeFragment(fragment: CfirCodeFragment): CodeFragmentEnterNode =
        enterGraph(fragment, "<code-fragment>", ControlFlowGraph.Kind.CodeFragment) {
            createCodeFragmentEnterNode(it) to createCodeFragmentExitNode(it)
        }

    fun exitCodeFragment(): Pair<CodeFragmentExitNode, ControlFlowGraph> = exitGraph()

    // ----------------------------------- Return expressions -----------------------------------

    /**
     * 从函数 CFG 中提取真实返回结果集合。
     *
     * 收集来源统一为：
     * - 直接连到函数 exit 的 block 尾表达式；
     * - 通过 non-direct jump 连到函数 exit 的显式 `return expr`。
     *
     * 这样既能覆盖“最后一条表达式即返回值”，也能覆盖显式 return，
     * 并且天然排除 CFG 上不可达的块尾表达式。
     */
    fun returnExpressionsOfFunction(function: CfirFunction): Collection<CfirExpression>? {
        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode ?: return null
        val result = linkedSetOf<CfirExpression>()

        fun CFGNode<*>.returnExpression(): CfirExpression? = when (this) {
            is BlockExitNode -> (fir.statements.lastOrNull() as? CfirExpression)?.takeUnless { it is CfirReturnExpression }
            is JumpNode -> (fir as? CfirReturnExpression)?.result
            else -> null
        }

        exitNode.previousNodes
            .filter { exitNode.edgeFrom(it).kind.usedInCfa && exitNode.edgeFrom(it).label == NormalPath }
            .mapNotNullTo(result) { it.returnExpression() }
        nonDirectJumps[exitNode].mapNotNullTo(result) { it.returnExpression() }
        return result
    }

    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction): Collection<CfirExpression>? {
        return returnExpressionsOfFunction(function)
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: CfirBlock): BlockEnterNode =
        createBlockEnterNode(block).also { addNewSimpleNode(it) }

    fun exitBlock(block: CfirBlock): BlockExitNode =
        createBlockExitNode(block).also { addNewSimpleNode(it) }

    // ----------------------------------- Match -----------------------------------

    fun enterMatchExpression(matchExpression: CfirMatchExpression): MatchEnterNode {
        val node = createMatchEnterNode(matchExpression)
        addNewSimpleNode(node)
        matchExitNodes.push(createMatchExitNode(matchExpression))
        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return node
    }

    fun enterMatchBranchCondition(branch: CfirMatchBranch): MatchBranchConditionEnterNode =
        createMatchBranchConditionEnterNode(branch).also { addNewSimpleNode(it) }

    fun exitMatchBranchCondition(branch: CfirMatchBranch): Pair<MatchBranchConditionExitNode, MatchBranchResultEnterNode> {
        val conditionExit = createMatchBranchConditionExitNode(branch).also { addNewSimpleNode(it) }
        lastNodes.push(conditionExit)
        val resultEnter = createMatchBranchResultEnterNode(branch).also { addNewSimpleNode(it) }
        return conditionExit to resultEnter
    }

    fun exitMatchBranchResult(branch: CfirMatchBranch): MatchBranchResultExitNode {
        val resultExit = createMatchBranchResultExitNode(branch)
        popAndAddEdge(resultExit)
        addEdge(resultExit, matchExitNodes.top(), propagateDeadness = false)
        return resultExit
    }

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
                addEdge(lastConditionExit, it)
                addEdge(it, exitNode, propagateDeadness = false)
            }
        } else {
            addEdge(lastConditionExit, exitNode, propagateDeadness = false)
            null
        }
        mergeDataFlowFromPostponedLambdas(exitNode, callCompleted)
        exitNode.updateDeadStatus()
        lastNodes.push(exitNode)
        return exitNode to syntheticElse
    }

    // ----------------------------------- Loop -----------------------------------

    fun enterWhileLoop(loop: CfirLoopExpression): Pair<LoopEnterNode, LoopConditionEnterNode> {
        val loopEnter = createLoopEnterNode(loop).also { addNewSimpleNode(it) }
        loopExitNodes[loop] = createLoopExitNode(loop)
        val conditionEnter = createLoopConditionEnterNode(loop.condition, loop).also { addNewSimpleNode(it) }
        loopConditionEnterNodes[loop] = conditionEnter
        return loopEnter to conditionEnter
    }

    fun exitWhileLoopCondition(loop: CfirLoopExpression): Pair<LoopConditionExitNode, LoopBlockEnterNode> {
        val conditionExit = createLoopConditionExitNode(loop.condition, loop).also { addNewSimpleNode(it) }
        addEdge(conditionExit, loopExitNodes.getValue(loop), propagateDeadness = false)
        val blockEnter = createLoopBlockEnterNode(loop).also { addNewSimpleNode(it) }
        return conditionExit to blockEnter
    }

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

    fun enterDoWhileLoop(loop: CfirLoopExpression): Pair<LoopEnterNode, LoopBlockEnterNode> {
        val loopEnter = createLoopEnterNode(loop).also { addNewSimpleNode(it) }
        loopExitNodes[loop] = createLoopExitNode(loop)
        val blockEnter = createLoopBlockEnterNode(loop).also { addNewSimpleNode(it) }
        lastNodes.push(blockEnter)
        loopConditionEnterNodes[loop] = createLoopConditionEnterNode(loop.condition, loop)
        return loopEnter to blockEnter
    }

    fun enterDoWhileLoopCondition(loop: CfirLoopExpression): Pair<LoopBlockExitNode, LoopConditionEnterNode> {
        val blockExit = createLoopBlockExitNode(loop).also { addNewSimpleNode(it) }
        val conditionEnter = loopConditionEnterNodes[loop] ?: error("Missing do-while condition entry")
        addNewSimpleNode(conditionEnter)
        conditionEnter.updateDeadStatus()
        return blockExit to conditionEnter
    }

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

    fun enterJump(jump: CfirJump<*>) {
        if (jump is CfirReturnExpression && jump.target.labeledElement is CfirAnonymousFunction) {
            splitDataFlowForPostponedLambdas()
        }
    }

    fun exitJump(jump: CfirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        addNonSuccessfullyTerminatingNode(node)

        if (jump is CfirReturnExpression && jump.target.labeledElement is CfirAnonymousFunction) {
            jumpDataFlowFromPostponedLambdas(jump.target.labeledElement.symbol)
        }

        val nextNode = when (jump) {
            is CfirReturnExpression -> exitTargetsForReturn[jump.target.labeledElement.symbol]
            is CfirContinueExpression -> loopConditionEnterNodes[jump.target.labeledElement]
            is CfirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> error("Unsupported jump type: ${jump::class.simpleName}")
        } ?: return node

        val nextFinally = finallyEnterNodes.topOrNull()?.takeIf { it.level > nextNode.level }
        when {
            nextFinally != null -> {
                addEdge(node, nextFinally, propagateDeadness = false, label = nextNode)
                nonDirectJumps.put(nextNode, node)
            }

            nextNode.returnPathIsBackwards -> addBackEdge(node, nextNode)
            else -> addEdge(node, nextNode, propagateDeadness = false)
        }
        return node
    }

    fun exitThrowExceptionNode(throwExpression: CfirThrowExpression): ThrowExceptionNode =
        createThrowExceptionNode(throwExpression).also { addNonSuccessfullyTerminatingNode(it) }

    // ----------------------------------- Try / catch / finally -----------------------------------

    fun enterTryExpression(tryExpression: CfirTryExpression): Pair<TryExpressionEnterNode, TryMainBlockEnterNode> {
        val tryEnter = createTryExpressionEnterNode(tryExpression).also { addNewSimpleNode(it) }
        tryExitNodes.push(createTryExpressionExitNode(tryExpression))

        val tryMainEnter = createTryMainBlockEnterNode(tryExpression).also { addNewSimpleNode(it) }
        catchNodes.push(tryExpression.catches.map(::createCatchClauseEnterNode))
        if (tryExpression.finallyBlock != null) {
            finallyEnterNodes.push(createFinallyBlockEnterNode(tryExpression))
        }

        for (catchEnter in catchNodes.top()) {
            addEdge(tryEnter, catchEnter, propagateDeadness = false)
        }
        finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExpression }?.let {
            addEdge(tryEnter, it, propagateDeadness = false, label = UncaughtExceptionPath)
        }

        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return tryEnter to tryMainEnter
    }

    fun exitTryMainBlock(): TryMainBlockExitNode {
        val tryExitNode = tryExitNodes.top()
        val node = createTryMainBlockExitNode(tryExitNode.fir)
        popAndAddEdge(node)
        val nextNode = finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNode.fir } ?: tryExitNode
        addEdge(node, nextNode, propagateDeadness = false)
        for (catchEnter in catchNodes.pop().asReversed()) {
            catchBlocksInProgress.push(catchEnter)
            addEdge(node, catchEnter, propagateDeadness = false)
        }
        return node
    }

    fun enterCatchClause(catch: CfirCatch): CatchClauseEnterNode {
        val catchEnter = catchBlocksInProgress.pop()
        require(catchEnter.fir === catch) { "Catch stack out of sync" }
        finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNodes.top().fir }?.let {
            addEdge(catchEnter, it, propagateDeadness = false, label = UncaughtExceptionPath)
        }
        lastNodes.push(catchEnter)
        return catchEnter
    }

    fun exitCatchClause(catch: CfirCatch): CatchClauseExitNode {
        val tryExitNode = tryExitNodes.top()
        val catchExit = createCatchClauseExitNode(catch)
        popAndAddEdge(catchExit)
        val nextNode = finallyEnterNodes.topOrNull()?.takeIf { it.fir === tryExitNode.fir } ?: tryExitNode
        addEdge(catchExit, nextNode, propagateDeadness = false)
        return catchExit
    }

    fun enterFinallyBlock(): FinallyBlockEnterNode {
        val node = finallyEnterNodes.pop()
        lastNodes.push(node)
        finallyBlocksInProgress.push(node)
        finallyBlocksInProgressSet.add(node.fir)
        return node
    }

    fun exitFinallyBlock(): FinallyBlockExitNode {
        val enterNode = finallyBlocksInProgress.top()
        val tryExitNode = tryExitNodes.top()
        val exitNode = createFinallyBlockExitNode(enterNode)
        popAndAddEdge(exitNode)
        addEdge(exitNode, tryExitNode, propagateDeadness = false, preferredKind = if (enterNode.allNormalInputsAreDead) EdgeKind.DeadForward else EdgeKind.Forward)

        val nextExitLevel = levelOfNextExceptionCatchingGraph()
        val nextFinally = finallyEnterNodes.topOrNull()?.takeIf { it.level > nextExitLevel }
        if (nextFinally != null) {
            addEdge(exitNode, nextFinally, propagateDeadness = false, label = UncaughtExceptionPath)
        }

        val incomingLabels = enterNode.previousNodes.mapTo(mutableSetOf()) { it.edgeTo(enterNode).label }
        val nextFinallyOrExitLevel = nextFinally?.level ?: nextExitLevel
        exitNode.addReturnEdges(exitTargetsForReturn.values.filter { it in incomingLabels }, nextFinallyOrExitLevel)
        exitNode.addReturnEdges(loopConditionEnterNodes.values.filter { it in incomingLabels }, nextFinallyOrExitLevel)
        exitNode.addReturnEdges(loopExitNodes.values.filter { it in incomingLabels }, nextFinallyOrExitLevel)
        return exitNode
    }

    fun exitTryExpression(callCompleted: Boolean): TryExpressionExitNode {
        var haveNothingReturnCall = false
        notCompletedFunctionCalls.pop().forEach { haveNothingReturnCall = completeFunctionCall(it) || haveNothingReturnCall }
        val node = tryExitNodes.pop()
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

    fun exitCallArguments(): Pair<SplitPostponedLambdasNode?, FunctionCallArgumentsExitNode?> {
        val splitNode = argumentListSplitNodes.pop()?.also { addNewSimpleNode(it) }
        val exitNode = exitFunctionCallArgumentsNodes.pop()?.also { addNewSimpleNode(it) }
        return splitNode to exitNode
    }

    fun exitCallExplicitReceiver() {
        exitFunctionCallArgumentsNodes.topOrNull()?.explicitReceiverExitNode = lastNode
    }

    fun enterFunctionCall(functionCall: CfirFunctionCall): FunctionCallEnterNode =
        createFunctionCallEnterNode(functionCall).also { addNewSimpleNode(it) }

    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean): FunctionCallExitNode {
        val exitNode = createFunctionCallExitNode(functionCall)
        unifyDataFlowFromPostponedLambdas(exitNode, callCompleted)
        addNewSimpleNode(exitNode)
        if (!callCompleted) {
            notCompletedFunctionCalls.topOrNull()?.add(exitNode)
        }
        return exitNode
    }

    // ----------------------------------- Simple terminal nodes -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression): QualifiedAccessNode =
        createQualifiedAccessNode(qualifiedAccessExpression).also { addNewSimpleNode(it) }

    fun exitLiteralExpression(literalExpression: CfirLiteralExpression): LiteralExpressionNode =
        createLiteralExpressionNode(literalExpression).also { addNewSimpleNode(it) }

    fun exitVariableAssignment(assignment: CfirAssignment): VariableAssignmentNode =
        createVariableAssignmentNode(assignment).also { node ->
            addNewSimpleNode(node)

            val nextCatchNodes = catchNodes.topOrNull()
            if (!nextCatchNodes.isNullOrEmpty()) {
                val kind = if (nextCatchNodes.first().level > levelOfNextExceptionCatchingGraph()) EdgeKind.Forward else EdgeKind.DfgForward
                for (catchEnter in nextCatchNodes) {
                    addEdge(node, catchEnter, preferredKind = kind, propagateDeadness = false)
                }
            }
            finallyEnterNodes.topOrNull()?.let { finallyEnter ->
                val kind = if (finallyEnter.level > levelOfNextExceptionCatchingGraph()) EdgeKind.Forward else EdgeKind.DfgForward
                addEdge(node, finallyEnter, preferredKind = kind, propagateDeadness = false, label = UncaughtExceptionPath)
            }
        }

    fun enterOptionalChain(optionalChainExpression: CfirOptionalChainExpression): EnterOptionalChainNode =
        createEnterOptionalChainNode(optionalChainExpression).also { addNewSimpleNode(it) }

    fun exitOptionalChain(optionalChainExpression: CfirOptionalChainExpression): ExitOptionalChainNode =
        createExitOptionalChainNode(optionalChainExpression).also { addNewSimpleNode(it) }

    fun exitWrappedExpression(wrappedExpression: CfirWrappedExpression): WrappedExpressionNode =
        createWrappedExpressionNode(wrappedExpression).also { addNewSimpleNode(it) }

    // ----------------------------------- Helpers -----------------------------------

    private fun collectSubGraphs(declarations: List<CfirDeclaration>): List<ControlFlowGraph> {
        return declarations
            .filterIsInstance<CfirControlFlowGraphOwner>()
            .mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }
    }

    // ----------------------------------- Postponed lambda data-flow -----------------------------------

    private fun splitDataFlowForPostponedLambdas(lambdas: Set<CfirFunctionSymbol<*>> = emptySet()) {
        postponedLambdaExits.push(PostponedLambdas(lambdas))
    }

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

    // ----------------------------------- Try / finally helpers -----------------------------------

    private val FinallyBlockEnterNode.allNormalInputsAreDead: Boolean
        get() = previousNodes.all { previous ->
            val edge = edgeFrom(previous)
            edge.kind.isDead || edge.label != NormalPath
        }

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

    private fun levelOfNextExceptionCatchingGraph(): Int =
        graphs.all().first { it.kind != ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace }.exitNode.level

    private fun completeFunctionCall(node: FunctionCallExitNode): Boolean {
        if (node.fir.coneTypeOrNull?.isNothing != true) return false

        val stub = StubNode(node.owner, node.level)
        val edges = node.followingNodes.map { it to node.edgeTo(it) }
        CFGNode.removeAllOutgoingEdges(node)
        CFGNode.addEdge(node, stub, EdgeKind.DeadForward, propagateDeadness = false)
        for ((to, edge) in edges) {
            val kind = if (edge.kind.isBack) EdgeKind.DeadCfgBackward else EdgeKind.DeadForward
            CFGNode.addEdge(stub, to, kind, propagateDeadness = false, label = edge.label)
            to.updateDeadStatus()
            propagateDeadnessForward(to)
        }
        return true
    }

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

    internal class PostponedLambdas(
        val lambdas: Set<CfirFunctionSymbol<*>>,
        val exits: MutableList<Pair<CFGNode<*>, EdgeKind>> = mutableListOf(),
    )

    /**
     * 简化版 multimap,用于 `nonDirectJumps`。
     * 对位 Kotlin FIR `ListMultimap`,但对仓颉侧足够用不依赖 util-listMultimap。
     */
    internal class ListMultimap<K, V> {
        private val backing: MutableMap<K, MutableList<V>> = mutableMapOf()

        operator fun get(key: K): List<V> = backing[key] ?: emptyList()

        fun put(key: K, value: V) {
            backing.getOrPut(key) { mutableListOf() }.add(value)
        }

        fun putAll(key: K, values: Collection<V>) {
            backing.getOrPut(key) { mutableListOf() }.addAll(values)
        }

        fun clear() {
            backing.clear()
        }

        fun entries(): Set<Map.Entry<K, List<V>>> = backing.mapValues { it.value.toList() }.entries
    }
}

/**
 * 沿用 Kotlin FIR 的 CfgInternals opt-in 约定:该注解标记"CFG 内部"API,供 builder 与 DFA
 * 核心使用,外部不应直接调用。
 */
