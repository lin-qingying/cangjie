@file:Suppress("Reformat")

package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference

import org.cangnova.cangjie.cfir.expressions.CfirBinaryExpression

import org.cangnova.cangjie.cfir.expressions.CfirUnaryExpression

import org.cangnova.cangjie.cfir.expressions.CfirCallExpression

import org.cangnova.cangjie.cfir.resolve.match.CfirOrPatternLoweringKind
import org.cangnova.cangjie.cfir.resolve.match.loweringKind
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.CfirIterationOwner
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
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
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
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
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.resolve.dfa.FlowPath
import org.cangnova.cangjie.cfir.resolve.dfa.PersistentFlow
import org.cangnova.cangjie.cfir.resolve.dfa.DfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.utils.SmartList

/**
 * 控制流图节点基类。
 *
 * @param E 节点绑定的 CFIR 元素类型。
 * @property owner 节点所属控制流图。
 * @property level 节点在 CFG 构造中的嵌套层级。
 */
sealed class CFGNode<out E : CfirElement>(val owner: ControlFlowGraph, val level: Int) {
    /** 节点在所属图中的稳定 id。 */
    @OptIn(CfgInternals::class)
    val id: Int = owner.nodeCount++

    /** 当前节点是否按 union 语义合并多个前驱 flow。 */
    open val isUnion: Boolean get() = false

    /**
     * CFG 边操作工具。
     */
    companion object {
        /**
         * 在两个节点之间添加边。
         */
        @CfgInternals
        fun addEdge(
            from: CFGNode<*>,
            to: CFGNode<*>,
            kind: EdgeKind,
            propagateDeadness: Boolean,
            label: EdgeLabel = NormalPath,
        ) {
            from.followingNodes += to
            to.previousNodes += from
            if (kind != EdgeKind.Forward || label != NormalPath) {
                to.insertIncomingEdge(from, Edge.create(label, kind))
            }
            if (propagateDeadness && kind.isDead && !kind.isBack) {
                to.isDead = true
            }
        }

        /**
         * 将现有边标记为死亡边。
         *
         * @return 如果边状态发生变化则返回 `true`。
         */
        @CfgInternals
        fun killEdge(from: CFGNode<*>, to: CFGNode<*>, propagateDeadness: Boolean): Boolean {
            val oldEdge = to.edgeFrom(from)
            if (oldEdge.kind.isDead) return false
            val newEdge = Edge.create(oldEdge.label, oldEdge.kind.toDead())
            to.insertIncomingEdge(from, newEdge)
            if (propagateDeadness) {
                to.isDead = true
            }
            return true
        }

        /**
         * 移除指定节点的全部后继边。
         */
        @CfgInternals
        fun removeAllOutgoingEdges(from: CFGNode<*>) {
            for (to in from.followingNodes) {
                to.previousNodes.remove(from)
                to._incomingEdges?.remove(from)
            }
            from.followingNodes.clear()
        }

        /** 图退休时按对象关系对称移除一条边，不能只改其中一端或遗留显式边表。 */
        @CfgInternals
        fun removeEdge(from: CFGNode<*>, to: CFGNode<*>) {
            check(to in from.followingNodes && from in to.previousNodes)
            from.followingNodes.remove(to)
            to.previousNodes.remove(from)
            to._incomingEdges?.remove(from)
        }

        /**
         * 移除指定节点的全部前驱边。
         */
        @CfgInternals
        fun removeAllIncomingEdges(to: CFGNode<*>) {
            for (from in to.previousNodes) {
                from.followingNodes.remove(to)
            }
            to.previousNodes.clear()
            to._incomingEdges?.clear()
        }
    }

    /** 当前节点的前驱节点列表。 */
    val previousNodes: MutableList<CFGNode<*>> = SmartList()

    /** 当前节点的后继节点列表。 */
    val followingNodes: MutableList<CFGNode<*>> = SmartList()

    /** 非普通前向边的显式边数据表。 */
    internal var _incomingEdges: MutableMap<CFGNode<*>, Edge>? = null

    /**
     * 记录来自 [from] 的入边数据。
     */
    private fun insertIncomingEdge(from: CFGNode<*>, edge: Edge) {
        val map = _incomingEdges
        if (map != null) {
            map[from] = edge
        } else {
            _incomingEdges = mutableMapOf(from to edge)
        }
    }

    /** 查询指定前驱到当前节点的边。 */
    fun edgeFrom(other: CFGNode<*>): Edge = _incomingEdges?.get(other) ?: Edge.Normal_Forward

    /** 查询当前节点到指定后继的边。 */
    fun edgeTo(other: CFGNode<*>): Edge = other.edgeFrom(this)

    /** 节点绑定的 CFIR 元素。 */
    abstract val fir: E

    /** 当前节点是否处于死代码路径。 */
    var isDead: Boolean = false
        protected set

    /** 当前节点主路径上的 DFA flow。 */
    private var _flow: PersistentFlow? = null

    /** 当前节点主 flow 是否已经初始化。 */
    open val flowInitialized: Boolean get() = _flow != null

    /** 默认流存在只证明构图；正式拥有图消费还要求该程序点及其输入的真实 transfer 完成。 */
    open var dataFlowAnalysisState: DataFlowAnalysisState = DataFlowAnalysisState.UNINITIALIZED
        @CfgInternals set

    /** 当前节点主路径上的 DFA flow。 */
    open var flow: PersistentFlow
        get() = _flow ?: throw IllegalStateException("Flow for $this is not initialized")
        @CfgInternals
        set(value) {
            assert(_flow == null) { "Reassigning flow for $this" }
            _flow = value
        }

    /** 当前节点按备用路径保存的 flow。 */
    private var _alternateFlows: MutableMap<FlowPath, PersistentFlow>? = null
    private val _requiredAlternateFlowPaths: MutableSet<FlowPath> = linkedSetOf()

    /** 从真实控制来源得出的必需路径，与已经存在的 alternate map 分开，防止漏路径被当作完成。 */
    open val requiredAlternateFlowPaths: Set<FlowPath> get() = _requiredAlternateFlowPaths

    @CfgInternals
    open fun requireAlternateFlow(path: FlowPath) {
        check(path !== FlowPath.Default)
        _requiredAlternateFlowPaths += path
    }

    fun hasCompletedRequiredAlternateFlows(): Boolean = requiredAlternateFlowPaths.all { getAlternateFlow(it) != null }

    /** 当前节点已经记录的备用 flow 路径集合。 */
    open val alternateFlowPaths: Set<FlowPath>
        get() = _alternateFlows?.keys ?: emptySet()

    /**
     * 查询指定备用路径上的 flow。
     */
    open fun getAlternateFlow(path: FlowPath): PersistentFlow? = _alternateFlows?.get(path)

