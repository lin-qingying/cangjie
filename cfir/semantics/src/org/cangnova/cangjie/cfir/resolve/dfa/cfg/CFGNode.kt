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

sealed class CFGNode<out E : CfirElement>(val owner: ControlFlowGraph, val level: Int) {
    @OptIn(CfgInternals::class)
    val id: Int = owner.nodeCount++

    open val isUnion: Boolean get() = false

    companion object {
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

        @CfgInternals
        fun removeAllOutgoingEdges(from: CFGNode<*>) {
            for (to in from.followingNodes) {
                to.previousNodes.remove(from)
                to._incomingEdges?.remove(from)
            }
            from.followingNodes.clear()
        }

        @CfgInternals
        fun removeAllIncomingEdges(to: CFGNode<*>) {
            for (from in to.previousNodes) {
                from.followingNodes.remove(to)
            }
            to.previousNodes.clear()
            to._incomingEdges?.clear()
        }
    }

    val previousNodes: MutableList<CFGNode<*>> = SmartList()
    val followingNodes: MutableList<CFGNode<*>> = SmartList()

    internal var _incomingEdges: MutableMap<CFGNode<*>, Edge>? = null

    private fun insertIncomingEdge(from: CFGNode<*>, edge: Edge) {
        val map = _incomingEdges
        if (map != null) {
            map[from] = edge
        } else {
            _incomingEdges = mutableMapOf(from to edge)
        }
    }

    fun edgeFrom(other: CFGNode<*>): Edge = _incomingEdges?.get(other) ?: Edge.Normal_Forward
    fun edgeTo(other: CFGNode<*>): Edge = other.edgeFrom(this)

    abstract val fir: E
    var isDead: Boolean = false
        protected set

    private var _flow: PersistentFlow? = null
    open val flowInitialized: Boolean get() = _flow != null
    open var flow: PersistentFlow
        get() = _flow ?: throw IllegalStateException("Flow for $this is not initialized")
        @CfgInternals
        set(value) {
            assert(_flow == null) { "Reassigning flow for $this" }
            _flow = value
        }

    private var _alternateFlows: MutableMap<FlowPath, PersistentFlow>? = null
    open val alternateFlowPaths: Set<FlowPath>
        get() = _alternateFlows?.keys ?: emptySet()

    open fun getAlternateFlow(path: FlowPath): PersistentFlow? = _alternateFlows?.get(path)

    @CfgInternals
    open fun addAlternateFlow(path: FlowPath, flow: PersistentFlow) {
        assert(path !== FlowPath.Default) { "Cannot add default path as alternate flow for $this" }
        assert(_alternateFlows?.get(path) == null) { "Reassigning $path flow for $this" }

        val alternateFlows = _alternateFlows ?: mutableMapOf<FlowPath, PersistentFlow>().also { _alternateFlows = it }
        alternateFlows[path] = flow
    }

    @CfgInternals
    fun updateDeadStatus() {
        isDead = if (isUnion) {
            _incomingEdges?.values?.any { it.kind.isDead } == true
        } else {
            _incomingEdges?.let { edges -> edges.size == previousNodes.size && edges.values.all { it.kind.isDead || !it.kind.usedInCfa } } == true
        }
    }

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

    @CfgInternals
    private inline fun <T> mapLabelOwner(owner: T, label: EdgeLabel, mapper: ControlFlowNodeMapper, factory: (EdgeLabel) -> T): T {
        return if (label is CFGNode<*>) factory(mapper[label]) else owner
    }

    abstract fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R

    fun accept(visitor: ControlFlowGraphVisitorVoid) {
        accept(visitor, null)
    }
}

@CfgInternals
interface ControlFlowNodeMapper {
    operator fun <E : CfirElement, N : CFGNode<E>> get(node: N): N
    operator fun get(graph: ControlFlowGraph): ControlFlowGraph
}

val CFGNode<*>.firstPreviousNode: CFGNode<*> get() = previousNodes[0]
val CFGNode<*>.lastPreviousNode: CFGNode<*> get() = previousNodes.last()
fun CFGNode<*>.usedInDfa(edge: Edge): Boolean = if (isDead) edge.kind.usedInDeadDfa else edge.kind.usedInDfa
val CFGNode<*>.previousLiveNodes: List<CFGNode<*>>
    get() = if (isDead) previousNodes else previousNodes.filter { !it.isDead }

