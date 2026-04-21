package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * 对位 Kotlin FIR `ControlFlowGraph` 的完整图模型。
 *
 * 这里不再保留旧的空壳图结构，后续 CFG builder/copier、DFA context snapshot、
 * low-level patching 都统一依赖这一份语义层基础设施。
 */
class ControlFlowGraph(
    val declaration: CfirDeclaration?,
    val name: String,
    val kind: Kind,
) {
    @set:CfgInternals
    var nodeCount: Int = 0

    lateinit var nodes: List<CFGNode<*>>
        private set

    @set:CfgInternals
    lateinit var enterNode: CFGNode<*>

    @set:CfgInternals
    lateinit var exitNode: CFGNode<*>

    val isSubGraph: Boolean
        get() = enterNode.previousNodes.isNotEmpty()

    val subGraphs: List<ControlFlowGraph>
        get() = nodes.flatMap { (it as? CFGNodeWithSubgraphs<*>)?.subGraphs ?: emptyList() }

    /**
     * 复制图关系数据。`nodeCount` 故意不从旧图覆盖，因为新图节点 id 在创建时已分配。
     */
    @CfgInternals
    fun copyData(from: ControlFlowGraph, mapper: ControlFlowNodeMapper) {
        if (from::nodes.isInitialized) {
            nodes = from.nodes.map(mapper::get)
        }
        if (from::enterNode.isInitialized) {
            enterNode = mapper[from.enterNode]
        }
        if (from::exitNode.isInitialized) {
            exitNode = mapper[from.exitNode]
        }
    }

    @CfgInternals
    fun complete() {
        nodes = orderNodes(isComplete = true)
    }

    /**
     * 从 enter 节点开始摊平成稳定节点序。
     *
     * Kotlin 原实现这里采用 BFS 风格排序，并依赖回边/前驱计数过滤循环。
     * snapshot/copy/render 都直接建立在这份顺序之上。
     */
    @CfgInternals
    fun orderNodes(isComplete: Boolean): List<CFGNode<*>> {
        val result = ArrayList<CFGNode<*>>(nodeCount).apply { add(enterNode) }
        val countdowns = IntArray(nodeCount)
        var index = 0
        while (index < result.size) {
            val node = result[index++]
            for (next in node.followingNodes) {
                if (next.owner != this) {
                    // 子图按各自 graph 独立排序。
                } else if (next.previousNodes.size == 1) {
                    result.add(next)
                } else if (!node.edgeTo(next).kind.isBack) {
                    val remaining = countdowns[next.id].let { if (it == 0) next.previousNodeCount else it } - 1
                    if (remaining == 0) {
                        result.add(next)
                    }
                    countdowns[next.id] = remaining
                }
            }
        }

        if (isComplete) {
            assert(result.size == nodeCount) {
                "some nodes ${if (countdowns.all { it == 0 }) "are not reachable" else "form loops"} in control flow graph $name"
            }
        }

        return result
    }

    enum class Kind {
        File,
        Class,
        Constructor,
        Function,
        LocalFunction,
        AnonymousFunction,
        AnonymousFunctionCalledInPlace,
        FieldInitializer,
        CodeFragment,
        DefaultArgument,
    }

    fun traverse(visitor: ControlFlowGraphVisitorVoid) {
        for (node in nodes) {
            node.accept(visitor)
            (node as? CFGNodeWithSubgraphs<*>)?.subGraphs?.forEach { it.traverse(visitor) }
        }
    }
}

data class Edge(
    val label: EdgeLabel,
    val kind: EdgeKind,
) {
    companion object {
        val Normal_Forward: Edge = Edge(NormalPath, EdgeKind.Forward)
        private val Normal_DfgForward: Edge = Edge(NormalPath, EdgeKind.DfgForward)
        private val Normal_CfgForward: Edge = Edge(NormalPath, EdgeKind.CfgForward)
        private val Normal_DeadForward: Edge = Edge(NormalPath, EdgeKind.DeadForward)
        private val Normal_DeadDfgForward: Edge = Edge(NormalPath, EdgeKind.DeadDfgForward)
        private val Normal_DeadCfgForward: Edge = Edge(NormalPath, EdgeKind.DeadCfgForward)
        private val Normal_CfgBackward: Edge = Edge(NormalPath, EdgeKind.CfgBackward)
        private val Normal_DeadCfgBackward: Edge = Edge(NormalPath, EdgeKind.DeadCfgBackward)

        fun create(label: EdgeLabel, kind: EdgeKind): Edge =
            when (label) {
                NormalPath -> {
                    when (kind) {
                        EdgeKind.Forward -> Normal_Forward
                        EdgeKind.DfgForward -> Normal_DfgForward
                        EdgeKind.CfgForward -> Normal_CfgForward
                        EdgeKind.DeadForward -> Normal_DeadForward
                        EdgeKind.DeadDfgForward -> Normal_DeadDfgForward
                        EdgeKind.DeadCfgForward -> Normal_DeadCfgForward
                        EdgeKind.CfgBackward -> Normal_CfgBackward
                        EdgeKind.DeadCfgBackward -> Normal_DeadCfgBackward
                    }
                }

                else -> Edge(label, kind)
            }
    }
}

sealed interface EdgeLabel {
    val label: String?
}

object NormalPath : EdgeLabel {
    override val label: String? get() = null
}

object UncaughtExceptionPath : EdgeLabel {
    override val label: String get() = "onUncaughtException"
}

object PostponedPath : EdgeLabel {
    override val label: String get() = "Postponed"
}

data object CapturedByValue : EdgeLabel {
    override val label: String get() = "CapturedByValue"
}

enum class EdgeKind(
    val usedInDfa: Boolean,
    val usedInDeadDfa: Boolean,
    val usedInCfa: Boolean,
    val isBack: Boolean,
    val isDead: Boolean,
) {
    Forward(usedInDfa = true, usedInDeadDfa = true, usedInCfa = true, isBack = false, isDead = false),
    DfgForward(usedInDfa = true, usedInDeadDfa = true, usedInCfa = false, isBack = false, isDead = false),
    CfgForward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = false, isDead = false),

    DeadForward(usedInDfa = false, usedInDeadDfa = true, usedInCfa = true, isBack = false, isDead = true),
    DeadDfgForward(usedInDfa = false, usedInDeadDfa = true, usedInCfa = false, isBack = false, isDead = true),
    DeadCfgForward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = false, isDead = true),

    CfgBackward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = true, isDead = false),
    DeadCfgBackward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = true, isDead = true),
    ;

    fun toDead(): EdgeKind = when (this) {
        Forward -> DeadForward
        DfgForward -> DeadDfgForward
        CfgForward -> DeadCfgForward
        DeadForward -> DeadForward
        DeadDfgForward -> DeadDfgForward
        DeadCfgForward -> DeadCfgForward
        CfgBackward -> DeadCfgBackward
        DeadCfgBackward -> DeadCfgBackward
    }

    companion object {
        fun forward(usedInCfa: Boolean = false, usedInDfa: Boolean = false): EdgeKind? {
            return when {
                usedInCfa && usedInDfa -> Forward
                usedInCfa -> CfgForward
                usedInDfa -> DfgForward
                else -> null
            }
        }
    }
}

@RequiresOptIn
annotation class CfgInternals

private val CFGNode<*>.previousNodeCount: Int
    get() = previousNodes.count { it.owner == owner && !edgeFrom(it).kind.isBack }