    /** 节点主载荷、备用路径及 flow 都属于保留状态，不能只访问 fir 字段。 */
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitElement(fir)
        requiredAlternateFlowPaths.forEach { path ->
            if (path is FlowPath.CfgEdge) {
                visitor.visitElement(path.fir)
                (path.label as? CFGNode<*>)?.let(visitor::visitNode)
            }
        }
        if (flowInitialized) visitor.visitFlow(flow)
        alternateFlowPaths.forEach { path ->
            if (path is FlowPath.CfgEdge) visitor.visitElement(path.fir)
            getAlternateFlow(path)?.let(visitor::visitFlow)
        }
        if (this is CFGNodeWithSubgraphs<*>) subGraphs.forEach(visitor::visitGraph)
        when (this) {
            is SplitPostponedLambdasNode -> lambdas.forEach(visitor::visitElement)
            is LoopConditionEnterNode -> visitor.visitElement(loop)
            is LoopConditionExitNode -> visitor.visitElement(loop)
            is ForInLoopGuardExitNode -> visitor.visitElement(loop)
            is ForInLoopRangeCheckNode -> { visitor.visitElement(index); visitor.visitElement(bound) }
            is ForInLoopStringCheckNode -> { visitor.visitElement(index); visitor.visitElement(bound) }
            is ForInLoopStringUpdateNode -> visitor.visitElement(index)
            is ForInLoopIteratorCheckNode -> { visitor.visitElement(resultExpression); visitor.visitElement(constructorReference); visitor.visitElement(constructorSymbol.cfir) }
            is TupleLiteralElementExitNode -> visitor.visitElement(tuple)
            is TupleLiteralExitNode -> elementExitNodes.forEach(visitor::visitNode)
            is IfExitNode -> resultSources.forEach { (node, expression) -> visitor.visitNode(node); expression?.let(visitor::visitElement) }
            else -> Unit
        }
    }

    /**
     * 添加指定备用路径上的 flow。
     */
    @CfgInternals
    open fun addAlternateFlow(path: FlowPath, flow: PersistentFlow) {
        assert(path !== FlowPath.Default) { "Cannot add default path as alternate flow for $this" }
        assert(_alternateFlows?.get(path) == null) { "Reassigning $path flow for $this" }

        val alternateFlows = _alternateFlows ?: mutableMapOf<FlowPath, PersistentFlow>().also { _alternateFlows = it }
        alternateFlows[path] = flow
    }

    /**
     * 根据入边状态重新计算节点是否死亡。
     */
    @CfgInternals
    fun updateDeadStatus() {
        isDead = if (isUnion) {
            _incomingEdges?.values?.any { it.kind.isDead } == true
        } else {
            _incomingEdges?.let { edges -> edges.size == previousNodes.size && edges.values.all { it.kind.isDead || !it.kind.usedInCfa } } == true
        }
    }

    /**
     * 从旧节点复制 CFG 关系、flow 和死亡状态。
     */
    @CfgInternals
    open fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        from.previousNodes.forEach { previousNodes += mapper[it] }
        from.followingNodes.forEach { followingNodes += mapper[it] }

        from._incomingEdges?.forEach { (node, edge) ->
            val mappedEdge = mapLabelOwner(edge, edge.label, mapper) { Edge(it, edge.kind) }
            insertIncomingEdge(mapper[node], mappedEdge)
        }

        if (fir !is CfirStub) {
            _flow = from._flow
            dataFlowAnalysisState = from.dataFlowAnalysisState
        }

        isDead = from.isDead

        from._alternateFlows?.forEach { (flowPath, flow) ->
            val mappedFlowPath = when (flowPath) {
                is FlowPath.CfgEdge -> mapLabelOwner(flowPath, flowPath.label, mapper) { FlowPath.CfgEdge(it, flowPath.fir) }
                FlowPath.Default -> flowPath
            }
            addAlternateFlow(mappedFlowPath, flow)
        }
        from.requiredAlternateFlowPaths.forEach { path ->
            val mappedPath = when (path) {
                is FlowPath.CfgEdge -> mapLabelOwner(path, path.label, mapper) { FlowPath.CfgEdge(it, path.fir) }
                FlowPath.Default -> error("Default path cannot be an alternate requirement")
            }
            requireAlternateFlow(mappedPath)
        }
    }

    /**
     * 在复制边标签时同步映射作为标签使用的 CFG 节点。
     */
    @CfgInternals
    private inline fun <T> mapLabelOwner(owner: T, label: EdgeLabel, mapper: ControlFlowNodeMapper, factory: (EdgeLabel) -> T): T {
        return if (label is CFGNode<*>) factory(mapper[label]) else owner
    }

    /**
     * 接受带返回值和数据参数的 CFG visitor。
     */
    abstract fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R

    /**
     * 接受无返回值 CFG visitor。
     */
    fun accept(visitor: ControlFlowGraphVisitorVoid) {
        accept(visitor, null)
    }
}

/**
 * CFG 复制过程中的节点/子图映射器。
 */
/** 当前节点产品阶段；未闭合的值/投影能力不得因已有默认 flow 获得正式消费授权。 */
enum class DataFlowAnalysisState {
    UNINITIALIZED,
    CONTROL_FLOW_READY,
    SEMANTICS_READY,
}

@CfgInternals
interface ControlFlowNodeMapper {
    /** 映射单个 CFG 节点。 */
    operator fun <E : CfirElement, N : CFGNode<E>> get(node: N): N

    /** 映射单个控制流子图。 */
    operator fun get(graph: ControlFlowGraph): ControlFlowGraph
}

/** 当前节点的第一个前驱节点。 */
val CFGNode<*>.firstPreviousNode: CFGNode<*> get() = previousNodes[0]

/** 当前节点的最后一个前驱节点。 */
val CFGNode<*>.lastPreviousNode: CFGNode<*> get() = previousNodes.last()

/** 指定边在当前节点死亡状态下是否参与 DFA。 */
fun CFGNode<*>.usedInDfa(edge: Edge): Boolean = if (isDead) edge.kind.usedInDeadDfa else edge.kind.usedInDfa

/** 当前节点的活跃前驱列表。 */
val CFGNode<*>.previousLiveNodes: List<CFGNode<*>>
    get() = if (isDead) previousNodes else previousNodes.filter { !it.isDead }

/** 入口节点标记。 */
interface EnterNodeMarker

/** 出口节点标记。 */
interface ExitNodeMarker

/** 图级入口节点标记。 */
interface GraphEnterNodeMarker : EnterNodeMarker

/** 图级出口节点标记。 */
interface GraphExitNodeMarker : ExitNodeMarker

/** tailrec 分析中的出口节点标记。 */
interface TailrecExitNodeMarker

/**
 * 持有子图的 CFG 节点基类。
 */
sealed class CFGNodeWithSubgraphs<out E : CfirElement>(owner: ControlFlowGraph, level: Int) : CFGNode<E>(owner, level) {
    /** 节点直接引用的子控制流图列表。 */
    abstract val subGraphs: List<ControlFlowGraph>
}

/**
 * 子图列表由 CFG 构造器显式写入的节点基类。
 */
sealed class CFGNodeWithExplicitSubgraphs<out E : CfirElement>(owner: ControlFlowGraph, level: Int) : CFGNodeWithSubgraphs<E>(owner, level) {
    /** 显式记录的子控制流图列表。 */
    @set:CfgInternals
    final override lateinit var subGraphs: List<ControlFlowGraph>

    /** 复制显式子图列表。 */
    @CfgInternals
    override fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        from as CFGNodeWithExplicitSubgraphs<*>
        super.copyData(from, mapper)
        if (from::subGraphs.isInitialized) {
            subGraphs = from.subGraphs.map(mapper::get)
        }
    }
}

/**
 * 子图由 CFIR owner 上的 controlFlowGraphReference 提供的节点基类。
 */
sealed class CFGNodeWithCfgOwner<out E : CfirControlFlowGraphOwner>(owner: ControlFlowGraph, level: Int) : CFGNodeWithSubgraphs<E>(owner, level) {
    /** 从 CFIR owner 上读取的子控制流图列表。 */
    final override val subGraphs: List<ControlFlowGraph>
        get() = listOfNotNull(fir.controlFlowGraphReference?.controlFlowGraph)

    /** 复制时确保子图也被 mapper 访问。 */
    @CfgInternals
    override fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        super.copyData(from, mapper)
        subGraphs.forEach { mapper[it] }
    }
}