interface EnterNodeMarker
interface ExitNodeMarker
interface GraphEnterNodeMarker : EnterNodeMarker
interface GraphExitNodeMarker : ExitNodeMarker
interface TailrecExitNodeMarker

sealed class CFGNodeWithSubgraphs<out E : CfirElement>(owner: ControlFlowGraph, level: Int) : CFGNode<E>(owner, level) {
    abstract val subGraphs: List<ControlFlowGraph>
}

sealed class CFGNodeWithExplicitSubgraphs<out E : CfirElement>(owner: ControlFlowGraph, level: Int) : CFGNodeWithSubgraphs<E>(owner, level) {
    @set:CfgInternals
    final override lateinit var subGraphs: List<ControlFlowGraph>

    @CfgInternals
    override fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        from as CFGNodeWithExplicitSubgraphs<*>
        super.copyData(from, mapper)
        if (from::subGraphs.isInitialized) {
            subGraphs = from.subGraphs.map(mapper::get)
        }
    }
}

sealed class CFGNodeWithCfgOwner<out E : CfirControlFlowGraphOwner>(owner: ControlFlowGraph, level: Int) : CFGNodeWithSubgraphs<E>(owner, level) {
    final override val subGraphs: List<ControlFlowGraph>
        get() = listOfNotNull(fir.controlFlowGraphReference?.controlFlowGraph)

    @CfgInternals
    override fun copyData(from: CFGNode<*>, mapper: ControlFlowNodeMapper) {
        super.copyData(from, mapper)
        subGraphs.forEach { mapper[it] }
    }
}

class FunctionEnterNode(owner: ControlFlowGraph, override val fir: CfirFunction, level: Int) : CFGNode<CfirFunction>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionEnterNode(this, data)
}

class FunctionExitNode(owner: ControlFlowGraph, override val fir: CfirFunction, level: Int) : CFGNode<CfirFunction>(owner, level), GraphExitNodeMarker, EdgeLabel {
    override val label: String
        get() = "return@${fir.symbol.callableId}"

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionExitNode(this, data)
}

class LocalFunctionDeclarationNode(owner: ControlFlowGraph, override val fir: CfirFunction, level: Int) : CFGNodeWithCfgOwner<CfirFunction>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLocalFunctionDeclarationNode(this, data)
}

class EnterValueParameterNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNodeWithCfgOwner<CfirValueParameter>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitEnterValueParameterNode(this, data)
}

class EnterDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNode<CfirValueParameter>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitEnterDefaultArgumentsNode(this, data)
}

class ExitDefaultArgumentsNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNode<CfirValueParameter>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitExitDefaultArgumentsNode(this, data)
}

class ExitValueParameterNode(owner: ControlFlowGraph, override val fir: CfirValueParameter, level: Int) : CFGNode<CfirValueParameter>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitExitValueParameterNode(this, data)
}

class SplitPostponedLambdasNode(owner: ControlFlowGraph, override val fir: CfirStatement, val lambdas: List<CfirFunction>, level: Int) :
    CFGNodeWithSubgraphs<CfirStatement>(owner, level) {
    override val subGraphs: List<ControlFlowGraph>
        get() = lambdas.mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSplitPostponedLambdasNode(this, data)
}

class PostponedLambdaExitNode(owner: ControlFlowGraph, override val fir: CfirAnonymousFunctionExpression, level: Int) :
    CFGNode<CfirAnonymousFunctionExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitPostponedLambdaExitNode(this, data)
}

class MergePostponedLambdaExitsNode(owner: ControlFlowGraph, override val fir: CfirElement, level: Int) : CFGNode<CfirElement>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMergePostponedLambdaExitsNode(this, data)
}

class AnonymousFunctionCaptureNode(owner: ControlFlowGraph, override val fir: CfirAnonymousFunctionExpression, level: Int) :
    CFGNode<CfirAnonymousFunctionExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitAnonymousFunctionCaptureNode(this, data)
}

class AnonymousFunctionExpressionNode(owner: ControlFlowGraph, override val fir: CfirAnonymousFunctionExpression, level: Int) :
    CFGNodeWithSubgraphs<CfirAnonymousFunctionExpression>(owner, level) {
    override val subGraphs: List<ControlFlowGraph>
        get() = listOfNotNull(fir.anonymousFunction.controlFlowGraphReference?.controlFlowGraph)

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitAnonymousFunctionExpressionNode(this, data)
}

class FileEnterNode(owner: ControlFlowGraph, override val fir: CfirFile, level: Int) : CFGNodeWithExplicitSubgraphs<CfirFile>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFileEnterNode(this, data)
}

