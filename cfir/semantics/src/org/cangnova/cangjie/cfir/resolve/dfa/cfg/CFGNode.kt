@file:Suppress("Reformat")

package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.CfirElement
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
import org.cangnova.cangjie.cfir.resolve.dfa.FlowPath
import org.cangnova.cangjie.cfir.resolve.dfa.PersistentFlow
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
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

    /** 当前节点已经记录的备用 flow 路径集合。 */
    open val alternateFlowPaths: Set<FlowPath>
        get() = _alternateFlows?.keys ?: emptySet()

    /**
     * 查询指定备用路径上的 flow。
     */
    open fun getAlternateFlow(path: FlowPath): PersistentFlow? = _alternateFlows?.get(path)

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
        }

        isDead = from.isDead

        from._alternateFlows?.forEach { (flowPath, flow) ->
            val mappedFlowPath = when (flowPath) {
                is FlowPath.CfgEdge -> mapLabelOwner(flowPath, flowPath.label, mapper) { FlowPath.CfgEdge(it, flowPath.fir) }
                FlowPath.Default -> flowPath
            }
            addAlternateFlow(mappedFlowPath, flow)
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
class FunctionEnterNode(owner: ControlFlowGraph, override val fir: CfirFunction, level: Int) : CFGNode<CfirFunction>(owner, level), GraphEnterNodeMarker {
    /**
     * 将函数入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionEnterNode(this, data)
}

/** 函数控制流图出口节点，同时作为 return 边标签。 */
class FunctionExitNode(owner: ControlFlowGraph, override val fir: CfirFunction, level: Int) : CFGNode<CfirFunction>(owner, level), GraphExitNodeMarker, EdgeLabel {
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
class SplitPostponedLambdasNode(owner: ControlFlowGraph, override val fir: CfirStatement, val lambdas: List<CfirFunction>, level: Int) :
    CFGNodeWithSubgraphs<CfirStatement>(owner, level) {
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
class MergePostponedLambdaExitsNode(owner: ControlFlowGraph, override val fir: CfirElement, level: Int) : CFGNode<CfirElement>(owner, level) {
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

/** match 表达式出口节点，按 union 语义合并各分支。 */
class MatchExitNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    /** match 分支出口需要 union 合并。 */
    override val isUnion: Boolean
        get() = true

    /**
     * 将 match 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchExitNode(this, data)
}

/** match 分支条件入口节点。 */
class MatchBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level), EnterNodeMarker {
    /**
     * 将 match 分支条件入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchConditionEnterNode(this, data)
}

/** match 分支条件出口节点。 */
class MatchBranchConditionExitNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level), ExitNodeMarker {
    /**
     * 将 match 分支条件出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchConditionExitNode(this, data)
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

/** if 表达式出口节点，按 union 语义合并 then/else。 */
class IfExitNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, level: Int) : CFGNode<CfirIfExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    /** if 两个分支出口需要 union 合并。 */
    override val isUnion: Boolean
        get() = true

    /**
     * 将 if 出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfExitNode(this, data)
}

/** 循环表达式入口节点。 */
class LoopEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), EnterNodeMarker {
    /**
     * 将循环入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopEnterNode(this, data)
}

/** 循环体入口节点。 */
class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), EnterNodeMarker {
    /**
     * 将循环体入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopBlockEnterNode(this, data)
}

/** 循环体出口节点。 */
class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), ExitNodeMarker {
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
class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirExpression, level: Int) :
    CFGNode<CfirExpression>(owner, level), EnterNodeMarker, EdgeLabel {
    /** continue 路径标签。 */
    override val label: String get() = "continue"
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
class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirExpression, level: Int) :
    CFGNode<CfirExpression>(owner, level), ExitNodeMarker {
    /**
     * 将循环条件出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopConditionExitNode(this, data)
}

/** 循环出口节点，同时作为 break 路径标签。 */
class LoopExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), ExitNodeMarker, EdgeLabel {
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

/** catch 子句入口节点。 */
class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: CfirCatch, level: Int) : CFGNode<CfirCatch>(owner, level), EnterNodeMarker {
    /**
     * 将 catch 子句入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCatchClauseEnterNode(this, data)
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
class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker {
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
class BooleanOperatorEnterNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, level: Int) : CFGNode<CfirBinaryOp>(owner, level), EnterNodeMarker {
    /**
     * 将短路布尔运算入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorEnterNode(this, data)
}

/** 短路布尔运算左操作数出口节点。 */
class BooleanOperatorExitLeftOperandNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, level: Int) : CFGNode<CfirBinaryOp>(owner, level) {
    /**
     * 将短路布尔运算左操作数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorExitLeftOperandNode(this, data)
}

/** 短路布尔运算右操作数入口节点。 */
class BooleanOperatorEnterRightOperandNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, level: Int) : CFGNode<CfirBinaryOp>(owner, level) {
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
class BooleanOperatorExitNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, val leftOperandNode: CFGNode<*>, val rightOperandNode: CFGNode<*>, level: Int) :
    CFGNode<CfirBinaryOp>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
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

/** 限定访问表达式节点。 */
class QualifiedAccessNode(owner: ControlFlowGraph, override val fir: CfirQualifiedAccessExpression, level: Int) : CFGNode<CfirQualifiedAccessExpression>(owner, level) {
    /**
     * 将限定访问表达式节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitQualifiedAccessNode(this, data)
}

/** 函数调用参数求值入口节点。 */
class FunctionCallArgumentsEnterNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, level: Int) : CFGNode<CfirFunctionCall>(owner, level), EnterNodeMarker {
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
class FunctionCallArgumentsExitNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, var explicitReceiverExitNode: CFGNode<*>, level: Int) :
    CFGNode<CfirFunctionCall>(owner, level), ExitNodeMarker {
    /**
     * 将函数调用参数出口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallArgumentsExitNode(this, data)
}

/** 函数调用执行入口节点。 */
class FunctionCallEnterNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, level: Int) : CFGNode<CfirFunctionCall>(owner, level) {
    /**
     * 将函数调用执行入口节点分派给 CFG visitor。
     */
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallEnterNode(this, data)
}

/** 函数调用执行出口节点，按 union 语义汇合正常和特殊调用路径。 */
class FunctionCallExitNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, level: Int) : CFGNode<CfirFunctionCall>(owner, level) {
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
    /** stub 没有源码位置。 */
    override val source: CjSourceElement? get() = null
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