/** 函数控制流图入口节点。 */
class FunctionEnterNode(owner: ControlFlowGraph, override val fir: CfirFunctionBodyOwner, level: Int) : CFGNode<CfirFunctionBodyOwner>(owner, level), GraphEnterNodeMarker {
    /**
     * 将函数入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionEnterNode(this, data)
}

/** 函数控制流图出口节点，同时作为 return 边标签。 */
class FunctionExitNode(owner: ControlFlowGraph, override val fir: CfirFunctionBodyOwner, level: Int) : CFGNode<CfirFunctionBodyOwner>(owner, level), GraphExitNodeMarker, EdgeLabel {
    /** return 路径标签。 */
    override val label: String
        get() = "return@${fir.symbol.callableId}"

    /**
     * 将函数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionExitNode(this, data)
}

/** 局部函数声明节点，节点自身持有局部函数子图。 */
class LocalFunctionDeclarationNode(owner: ControlFlowGraph, override val fir: CfirFunction, level: Int) : CFGNodeWithCfgOwner<CfirFunction>(owner, level) {
    /**
     * 将局部函数声明节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLocalFunctionDeclarationNode(this, data)
}

/** 值参数默认值或子图进入节点。 */
class EnterValueParameterNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNodeWithCfgOwner<CfirValueParameter>(owner, level) {
    /**
     * 将值参数入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitEnterValueParameterNode(this, data)
}

/** 默认参数表达式子图入口节点。 */
class EnterDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNode<CfirValueParameter>(owner, level), GraphEnterNodeMarker {
    /**
     * 将默认参数入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitEnterDefaultArgumentsNode(this, data)
}

/** 默认参数表达式子图出口节点。 */
class ExitDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNode<CfirValueParameter>(owner, level), GraphExitNodeMarker {
    /**
     * 将默认参数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitExitDefaultArgumentsNode(this, data)
}

/** 值参数处理结束节点。 */
class ExitValueParameterNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNode<CfirValueParameter>(owner, level) {
    /**
     * 将值参数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitExitValueParameterNode(this, data)
}

/**
 * 将延期 lambda 子图从主调用流中拆分出来的节点。
 *
 * @property lambdas 被拆分出来的延期 lambda 函数声明。
 */
class SplitPostponedLambdasNode(owner: ControlFlowGraph, fir: CfirStatement, val lambdas: List<CfirFunction>, level: Int) :
    CFGNodeWithSubgraphs<CfirStatement>(owner, level), RevisableFunctionCallNode {
    private val firState = FunctionCallNodeFirState(fir)
    override val fir: CfirStatement get() = firState.fir

    @CfgInternals
    override fun setResolvedFunctionCall(original: CfirFunctionCall, replacement: CfirFunctionCall) {
        firState.setResolvedFunctionCall(original, replacement)
    }

    @CfgInternals
    override fun captureFirState(): RevisableFunctionCallNodeSnapshot = firState.capture(this)

    /** 延期 lambda 对应的子控制流图。 */
    override val subGraphs: List<ControlFlowGraph>
        get() = lambdas.mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }

    /**
     * 将延期 lambda 拆分节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSplitPostponedLambdasNode(this, data)
}

/** 延期 lambda 子图出口节点。 */
class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: CfirAnonymousFunctionExpression, level: Int) :
    CFGNode<CfirAnonymousFunctionExpression>(owner, level) {
    /**
     * 将延期 lambda 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitPostponedLambdaExitNode(this, data)
}

/** 合并所有延期 lambda 出口的节点。 */
class MergePostponedLambdaExitsNode(owner: ControlFlowGraph, fir: CfirElement, level: Int) : CFGNode<CfirElement>(owner, level), RevisableFunctionCallNode {
    private val firState = FunctionCallNodeFirState(fir)
    override val fir: CfirElement get() = firState.fir

    @CfgInternals
    override fun setResolvedFunctionCall(original: CfirFunctionCall, replacement: CfirFunctionCall) {
        firState.setResolvedFunctionCall(original, replacement)
    }

    @CfgInternals
    override fun captureFirState(): RevisableFunctionCallNodeSnapshot = firState.capture(this)

    /**
     * 将延期 lambda 出口合并节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMergePostponedLambdaExitsNode(this, data)
}

/** 匿名函数捕获节点。 */
class AnonymousFunctionCaptureNode(owner: ControlFlowGraph, override val fir: CfirAnonymousFunctionExpression, level: Int) :
    CFGNode<CfirAnonymousFunctionExpression>(owner, level) {
    /**
     * 将匿名函数捕获节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitAnonymousFunctionCaptureNode(this, data)
}

/** 匿名函数表达式节点，节点持有匿名函数体子图。 */
class AnonymousFunctionExpressionNode(owner: ControlFlowGraph, override val fir: CfirAnonymousFunctionExpression, level: Int) :
    CFGNodeWithSubgraphs<CfirAnonymousFunctionExpression>(owner, level) {
    /** 匿名函数体对应的子控制流图。 */
    override val subGraphs: List<ControlFlowGraph>
        get() = listOfNotNull(fir.anonymousFunction.controlFlowGraphReference?.controlFlowGraph)

    /**
     * 将匿名函数表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitAnonymousFunctionExpressionNode(this, data)
}

/** 文件控制流图入口节点。 */
class FileEnterNode(owner: ControlFlowGraph, override val fir: CfirFile, level: Int) : CFGNodeWithExplicitSubgraphs<CfirFile>(owner, level), GraphEnterNodeMarker {
    /**
     * 将文件入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFileEnterNode(this, data)
}

/** 文件控制流图出口节点。 */
class FileExitNode(owner: ControlFlowGraph, override val fir: CfirFile, level: Int) : CFGNode<CfirFile>(owner, level), GraphExitNodeMarker {
    /**
     * 将文件出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFileExitNode(this, data)
}

/** 类控制流图入口节点。 */
class ClassEnterNode(owner: ControlFlowGraph, override val fir: CfirClass, level: Int) : CFGNodeWithExplicitSubgraphs<CfirClass>(owner, level), GraphEnterNodeMarker {
    /**
     * 将类入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitClassEnterNode(this, data)
}

/** 类控制流图出口节点。 */
class ClassExitNode(owner: ControlFlowGraph, override val fir: CfirClass, level: Int) : CFGNodeWithExplicitSubgraphs<CfirClass>(owner, level), GraphExitNodeMarker {
    /**
     * 将类出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitClassExitNode(this, data)
}

/** 局部类声明结束节点，节点通过类声明 owner 引用子图。 */
class LocalClassExitNode(owner: ControlFlowGraph, override val fir: CfirClass, level: Int) : CFGNodeWithCfgOwner<CfirClass>(owner, level) {
    /**
     * 将局部类出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLocalClassExitNode(this, data)
}

/** 代码片段控制流图入口节点。 */
class CodeFragmentEnterNode(owner: ControlFlowGraph, override val fir: CfirCodeFragment, level: Int) : CFGNode<CfirCodeFragment>(owner, level), GraphEnterNodeMarker {
    /**
     * 将代码片段入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCodeFragmentEnterNode(this, data)
}

/** 代码片段控制流图出口节点。 */
class CodeFragmentExitNode(owner: ControlFlowGraph, override val fir: CfirCodeFragment, level: Int) : CFGNode<CfirCodeFragment>(owner, level), GraphExitNodeMarker {
    /**
     * 将代码片段出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCodeFragmentExitNode(this, data)
}

/** 代码块入口节点。 */
class BlockEnterNode(owner: ControlFlowGraph, override val fir: CfirBlock, level: Int) : CFGNode<CfirBlock>(owner, level), EnterNodeMarker {
    /**
     * 将代码块入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBlockEnterNode(this, data)
}

/** 代码块出口节点。 */
class BlockExitNode(owner: ControlFlowGraph, override val fir: CfirBlock, level: Int) : CFGNode<CfirBlock>(owner, level), ExitNodeMarker {
    /**
     * 将代码块出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBlockExitNode(this, data)
}

/** match 表达式入口节点。 */
class MatchEnterNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level), EnterNodeMarker {
    /**
     * 将 match 入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchEnterNode(this, data)
}

/** match 的互斥路径出口；对位 Kotlin WhenExitNode，任一正常分支都能继续执行。 */
class MatchExitNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    /**
     * 将 match 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchExitNode(this, data)
}

/** match 分支条件入口节点。 */
class MatchBranchConditionEnterNode(
    owner: ControlFlowGraph,
    override val fir: CfirMatchBranch,
    /** 分支所属的 match；CFG 后续分析不得通过 tree identity 反查该关系。 */
    val matchExpression: CfirMatchExpression,
    level: Int,
) : CFGNode<CfirMatchBranch>(owner, level), EnterNodeMarker {
    /**
     * 将 match 分支条件入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchConditionEnterNode(this, data)
}

/** 模式成功后的 guard 求值入口；失败路径不经过该节点。 */
class MatchBranchGuardEnterNode(
    owner: ControlFlowGraph,
    override val fir: CfirMatchBranch,
    val matchExpression: CfirMatchExpression,
    level: Int,
) : CFGNode<CfirMatchBranch>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchGuardEnterNode(this, data)
}

/** 模式和 guard 均成功后的分支条件出口。 */
class MatchBranchConditionExitNode(
    owner: ControlFlowGraph,
    override val fir: CfirMatchBranch,
    /** 分支所属的 match；常量流据此取得 selector 并限定 failure 链扫描范围。 */
    val matchExpression: CfirMatchExpression,
    level: Int,
) : CFGNode<CfirMatchBranch>(owner, level), ExitNodeMarker {
    /**
     * 将 match 分支条件出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchConditionExitNode(this, data)
}

/**
 * match 模式中的一个原子判定节点。
 *
 * 一个 source-level case 可能先后检查 enum tag、嵌套 payload 和常量模式。把每个会分叉的
 * 判定保存在 CFG 中，常量流才能像 CHIR 一样选择实际 successor，并把被拒绝路径的诊断
 * 精确归属到 [reportSource]。其中 [subjectPath] 是从 match selector 到当前 payload 的索引链。
 */
class MatchPatternDecisionNode(
    owner: ControlFlowGraph,
    /** 当前原子判定所属的完整 case。 */
    val branch: CfirMatchBranch,
    /** 当前要检查的原子模式；guard 判定节点为空。 */
    val pattern: CfirPattern?,
    /** 当前 branch guard；模式判定节点为空。 */
    val guard: CfirExpression?,
    /** 该原子模式在 match subject 中的 payload 路径。 */
    val subjectPath: List<Int>,
    /** 对应 CHIR block debug location 的 source pattern。 */
    val reportSource: CfirPattern?,
    /** 当前判定所属的 match。 */
    val matchExpression: CfirMatchExpression,
    level: Int,
) : CFGNode<CfirMatchBranch>(owner, level) {
    /** 不失败模式仍保留结构节点，其 failure 边为死亡边，不代表运行时条件分支。 */
    val isAlwaysSuccessful: Boolean
        get() = guard == null && (pattern is CfirWildcardPattern ||
                pattern is CfirBindingPattern && pattern.nestedPattern == null ||
                pattern is CfirTypePattern && pattern.matchingKind == CfirTypePatternMatchingKind.ALWAYS ||
                pattern is CfirOrPattern && pattern.loweringKind() == CfirOrPatternLoweringKind.UNCONDITIONAL)

    /** guard 和合并 OR 的成功块没有模式 debug location。 */
    val successReportSource: CfirPattern?
        get() = reportSource.takeIf { guard == null && pattern !is CfirOrPattern && !isAlwaysSuccessful }

    /** CFG 节点的通用 CFIR owner 与 [branch] 保持一致。 */
    override val fir: CfirMatchBranch
        get() = branch

    /** 将原子模式判定节点分派给 CFG visitor。 */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchPatternDecisionNode(this, data)
}

/**
 * 一个 case 所有原子模式判定失败后的汇合节点。
 *
 * 该节点让后续 case 和 synthetic else 继续沿正常 CFG 构造；它本身不是条件节点，常量分析
 * 只在 [MatchPatternDecisionNode] 的 success/failure 边上作出唯一 successor 选择。
 */
class MatchBranchFailureNode(
    owner: ControlFlowGraph,
    override val fir: CfirMatchBranch,
    /** 分支所属的 match。 */
    val matchExpression: CfirMatchExpression,
    level: Int,
) : CFGNode<CfirMatchBranch>(owner, level) {
    /** 将分支失败汇合节点分派给 CFG visitor。 */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchFailureNode(this, data)
}

/** match 分支结果入口节点。 */
class MatchBranchResultEnterNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level) {
    /**
     * 将 match 分支结果入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchResultEnterNode(this, data)
}

/** match 分支结果出口节点。 */
class MatchBranchResultExitNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level), TailrecExitNodeMarker {
    /**
     * 将 match 分支结果出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchResultExitNode(this, data)
}

/** match 合成 else 分支节点。 */
class MatchSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level) {
    /**
     * 将 match 合成 else 节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchSyntheticElseBranchNode(this, data)
}

/** if 表达式入口节点。 */
class IfEnterNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, level: Int) : CFGNode<CfirIfExpression>(owner, level), EnterNodeMarker {
    /**
     * 将 if 入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfEnterNode(this, data)
}

/** 条件求值完成后的分叉；节点保留真实 if，分支标签不持有另一棵树。 */
class IfConditionExitNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, level: Int) : CFGNode<CfirIfExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfConditionExitNode(this, data)
}

/** 从条件的一条实际成功路径进入 then 或 else。 */
class IfBranchResultEnterNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, val isThen: Boolean, level: Int) : CFGNode<CfirIfExpression>(owner, level), EnterNodeMarker {
    init { require(isThen || fir.elseBranch != null) }
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfBranchResultEnterNode(this, data)
}

/** 一条分支自己的正常出口；abrupt 分支仍以 dead 输入接入汇合。 */
class IfBranchResultExitNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, val isThen: Boolean, level: Int) : CFGNode<CfirIfExpression>(owner, level), ExitNodeMarker {
    init { require(isThen || fir.elseBranch != null) }
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfBranchResultExitNode(this, data)
}

/** if 表达式出口节点，按分支共有事实合流；一条终止路径不使另一条路径死亡。 */
class IfExitNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, level: Int) : CFGNode<CfirIfExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    /** 实际then/else出口与其返回表达式，不从前驱顺序猜分支。null仅表示原语法无else的Unit。 */
    private val mutableResults = linkedMapOf<CFGNode<*>, CfirExpression?>()
    val resultSources: Map<CFGNode<*>, CfirExpression?> get() = java.util.Collections.unmodifiableMap(mutableResults)

    @CfgInternals
    fun registerResult(node: CFGNode<*>, expression: CfirExpression?) {
        check(node.owner === owner && !mutableResults.containsKey(node))
        mutableResults[node] = expression
    }

    @CfgInternals
    override fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        super.copyData(from, mapper)
        mutableResults.clear()
        (from as IfExitNode).resultSources.forEach { (node, expression) -> mutableResults[mapper[node]] = expression }
    }
    /**
     * 将 if 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfExitNode(this, data)
}

/** For 的实际执行阶段，读取/写入原有节点身份。 */
class ForInLoopInitialCheckExitNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, level: Int) :
    CFGNode<CfirForInExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopInitialCheckExitNode(this, data)
}

/** For 的实际执行阶段，读取/写入原有节点身份。 */
class ForInLoopConditionEnterNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, level: Int) :
    CFGNode<CfirForInExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopConditionEnterNode(this, data)
}

/** For 的实际执行阶段，读取/写入原有节点身份。 */
/** RANGE 条件读取 index/bound，比较节点保留原生运算的来源身份。 */
class ForInLoopRangeCheckNode(
    owner: ControlFlowGraph,
    override val fir: CfirBinaryExpression,
    val index: CfirVariable,
    val bound: CfirVariable,
    level: Int,
) : CFGNode<CfirBinaryExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopRangeCheckNode(this, data)
}

class ForInLoopStringCheckNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, val index: CfirVariable, val bound: CfirVariable, level: Int) :
    CFGNode<CfirForInExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopStringCheckNode(this, data)
}

/** For 的实际执行阶段，读取/写入原有节点身份。 */
class ForInLoopStringUpdateNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, val index: CfirVariable, level: Int) :
    CFGNode<CfirForInExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopStringUpdateNode(this, data)
}

/** For 的实际执行阶段，读取/写入原有节点身份。 */
class ForInLoopIteratorCheckNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, val resultExpression: CfirExpression, val constructorReference: CfirResolvedNamedReference, val constructorSymbol: org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>, level: Int) :
    CFGNode<CfirForInExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopIteratorCheckNode(this, data)
}

/** 完成的表达式语义节点，保留原运算或调用事实。 */
class CallExpressionNode(owner: ControlFlowGraph, override val fir: CfirCallExpression, level: Int) :
    CFGNode<CfirCallExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCallExpressionNode(this, data)
}

/** 完成的表达式语义节点，保留原运算或调用事实。 */
class UnaryExpressionNode(owner: ControlFlowGraph, override val fir: CfirUnaryExpression, level: Int) :
    CFGNode<CfirUnaryExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitUnaryExpressionNode(this, data)
}

/** 完成的表达式语义节点，保留原运算或调用事实。 */
class BinaryExpressionNode(owner: ControlFlowGraph, override val fir: CfirBinaryExpression, level: Int) :
    CFGNode<CfirBinaryExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBinaryExpressionNode(this, data)
}