class FileExitNode(owner: ControlFlowGraph, override val fir: CfirFile, level: Int) : CFGNode<CfirFile>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFileExitNode(this, data)
}

class ClassEnterNode(owner: ControlFlowGraph, override val fir: CfirClass, level: Int) : CFGNodeWithExplicitSubgraphs<CfirClass>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitClassEnterNode(this, data)
}

class ClassExitNode(owner: ControlFlowGraph, override val fir: CfirClass, level: Int) : CFGNodeWithExplicitSubgraphs<CfirClass>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitClassExitNode(this, data)
}

class LocalClassExitNode(owner: ControlFlowGraph, override val fir: CfirClass, level: Int) : CFGNodeWithCfgOwner<CfirClass>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLocalClassExitNode(this, data)
}

class CodeFragmentEnterNode(owner: ControlFlowGraph, override val fir: CfirCodeFragment, level: Int) : CFGNode<CfirCodeFragment>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCodeFragmentEnterNode(this, data)
}

class CodeFragmentExitNode(owner: ControlFlowGraph, override val fir: CfirCodeFragment, level: Int) : CFGNode<CfirCodeFragment>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCodeFragmentExitNode(this, data)
}

class BlockEnterNode(owner: ControlFlowGraph, override val fir: CfirBlock, level: Int) : CFGNode<CfirBlock>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBlockEnterNode(this, data)
}

class BlockExitNode(owner: ControlFlowGraph, override val fir: CfirBlock, level: Int) : CFGNode<CfirBlock>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBlockExitNode(this, data)
}

class MatchEnterNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchEnterNode(this, data)
}

class MatchExitNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchExitNode(this, data)
}

class MatchBranchConditionEnterNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchConditionEnterNode(this, data)
}

class MatchBranchConditionExitNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchConditionExitNode(this, data)
}

class MatchBranchResultEnterNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchResultEnterNode(this, data)
}

class MatchBranchResultExitNode(owner: ControlFlowGraph, override val fir: CfirMatchBranch, level: Int) : CFGNode<CfirMatchBranch>(owner, level), TailrecExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchBranchResultExitNode(this, data)
}

class MatchSyntheticElseBranchNode(owner: ControlFlowGraph, override val fir: CfirMatchExpression, level: Int) : CFGNode<CfirMatchExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitMatchSyntheticElseBranchNode(this, data)
}

class IfEnterNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, level: Int) : CFGNode<CfirIfExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfEnterNode(this, data)
}

class IfExitNode(owner: ControlFlowGraph, override val fir: CfirIfExpression, level: Int) : CFGNode<CfirIfExpression>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitIfExitNode(this, data)
}

class LoopEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopEnterNode(this, data)
}

class LoopBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopBlockEnterNode(this, data)
}

class LoopBlockExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopBlockExitNode(this, data)
}

class LoopConditionEnterNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirExpression, level: Int) :
    CFGNode<CfirExpression>(owner, level), EnterNodeMarker, EdgeLabel {
    override val label: String get() = "continue"
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopConditionEnterNode(this, data)
}

class LoopConditionExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, val loop: CfirExpression, level: Int) :
    CFGNode<CfirExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopConditionExitNode(this, data)
}

class LoopExitNode(owner: ControlFlowGraph, override val fir: CfirExpression, level: Int) : CFGNode<CfirExpression>(owner, level), ExitNodeMarker, EdgeLabel {
    override val label: String get() = "break"
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLoopExitNode(this, data)
}

class TryExpressionEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryExpressionEnterNode(this, data)
}

class TryMainBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryMainBlockEnterNode(this, data)
}

class TryMainBlockExitNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryMainBlockExitNode(this, data)
}

class CatchClauseEnterNode(owner: ControlFlowGraph, override val fir: CfirCatch, level: Int) : CFGNode<CfirCatch>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCatchClauseEnterNode(this, data)
}

class CatchClauseExitNode(owner: ControlFlowGraph, override val fir: CfirCatch, level: Int) : CFGNode<CfirCatch>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitCatchClauseExitNode(this, data)
}

class FinallyBlockEnterNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFinallyBlockEnterNode(this, data)
}

class FinallyBlockExitNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, val enterNode: FinallyBlockEnterNode, level: Int) :
    CFGNode<CfirTryExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFinallyBlockExitNode(this, data)
}

