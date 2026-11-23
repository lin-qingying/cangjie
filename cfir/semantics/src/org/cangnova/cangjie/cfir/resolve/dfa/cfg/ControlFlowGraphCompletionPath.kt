package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression

/**
 * 同一真实 CFG 程序点的 completion 状态。
 * finally 正文只有一份；进入时保存完成原因与实际 jump，退出时只恢复对应状态。
 */
data class ControlFlowGraphCompletionPath(
    val node: CFGNode<*>,
    val pending: List<PendingFinally> = emptyList(),
    val abruptJump: JumpNode? = null,
) {
    data class PendingFinally(
        val enter: FinallyBlockEnterNode,
        val completion: EdgeLabel,
        val jump: JumpNode? = null,
    )

    /** snapshot 同时映射程序点、finally frame、目标标签和实际 jump。 */
    @CfgInternals
    fun createSnapshot(mapper: ControlFlowNodeMapper): ControlFlowGraphCompletionPath =
        ControlFlowGraphCompletionPath(mapper[node], pending.map {
            val completion = it.completion
            PendingFinally(mapper[it.enter], if (completion is CFGNode<*>) mapper[completion] as EdgeLabel else completion,
                it.jump?.let(mapper::get))
        }, abruptJump?.let(mapper::get))

    /**
     * runtimeMatch 的 null 表示保留有序 InstanceOf 的两种运行时结果。
     * includeDead 仅供诊断在已拒绝的结构区域收集 source，正常流分析始终传 false。
     */
    fun successors(
        runtimeMatch: (ExceptionDispatchNode) -> Boolean? = { null },
        includeDead: Boolean = false,
        acceptEdge: (CFGNode<*>, Edge) -> Boolean = { _, _ -> true },
    ): List<ControlFlowGraphCompletionPath> {
        val decision = node as? ExceptionDispatchNode
        val matched = decision?.let(runtimeMatch)
        val active = if (node is FinallyBlockExitNode) {
            check(pending.lastOrNull()?.enter === node.enterNode) { "Finally completion lacks its real entry" }
            pending.last()
        } else null
        return node.followingNodes.mapNotNull { next ->
            val edge = node.edgeTo(next)
            if (!edge.kind.usedInCfa || !includeDead && edge.kind.isDead || !acceptEdge(next, edge)) return@mapNotNull null
            if (decision != null && matched != null && (next is CatchClauseEnterNode) != matched) return@mapNotNull null
            if (active != null && edge.label !== active.completion) return@mapNotNull null
            var remaining = if (active == null) pending else pending.dropLast(1)
            var jump = when {
                node is JumpNode -> node
                active != null -> active.jump
                else -> abruptJump
            }
            if (edge.label === UncaughtExceptionPath) jump = null
            val overridden = edge.label as? FinallyBlockEnterNode
            if (overridden != null) {
                val index = remaining.indexOfLast { it.enter === overridden }
                check(index >= 0) { "Override lacks the executing finally completion" }
                remaining = remaining.take(index)
            }
            if (next is FinallyBlockEnterNode) {
                remaining = remaining + PendingFinally(next, edge.label, jump)
                jump = null
            } else if (next is LoopConditionEnterNode || next is LoopExitNode) {
                jump = null
            }
            ControlFlowGraphCompletionPath(next, remaining, jump)
        }
    }
}

/**
 * 生产 CFA 的 completion 工作表。
 * transfer/join 按整个路径状态索引；在 finally 入口不得先把不同完成原因的数据混在一起。
 * 正常函数出口截断本次执行，局部函数和普通 lambda 由各自声明的 consumer 独立处理。
 */
class ControlFlowGraphCompletionTraversal(val graph: ControlFlowGraph) {
    /**
     * 本次执行只包括根图及从其实际入口边进入的 called-in-place 子图。
     * 不能凭 graph kind 把词法父图、兄弟图或延迟 lambda 纳入独立函数分析。
     */
    private val graphsInExecution: Set<ControlFlowGraph> = buildSet {
        add(graph)
        val visited = mutableSetOf<CFGNode<*>>()
        val worklist = ArrayDeque<CFGNode<*>>()
        worklist.add(graph.enterNode)
        while (worklist.isNotEmpty()) {
            val node = worklist.removeFirst()
            if (!visited.add(node) || node === graph.exitNode || node is ExceptionExitNode) continue
            for (next in node.followingNodes) {
                val edge = node.edgeTo(next)
                if (!edge.kind.usedInCfa) continue
                val overridden = edge.label as? FinallyBlockEnterNode
                if (overridden != null && overridden.owner !in this) continue
                if (next is FunctionEnterNode && next.owner.kind == ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace) {
                    add(next.owner)
                }
                if (next.owner in this) worklist.add(next)
            }
        }
    }

    fun successors(path: ControlFlowGraphCompletionPath, includeDead: Boolean = false): List<ControlFlowGraphCompletionPath> {
        if (path.node === graph.exitNode || path.node is ExceptionExitNode) return emptyList()
        // 子图的局部 return 在返回调用方时已消费；外层正在执行的 finally frame 继续保留。
        val localReturn = path.abruptJump?.fir as? CfirReturnExpression
        val continuedPath = if (path.node is FunctionExitNode &&
            localReturn?.target?.labeledElement === path.node.owner.declaration
        ) path.copy(abruptJump = null) else path
        return continuedPath.successors(includeDead = includeDead) { next, edge ->
            val overridden = edge.label as? FinallyBlockEnterNode
            next.owner in graphsInExecution && (overridden == null || overridden.owner in graphsInExecution)
        }
    }

    /** 不动点包括合法回边；只有同一 completion 状态的数据才合并。 */
    fun <S : Any> collect(
        initial: S,
        transfer: (CFGNode<*>, S) -> S,
        join: (S, S) -> S,
        selectSuccessor: (CFGNode<*>, S) -> CFGNode<*>? = { _, _ -> null },
    ): Map<ControlFlowGraphCompletionPath, S> {
        val entry = ControlFlowGraphCompletionPath(graph.enterNode)
        val incoming = linkedMapOf(entry to initial)
        val worklist = ArrayDeque<ControlFlowGraphCompletionPath>()
        worklist.add(entry)
        while (worklist.isNotEmpty()) {
            val path = worklist.removeFirst()
            val output = transfer(path.node, checkNotNull(incoming[path]))
            val selected = selectSuccessor(path.node, output)
            for (successor in successors(path)) {
                if (selected != null && successor.node !== selected) continue
                val old = incoming[successor]
                val merged = if (old == null) output else join(old, output)
                if (old != merged) {
                    incoming[successor] = merged
                    worklist.add(successor)
                }
            }
        }
        return incoming
    }

    /**
     * 从本次执行的实际出口转移收集 return。
     * finally 覆盖旧 return 后，旧 JumpNode 即使仍在 nonDirectJumps 中也不能贡献返回类型。
     */
    fun returnPredecessors(): Set<CFGNode<*>> {
        val reachable = collect(Unit, { _, state -> state }, { _, _ -> Unit }).keys
        val result = linkedSetOf<CFGNode<*>>()
        for (path in reachable) {
            for (next in successors(path)) {
                if (next.node !== graph.exitNode) continue
                val jump = next.abruptJump
                if (jump != null) {
                    val statement = jump.fir as? CfirReturnExpression ?: continue
                    if (statement.target.labeledElement === graph.declaration) result.add(jump)
                } else if (path.node.edgeTo(next.node).label === NormalPath) {
                    result.add(path.node)
                }
            }
        }
        return result
    }
}