/** 所有循环的真实重复入口，共用 continue/finally 的路径标签。 */
abstract class LoopRepeatEnterNode<E : CfirExpression>(owner: ControlFlowGraph, level: Int) :
    CFGNode<E>(owner, level), EnterNodeMarker, EdgeLabel {
    override val label: String get() = "continue"
}

/** For 每轮迭代准备入口；它不表达 while 条件。 */
class ForInLoopIterationEnterNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, level: Int) :
    LoopRepeatEnterNode<CfirForInExpression>(owner, level) {
    /** 前向预留的 latch 在正式进入前不能作为回边目标。 */
    var isEntered: Boolean = false
        private set
    /** 原 CFG builder 在所有前向 inputs 就绪后认领该阶段。 */
    @CfgInternals
    fun markEntered() {
        check(!isEntered)
        isEntered = true
    }
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopIterationEnterNode(this, data)
}

/** For 取得本轮元素或耗尽后的分支点。 */
class ForInLoopIterationExitNode(owner: ControlFlowGraph, override val fir: CfirForInExpression, level: Int) :
    CFGNode<CfirForInExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopIterationExitNode(this, data)
}

/** For 的真实 guard 计算出口；失败跳下一轮，成功进入 body。 */
class ForInLoopGuardExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirForInExpression, level: Int) :
    CFGNode<CfirExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitForInLoopGuardExitNode(this, data)
}