class TryExpressionExitNode(owner: ControlFlowGraph, override val fir: CfirTryExpression, level: Int) : CFGNode<CfirTryExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTryExpressionExitNode(this, data)
}

class BooleanOperatorEnterNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, level: Int) : CFGNode<CfirBinaryOp>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorEnterNode(this, data)
}

class BooleanOperatorExitLeftOperandNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, level: Int) : CFGNode<CfirBinaryOp>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorExitLeftOperandNode(this, data)
}

class BooleanOperatorEnterRightOperandNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, level: Int) : CFGNode<CfirBinaryOp>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorEnterRightOperandNode(this, data)
}

class BooleanOperatorExitNode(owner: ControlFlowGraph, override val fir: CfirBinaryOp, val leftOperandNode: CFGNode<*>, val rightOperandNode: CFGNode<*>, level: Int) :
    CFGNode<CfirBinaryOp>(owner, level), ExitNodeMarker, TailrecExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitBooleanOperatorExitNode(this, data)
}

class TypeOperatorCallNode(owner: ControlFlowGraph, override val fir: CfirTypeOperator, level: Int) : CFGNode<CfirTypeOperator>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitTypeOperatorCallNode(this, data)
}

class ComparisonExpressionNode(owner: ControlFlowGraph, override val fir: CfirComparisonExpression, level: Int) : CFGNode<CfirComparisonExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitComparisonExpressionNode(this, data)
}

class JumpNode(owner: ControlFlowGraph, override val fir: CfirJump<*>, level: Int) : CFGNode<CfirJump<*>>(owner, level), TailrecExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitJumpNode(this, data)
}

class LiteralExpressionNode(owner: ControlFlowGraph, override val fir: CfirLiteralExpression, level: Int) : CFGNode<CfirLiteralExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitLiteralExpressionNode(this, data)
}

class QualifiedAccessNode(owner: ControlFlowGraph, override val fir: CfirQualifiedAccessExpression, level: Int) : CFGNode<CfirQualifiedAccessExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitQualifiedAccessNode(this, data)
}

class FunctionCallArgumentsEnterNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, level: Int) : CFGNode<CfirFunctionCall>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallArgumentsEnterNode(this, data)
}

class FunctionCallArgumentsExitNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, var explicitReceiverExitNode: CFGNode<*>, level: Int) :
    CFGNode<CfirFunctionCall>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallArgumentsExitNode(this, data)
}

class FunctionCallEnterNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, level: Int) : CFGNode<CfirFunctionCall>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallEnterNode(this, data)
}

class FunctionCallExitNode(owner: ControlFlowGraph, override val fir: CfirFunctionCall, level: Int) : CFGNode<CfirFunctionCall>(owner, level) {
    override val isUnion: Boolean
        get() = true

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFunctionCallExitNode(this, data)
}

class ThrowExceptionNode(owner: ControlFlowGraph, override val fir: CfirThrowExpression, level: Int) : CFGNode<CfirThrowExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitThrowExceptionNode(this, data)
}

class VariableDeclarationEnterNode(owner: ControlFlowGraph, override val fir: CfirVariable, level: Int) : CFGNode<CfirVariable>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitVariableDeclarationEnterNode(this, data)
}

class VariableDeclarationExitNode(owner: ControlFlowGraph, override val fir: CfirVariable, level: Int) : CFGNode<CfirVariable>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitVariableDeclarationExitNode(this, data)
}

class VariableAssignmentNode(owner: ControlFlowGraph, override val fir: CfirAssignment, level: Int) : CFGNode<CfirAssignment>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitVariableAssignmentNode(this, data)
}

class EnterOptionalChainNode(owner: ControlFlowGraph, override val fir: CfirOptionalChainExpression, level: Int) : CFGNode<CfirOptionalChainExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitEnterOptionalChainNode(this, data)
}

class ExitOptionalChainNode(owner: ControlFlowGraph, override val fir: CfirOptionalChainExpression, level: Int) : CFGNode<CfirOptionalChainExpression>(owner, level), TailrecExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitExitOptionalChainNode(this, data)
}

class WrappedExpressionNode(owner: ControlFlowGraph, override val fir: CfirWrappedExpression, level: Int) : CFGNode<CfirWrappedExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitWrappedExpressionNode(this, data)
}

class StubNode(owner: ControlFlowGraph, level: Int) : CFGNode<CfirStub>(owner, level) {
    init {
        isDead = true
    }

    override val fir: CfirStub get() = CfirStub

