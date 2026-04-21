package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.resolve.dfa.Stack
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode.Companion.addEdge as addEdgeStatic
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode.Companion.killEdge as killEdgeStatic
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
    private val notCompletedFunctionCalls: Stack<MutableList<FunctionCallExitNode>>,
) {
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
        label: EdgeLabel = NormalPath,
    ) {
        addEdgeStatic(from, to, EdgeKind.CfgBackward, propagateDeadness = false, label)
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

    fun exitFile(): Pair<FileExitNode, ControlFlowGraph> = exitGraph()

    // ----------------------------------- Class -----------------------------------

    fun enterClass(klass: CfirClass): ClassEnterNode =
        enterGraph(klass, klass.name.asString(), ControlFlowGraph.Kind.Class) {
            createClassEnterNode(it) to createClassExitNode(it)
        }

    fun exitClass(): Pair<ClassExitNode, ControlFlowGraph> = exitGraph()

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

        fun entries(): Set<Map.Entry<K, List<V>>> = backing.mapValues { it.value.toList() }.entries
    }
}

/**
 * 沿用 Kotlin FIR 的 CfgInternals opt-in 约定:该注解标记"CFG 内部"API,供 builder 与 DFA
 * 核心使用,外部不应直接调用。
 */