/** 循环表达式入口节点。 */
class LoopEnterNode(owner: ControlFlowGraph, override val fir: CfirIterationOwner, level: Int) : CFGNode<CfirIterationOwner>(owner, level), EnterNodeMarker {
    /**
     * 将循环入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopEnterNode(this, data)
}

/** 循环体入口节点。 */
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirIterationOwner, level: Int) : CFGNode<CfirIterationOwner>(owner, level), EnterNodeMarker {
    /**
     * 将循环体入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopBlockEnterNode(this, data)
}

/** 循环体出口节点。 */
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: CfirIterationOwner, level: Int) : CFGNode<CfirIterationOwner>(owner, level), ExitNodeMarker {
    /**
     * 将循环体出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopBlockExitNode(this, data)
}

/**
 * 循环条件入口节点，同时作为 continue 路径标签。
 *
 * @property loop 当前条件所属循环表达式。
 */
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirLoopExpression, level: Int) :
    LoopRepeatEnterNode<CfirExpression>(owner, level) {
    /**
     * 将循环条件入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopConditionEnterNode(this, data)
}

/**
 * 循环条件出口节点。
 *
 * @property loop 当前条件所属循环表达式。
 */
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirLoopExpression, level: Int) :
    CFGNode<CfirExpression>(owner, level), ExitNodeMarker {
    /**
     * 将循环条件出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopConditionExitNode(this, data)
}

/** 循环出口节点，同时作为 break 路径标签。 */
class LoopExitNode(owner: ControlFlowGraph, override val fir: CfirIterationOwner, level: Int) : CFGNode<CfirIterationOwner>(owner, level), ExitNodeMarker, EdgeLabel {
    /** break 路径标签。 */
    override val label: String get() = "break"
    /**
     * 将循环出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopExitNode(this, data)
}

/** try 表达式入口节点。 */
class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker {
    /**
     * 将 try 表达式入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryExpressionEnterNode(this, data)
}

/** try 主体块入口节点。 */
class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker {
    /**
     * 将 try 主体块入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryMainBlockEnterNode(this, data)
}

/** try 主体块出口节点。 */
class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), ExitNodeMarker {
    /**
     * 将 try 主体块出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryMainBlockExitNode(this, data)
}

/** catch 子句入口；异常成功分派以真实目标节点为标签。 */
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: CfirCatch, level: Int) : CFGNode<CfirCatch>(owner, level), EnterNodeMarker, EdgeLabel {
    override val label: String get() = "onCaughtException"
    /**
     * 将 catch 子句入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCatchClauseEnterNode(this, data)
}

/** 函数异常出口独立于正常 return 出口，不连接 lambda 的重复调用回边。 */
class ExceptionExitNode(owner: ControlFlowGraph, override val fir: CfirElement, level: Int) :
    CFGNode<CfirElement>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitExceptionExitNode(this, data)
}

/** finally 内 abrupt completion 覆盖本层原 completion 的独立程序点。 */
class CompletionOverrideNode(owner: ControlFlowGraph, val sourceNode: CFGNode<*>, level: Int) :
    CFGNode<CfirElement>(owner, level) {
    override val fir: CfirElement get() = sourceNode.fir
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitCompletionOverrideNode(this, data)
}

/** 一次异常的控制转移；节点身份同时标识跨 finally 后应恢复的异常。 */
class ExceptionTransferNode(owner: ControlFlowGraph, val sourceNode: CFGNode<*>, level: Int) :
    CFGNode<CfirElement>(owner, level), EdgeLabel {
    override val fir: CfirElement get() = sourceNode.fir
    override val label: String get() = "resume-exception"
    /** 显式 throw 有真实静态上界；调用和资源释放的异常集合由运行时确定。 */
    val exceptionType: org.cangnova.cangjie.cfir.types.ConeCangJieType?
        get() = (sourceNode.fir as? CfirThrowExpression)?.exception?.coneTypeOrNull
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitExceptionTransferNode(this, data)
}

/** 按源码顺序测试一个完整 catch union；失败才进入后续 catch。 */
class ExceptionDispatchNode(
    owner: ControlFlowGraph,
    override val fir: CfirCatch,
    val transfer: ExceptionTransferNode,
    val precedingCatches: List<CatchClauseEnterNode>,
    level: Int,
) : CFGNode<CfirCatch>(owner, level) {
    /** union 保留逐项类型；裸 wildcard 使用已解析的 Exception 类型。 */
    val acceptedTypes: List<ConeCangJieType>
        get() = fir.exceptionDispatchTypes()

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitExceptionDispatchNode(this, data)
}

/**
 * 有类型的 catch 保留有序类型列表；独立 wildcard 和错误恢复节点使用 aggregate 类型。
 * 错误类型必须保留，分派不能据此剪掉仍可达的异常路径。
 */
fun CfirCatch.exceptionDispatchTypes(): List<ConeCangJieType> {
    val pattern = pattern
    if (pattern is CfirCatchPattern && pattern.typeRefs.isNotEmpty()) {
        return pattern.typeRefs.map { checkNotNull(it.coneTypeOrNull) { "Catch type not resolved" } }
    }
    return listOf(checkNotNull(pattern.semanticTypeRef?.coneTypeOrNull) { "Catch pattern type not resolved" })
}

/**
 * 资源的隐式释放程序点：先 isClosed，未关闭时 close。
 * 此节点不是额外的源码调用；已有异常时释放失败保留原异常，否则建立首次释放异常。
 */
class ResourceCleanupNode(
    owner: ControlFlowGraph,
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirResourceVariable>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitResourceCleanupNode(this, data)
}

/** catch 子句出口节点。 */
class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: CfirCatch, level: Int) : CFGNode<CfirCatch>(owner, level), ExitNodeMarker {
    /**
     * 将 catch 子句出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCatchClauseExitNode(this, data)
}

/** effect handle 子句入口节点。 */
class HandleClauseEnterNode(owner: ControlFlowGraph, override val fir: CfirHandleClause, level: Int) : CFGNode<CfirHandleClause>(owner, level), EnterNodeMarker {
    /**
     * 将 effect handle 子句入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitHandleClauseEnterNode(this, data)
}

/** effect handle 子句出口节点。 */
class HandleClauseExitNode(owner: ControlFlowGraph, override val fir: CfirHandleClause, level: Int) : CFGNode<CfirHandleClause>(owner, level), ExitNodeMarker {
    /**
     * 将 effect handle 子句出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitHandleClauseExitNode(this, data)
}

/** finally 块入口节点。 */
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker, EdgeLabel {
    /** 穿越正在执行的 finally 时，废弃该层原 completion。 */
    override val label: String get() = "override-finally-completion"
    /**
     * 将 finally 块入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFinallyBlockEnterNode(this, data)
}

/**
 * finally 块出口节点。
 *
 * @property enterNode 与该出口成对的 finally 入口节点。
 */
class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, val enterNode: FinallyBlockEnterNode, level: Int) :
    CFGNode<CfirTryExpression>(owner, level), ExitNodeMarker {
    /**
     * 将 finally 块出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFinallyBlockExitNode(this, data)
}

/** try 表达式整体出口节点。 */
class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), ExitNodeMarker {
    /**
     * 将 try 表达式出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryExpressionExitNode(this, data)
}

/** 短路布尔运算入口节点。 */
class BooleanOperatorEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), EnterNodeMarker {
    /**
     * 将短路布尔运算入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorEnterNode(this, data)
}

/** 短路布尔运算左操作数出口节点。 */
class BooleanOperatorExitLeftOperandNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level) {
    /**
     * 将短路布尔运算左操作数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorExitLeftOperandNode(this, data)
}

/** 短路布尔运算右操作数入口节点。 */
class BooleanOperatorEnterRightOperandNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level) {
    /**
     * 将短路布尔运算右操作数入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorEnterRightOperandNode(this, data)
}

/**
 * 短路布尔运算出口节点。
 *
 * @property leftOperandNode 左操作数出口节点。
 * @property rightOperandNode 右操作数出口节点。
 */
class BooleanOperatorExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, val leftOperandNode: CFGNode<*>, val rightOperandNode: CFGNode<*>, level: Int) :
    CFGNode<CfirExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    /**
     * 将短路布尔运算出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorExitNode(this, data)
}

/** 类型操作表达式节点。 */
class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: CfirTypeOperator, level: Int) : CFGNode<CfirTypeOperator>(owner, level) {
    /**
     * 将类型操作表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTypeOperatorCallNode(this, data)
}

/** 比较表达式节点。 */
class ComparisonExpressionNode(owner: ControlFlowGraph, override val fir: CfirComparisonExpression, level: Int) : CFGNode<CfirComparisonExpression>(owner, level) {
    /**
     * 将比较表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitComparisonExpressionNode(this, data)
}

/** 跳转表达式节点。 */
class JumpNode(owner: ControlFlowGraph, override val fir: CfirJump<*>, level: Int) : CFGNode<CfirJump<*>>(owner, level), TailrecExitNodeMarker {
    /**
     * 将跳转表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitJumpNode(this, data)
}

/** 字面量表达式节点。 */
class LiteralExpressionNode(owner: ControlFlowGraph, override val fir: CfirLiteralExpression, level: Int) : CFGNode<CfirLiteralExpression>(owner, level) {
    /**
     * 将字面量表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLiteralExpressionNode(this, data)
}

/** tuple 的一次求值入口；不执行或复制任何元素表达式。 */
class TupleLiteralEnterNode(owner: ControlFlowGraph, override val fir: CfirTupleLiteral, level: Int) : CFGNode<CfirTupleLiteral>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTupleLiteralEnterNode(this, data)
}

/**
 * 原元素完成后的实际程序点。每个节点冻结自己的主/备用 flow，
 * 后面的元素即使重写同一存储，也不能改变此处已经完成的值。
 */
class TupleLiteralElementExitNode(
    owner: ControlFlowGraph,
    override val fir: CfirExpression,
    val tuple: CfirTupleLiteral,
    val index: Int,
    level: Int,
) : CFGNode<CfirExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTupleLiteralElementExitNode(this, data)
}

/** tuple 聚合完成点消费各元素的真实完成程序点，不在此重新读取它们的存储。 */
class TupleLiteralExitNode(
    owner: ControlFlowGraph,
    override val fir: CfirTupleLiteral,
    elementExitNodes: List<TupleLiteralElementExitNode>,
    level: Int,
) : CFGNode<CfirTupleLiteral>(owner, level) {
    var elementExitNodes: List<TupleLiteralElementExitNode> = elementExitNodes.toPersistentList()
        private set

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTupleLiteralExitNode(this, data)

    @CfgInternals
    override fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        super.copyData(from, mapper)
        elementExitNodes = (from as TupleLiteralExitNode).elementExitNodes.map { mapper[it] }.toPersistentList()
    }
}

/** 限定访问表达式节点。 */
class QualifiedAccessNode(owner: ControlFlowGraph, override val fir: CfirQualifiedAccessExpression, level: Int) : CFGNode<CfirQualifiedAccessExpression>(owner, level) {
    /**
     * 将限定访问表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitQualifiedAccessNode(this, data)
}

/** 函数调用的实际 CFG 承载；最终 AST replacement 只换 fir，不重建求值拓扑。 */
interface RevisableFunctionCallNode {
    val fir: CfirElement
    @CfgInternals
    fun setResolvedFunctionCall(original: CfirFunctionCall, replacement: CfirFunctionCall)
    /** 完整 AST 快照同时保存 fir 单元及其实际 graph 索引成员资格。 */
    @CfgInternals
    fun captureFirState(): RevisableFunctionCallNodeSnapshot
}

/** 索引事实属于实际 CFG；隔离图被正式 AST 采用后仍有同一可修订承载。 */
interface FunctionCallNodeIndexOwner {
    @CfgInternals
    fun captureFunctionCallNodeIndex(node: CFGNode<*>): RevisableFunctionCallNodeSnapshot
}

/** 在任何 AST/cache 写入前验证实际节点归属，再原位恢复 fir 及 alias 成员。 */
interface RevisableFunctionCallNodeSnapshot {
    @CfgInternals
    fun validateRestore()
    @CfgInternals
    fun restore()
}

/** 三类节点共享 fir/index memento；非 call 的 postponed 节点只保存自身 fir。 */
private class FunctionCallNodeFirState<E : CfirElement>(initialFir: E) {
    var fir: E = initialFir
        private set

    @CfgInternals
    fun setResolvedFunctionCall(original: CfirFunctionCall, replacement: E) {
        check(fir === original) { "CFG replacement changed its actual call owner" }
        fir = replacement
    }

    @CfgInternals
    fun capture(node: CFGNode<*>): RevisableFunctionCallNodeSnapshot {
        check(node.fir === fir) { "CFG fir snapshot must belong to the actual node" }
        val before = fir
        val indexState = if (before is CfirFunctionCall) node.owner.captureFunctionCallNodeIndex(node) else null
        return object : RevisableFunctionCallNodeSnapshot {
            override fun validateRestore() {
                indexState?.validateRestore()
            }

            override fun restore() {
                validateRestore()
                indexState?.restore()
                fir = before
            }
        }
    }
}

/** 对位 Kotlin CFGNodeWithRevisableFunctionCall；仓颉另允许已解析 nested call 的原位替换。 */
sealed class CFGNodeWithRevisableFunctionCall(
    owner: ControlFlowGraph,
    fir: CfirFunctionCall,
    level: Int,
) : CFGNode<CfirFunctionCall>(owner, level), RevisableFunctionCallNode {
    private val firState = FunctionCallNodeFirState(fir)
    final override val fir: CfirFunctionCall get() = firState.fir

    @CfgInternals
    final override fun setResolvedFunctionCall(original: CfirFunctionCall, replacement: CfirFunctionCall) {
        firState.setResolvedFunctionCall(original, replacement)
    }

    @CfgInternals
    final override fun captureFirState(): RevisableFunctionCallNodeSnapshot = firState.capture(this)
}

/** 函数调用参数求值入口节点。 */
class FunctionCallArgumentsEnterNode(owner: ControlFlowGraph, fir: CfirFunctionCall, level: Int) : CFGNodeWithRevisableFunctionCall(owner, fir, level), EnterNodeMarker {
    /**
     * 将函数调用参数入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallArgumentsEnterNode(this, data)
}

/**
 * 函数调用参数求值出口节点。
 *
 * @property explicitReceiverExitNode 显式接收者求值出口节点。
 */
class FunctionCallArgumentsExitNode(owner: ControlFlowGraph, fir: CfirFunctionCall, var explicitReceiverExitNode: CFGNode<*>, level: Int) :
    CFGNodeWithRevisableFunctionCall(owner, fir, level), ExitNodeMarker {
    /**
     * 将函数调用参数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallArgumentsExitNode(this, data)
}

/** 函数调用执行入口节点。 */
class FunctionCallEnterNode(owner: ControlFlowGraph, fir: CfirFunctionCall, level: Int) : CFGNodeWithRevisableFunctionCall(owner, fir, level) {
    /**
     * 将函数调用执行入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallEnterNode(this, data)
}

/** 函数调用执行出口节点，按 union 语义汇合正常和特殊调用路径。 */
class FunctionCallExitNode(owner: ControlFlowGraph, fir: CfirFunctionCall, level: Int) : CFGNodeWithRevisableFunctionCall(owner, fir, level) {
    /** 调用出口需要 union 合并。 */
    override val isUnion: Boolean
        get() = true

    /**
     * 将函数调用执行出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallExitNode(this, data)
}

/** throw 表达式节点。 */
class ThrowExceptionNode(owner: ControlFlowGraph, override val fir: CfirThrowExpression, level: Int) : CFGNode<CfirThrowExpression>(owner, level) {
    /**
     * 将 throw 表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitThrowExceptionNode(this, data)
}

/** 变量声明入口节点。 */
class VariableDeclarationEnterNode(owner: ControlFlowGraph, override val fir: CfirVariable, level: Int) : CFGNode<CfirVariable>(owner, level) {
    /**
     * 将变量声明入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitVariableDeclarationEnterNode(this, data)
}

/** 变量声明出口节点。 */
class VariableDeclarationExitNode(owner: ControlFlowGraph, override val fir: CfirVariable, level: Int) : CFGNode<CfirVariable>(owner, level) {
    /**
     * 将变量声明出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitVariableDeclarationExitNode(this, data)
}

/** 变量赋值节点。 */
class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: CfirAssignment, level: Int) : CFGNode<CfirAssignment>(owner, level) {
    /**
     * 将变量赋值节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitVariableAssignmentNode(this, data)
}

/**
 * 自增/自减表达式的写入完成节点。
 *
 * `++` / `--` 在 CFIR 中保留为专用语法节点，而不是普通 [CfirAssignment]。该节点把它
 * 对左值的真实写入显式放入 CFG，避免局部状态分析把 `i++` 误当作纯读取。
 */
class IncrementDecrementNode(
    owner: ControlFlowGraph,
    override val fir: CfirIncrementDecrementExpression,
    level: Int,
) : CFGNode<CfirIncrementDecrementExpression>(owner, level) {
    /** 将自增/自减写入分派给 CFG visitor。 */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitIncrementDecrementNode(this, data)
}

/** optional chain 入口节点。 */
class EnterOptionalChainNode(owner: ControlFlowGraph, override val fir: CfirOptionalChainExpression, level: Int) : CFGNode<CfirOptionalChainExpression>(owner, level) {
    /**
     * 将 optional chain 入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitEnterOptionalChainNode(this, data)
}

/** optional chain 出口节点。 */
class ExitOptionalChainNode(owner: ControlFlowGraph, override val fir: CfirOptionalChainExpression, level: Int) : CFGNode<CfirOptionalChainExpression>(owner, level), TailrecExitNodeMarker {
    /**
     * 将 optional chain 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitExitOptionalChainNode(this, data)
}

/** 包装表达式节点。 */
class WrappedExpressionNode(owner: ControlFlowGraph, override val fir: CfirWrappedExpression, level: Int) : CFGNode<CfirWrappedExpression>(owner, level) {
    /**
     * 将包装表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitWrappedExpressionNode(this, data)
}

/** CFG 构造中用于占位的死亡 stub 节点。 */
class StubNode(owner: ControlFlowGraph, level: Int) : CFGNode<CfirStub>(owner, level) {
    init {
        isDead = true
    }

    /**
     * stub 节点绑定的合成 CFIR 表达式。
     */
    override val fir: CfirStub get() = CfirStub

    /**
     * stub 节点复用第一个前驱节点的 flow 初始化状态。
     */
    override val flowInitialized: Boolean
        get() = firstPreviousNode.flowInitialized

    override var dataFlowAnalysisState: DataFlowAnalysisState
        get() = firstPreviousNode.dataFlowAnalysisState
        @CfgInternals set(_) = throw IllegalStateException("Cannot set analysis state for stub node")

    /**
     * stub 节点透传第一个前驱节点的主 flow。
     */
    override var flow: PersistentFlow
        get() = firstPreviousNode.flow
        @CfgInternals
        set(_) = throw IllegalStateException("Cannot set flow for stub node")

    /**
     * stub 节点透传第一个前驱节点的备用 flow 路径集合。
     */
    override val alternateFlowPaths: Set<FlowPath>
        get() = firstPreviousNode.alternateFlowPaths

    override val requiredAlternateFlowPaths: Set<FlowPath>
        get() = firstPreviousNode.requiredAlternateFlowPaths

    @CfgInternals
    override fun requireAlternateFlow(path: FlowPath) = firstPreviousNode.requireAlternateFlow(path)

    /**
     * 从第一个前驱节点读取指定备用 flow。
     */
    override fun getAlternateFlow(path: FlowPath): PersistentFlow? = firstPreviousNode.getAlternateFlow(path)

    /**
     * 将备用 flow 写入透传给第一个前驱节点。
     */
    @CfgInternals
    override fun addAlternateFlow(path: FlowPath, flow: PersistentFlow) {
        firstPreviousNode.addAlternateFlow(path, flow)
    }

    /**
     * 将 stub 节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitStubNode(this, data)
}

/** stub 节点绑定的合成表达式。 */
object CfirStub : CfirExpression() {
    override val mappedExpression: CfirExpression? get() = null
    override val overflowStrategy: org.cangnova.cangjie.annotations.CangjieOverflowStrategy? get() = null
    override fun replaceMappedExpression(newMappedExpression: CfirExpression?) { require(newMappedExpression == null) }
    override fun replaceOverflowStrategy(newOverflowStrategy: org.cangnova.cangjie.annotations.CangjieOverflowStrategy?) {
        require(newOverflowStrategy == null)
    }
    /** stub 没有源码位置。 */
    override val source: CjSourceElement? get() = null
    /** 全局stub不拥有任何解析代次的源锚点。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        require(newSource == null)
    }
    /** stub 没有类型。 */
    override val coneTypeOrNull = null
    /** stub 没有注解。 */
    override val annotations: List<CfirAnnotation> get() = emptyList()

    /** stub 没有子节点。 */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}
    /** stub 没有可变换注解。 */
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression = this
    /** stub 没有可变换子节点。 */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement = this
    /** stub 不允许写入非空注解。 */
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) { assert(newAnnotations.isEmpty()) }
    /** stub 不允许写入类型。 */
    override fun replaceConeTypeOrNull(newConeTypeOrNull: org.cangnova.cangjie.cfir.types.ConeCangJieType?) { assert(newConeTypeOrNull == null) }
}

/** 构造器临时使用的假表达式入口节点。 */
class FakeExpressionEnterNode(owner: ControlFlowGraph, level: Int) : CFGNode<CfirStub>(owner, level), GraphEnterNodeMarker, GraphExitNodeMarker {
    init {
        isDead = true
    }

    /**
     * 假表达式入口节点绑定的合成 stub 表达式。
     */
    override val fir: CfirStub get() = CfirStub

    /**
     * 将假表达式入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFakeExpressionEnterNode(this, data)
}

// ----------------------------------- Field initializer -----------------------------------
// 对齐仓颉 `let x: T = expr` / `var x: T = expr` 字段初始化子图。
// 仓颉 `prop` 仅声明 getter/setter 无 initializer,故不建模 PropertyInitializer*Node。

/** 字段初始化子图入口节点。 */
class FieldInitializerEnterNode(
    owner: ControlFlowGraph,
    /**
     * 该字段初始化子图对应的字段变量声明。
     */
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirFieldVariable>(owner, level), GraphEnterNodeMarker {
    /**
     * 将字段初始化入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFieldInitializerEnterNode(this, data)
}

/** 字段初始化子图出口节点。 */
class FieldInitializerExitNode(
    owner: ControlFlowGraph,
    /**
     * 该字段初始化子图对应的字段变量声明。
     */
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirFieldVariable>(owner, level), GraphExitNodeMarker {
    /**
     * 将字段初始化出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFieldInitializerExitNode(this, data)
}

/** 资源 initializer 独立执行根入口；实际资源声明保持原身份。 */
class ResourceInitializerEnterNode(
    owner: ControlFlowGraph,
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirResourceVariable>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitResourceInitializerEnterNode(this, data)
}

/** 资源 initializer 独立执行根出口；关闭 CFG 不授予 Source/FULL/lease 权限。 */
class ResourceInitializerExitNode(
    owner: ControlFlowGraph,
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirResourceVariable>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R =
        visitor.visitResourceInitializerExitNode(this, data)
}

// ----------------------------------- Spawn (仓颉协程) -----------------------------------
// `spawn { body }` 启动新线程/协程执行 body,主控制流直接继续到 spawn 表达式结果。
// body 作为独立子图存在,由 CfirAnonymousFunction 持有 controlFlowGraphReference。

/** 仓颉 `spawn` 表达式节点。 */
class SpawnExpressionNode(
    owner: ControlFlowGraph,
    /**
     * 当前节点代表的 spawn 表达式。
     */
    override val fir: CfirSpawnExpression,
    level: Int,
) : CFGNode<CfirSpawnExpression>(owner, level) {
    /**
     * 将 spawn 表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSpawnExpressionNode(this, data)
}

// ----------------------------------- Synchronized (仓颉同步块) -----------------------------------
// `synchronized(lock) { body }` 获取互斥锁→执行 body→无论正常/异常都释放。
// 语义上 Enter/Exit 成对,Exit 作为 union 节点汇合正常路径与潜在抛出路径。

/** 仓颉 `synchronized` 表达式入口节点。 */
class SynchronizedEnterNode(
    owner: ControlFlowGraph,
    /**
     * 当前节点代表的 synchronized 表达式。
     */
    override val fir: CfirSynchronizedExpression,
    level: Int,
) : CFGNode<CfirSynchronizedExpression>(owner, level), EnterNodeMarker {
    /**
     * 将 synchronized 入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSynchronizedEnterNode(this, data)
}

/** 仓颉 `synchronized` 表达式出口节点。 */
class SynchronizedExitNode(
    owner: ControlFlowGraph,
    /**
     * 当前节点代表的 synchronized 表达式。
     */
    override val fir: CfirSynchronizedExpression,
    level: Int,
) : CFGNode<CfirSynchronizedExpression>(owner, level), ExitNodeMarker {
    /**
     * 将 synchronized 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSynchronizedExitNode(this, data)
}

// ----------------------------------- Unsafe (仓颉不安全块) -----------------------------------
// `unsafe { body }` 只切换编译器的 unsafe 上下文标记,不引入实际控制流变化,
// 但为 DFA 提供独立的边界节点以区分 unsafe 区域内/外。

/** 仓颉 `unsafe` 表达式入口节点。 */
class UnsafeEnterNode(
    owner: ControlFlowGraph,
    /**
     * 当前节点代表的 unsafe 表达式。
     */
    override val fir: CfirUnsafeExpression,
    level: Int,
) : CFGNode<CfirUnsafeExpression>(owner, level), EnterNodeMarker {
    /**
     * 将 unsafe 入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitUnsafeEnterNode(this, data)
}

/** 仓颉 `unsafe` 表达式出口节点。 */
class UnsafeExitNode(
    owner: ControlFlowGraph,
    /**
     * 当前节点代表的 unsafe 表达式。
     */
    override val fir: CfirUnsafeExpression,
    level: Int,
) : CFGNode<CfirUnsafeExpression>(owner, level), ExitNodeMarker {
    /**
     * 将 unsafe 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitUnsafeExitNode(this, data)
}