    override val flowInitialized: Boolean
        get() = firstPreviousNode.flowInitialized

    override var flow: PersistentFlow
        get() = firstPreviousNode.flow
        @CfgInternals
        set(_) = throw IllegalStateException("Cannot set flow for stub node")

    override val alternateFlowPaths: Set<FlowPath>
        get() = firstPreviousNode.alternateFlowPaths

    override fun getAlternateFlow(path: FlowPath): PersistentFlow? = firstPreviousNode.getAlternateFlow(path)

    @CfgInternals
    override fun addAlternateFlow(path: FlowPath, flow: PersistentFlow) {
        firstPreviousNode.addAlternateFlow(path, flow)
    }

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitStubNode(this, data)
}

object CfirStub : CfirExpression() {
    override val source: CjSourceElement? get() = null
    override val coneTypeOrNull = null
    override val annotations: List<CfirAnnotation> get() = emptyList()

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression = this
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement = this
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) { assert(newAnnotations.isEmpty()) }
    override fun replaceConeTypeOrNull(newConeTypeOrNull: org.cangnova.cangjie.cfir.types.ConeCangJieType?) { assert(newConeTypeOrNull == null) }
}

class FakeExpressionEnterNode(owner: ControlFlowGraph, level: Int) : CFGNode<CfirStub>(owner, level), GraphEnterNodeMarker, GraphExitNodeMarker {
    init {
        isDead = true
    }

    override val fir: CfirStub get() = CfirStub

    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFakeExpressionEnterNode(this, data)
}

// ----------------------------------- Field initializer -----------------------------------
// 对齐仓颉 `let x: T = expr` / `var x: T = expr` 字段初始化子图。
// 仓颉 `prop` 仅声明 getter/setter 无 initializer,故不建模 PropertyInitializer*Node。

class FieldInitializerEnterNode(
    owner: ControlFlowGraph,
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirFieldVariable>(owner, level), GraphEnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFieldInitializerEnterNode(this, data)
}

class FieldInitializerExitNode(
    owner: ControlFlowGraph,
    override val fir: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
    level: Int,
) : CFGNode<org.cangnova.cangjie.cfir.declarations.CfirFieldVariable>(owner, level), GraphExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitFieldInitializerExitNode(this, data)
}

// ----------------------------------- Spawn (仓颉协程) -----------------------------------
// `spawn { body }` 启动新线程/协程执行 body,主控制流直接继续到 spawn 表达式结果。
// body 作为独立子图存在,由 CfirAnonymousFunction 持有 controlFlowGraphReference。

class SpawnExpressionNode(
    owner: ControlFlowGraph,
    override val fir: CfirSpawnExpression,
    level: Int,
) : CFGNode<CfirSpawnExpression>(owner, level) {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSpawnExpressionNode(this, data)
}

// ----------------------------------- Synchronized (仓颉同步块) -----------------------------------
// `synchronized(lock) { body }` 获取互斥锁→执行 body→无论正常/异常都释放。
// 语义上 Enter/Exit 成对,Exit 作为 union 节点汇合正常路径与潜在抛出路径。

class SynchronizedEnterNode(
    owner: ControlFlowGraph,
    override val fir: CfirSynchronizedExpression,
    level: Int,
) : CFGNode<CfirSynchronizedExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSynchronizedEnterNode(this, data)
}

class SynchronizedExitNode(
    owner: ControlFlowGraph,
    override val fir: CfirSynchronizedExpression,
    level: Int,
) : CFGNode<CfirSynchronizedExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitSynchronizedExitNode(this, data)
}

// ----------------------------------- Unsafe (仓颉不安全块) -----------------------------------
// `unsafe { body }` 只切换编译器的 unsafe 上下文标记,不引入实际控制流变化,
// 但为 DFA 提供独立的边界节点以区分 unsafe 区域内/外。

class UnsafeEnterNode(
    owner: ControlFlowGraph,
    override val fir: CfirUnsafeExpression,
    level: Int,
) : CFGNode<CfirUnsafeExpression>(owner, level), EnterNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitUnsafeEnterNode(this, data)
}

class UnsafeExitNode(
    owner: ControlFlowGraph,
    override val fir: CfirUnsafeExpression,
    level: Int,
) : CFGNode<CfirUnsafeExpression>(owner, level), ExitNodeMarker {
    override fun <R, D> accept(visitor: ControlFlowGraphVisitor<R, D>, data: D): R = visitor.visitUnsafeExitNode(this, data)
}
