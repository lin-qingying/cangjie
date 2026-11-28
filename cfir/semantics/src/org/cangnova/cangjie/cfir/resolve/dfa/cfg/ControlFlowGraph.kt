package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.resolve.dfa.DfaSourceVisitor

/**
 * 对位 Kotlin FIR `ControlFlowGraph` 的完整图模型。
 *
 * 这里不再保留旧的空壳图结构，后续 CFG builder/copier、DFA context snapshot、
 * low-level patching 都统一依赖这一份语义层基础设施。
 *
 * @property declaration 该控制流图对应的声明；文件级或合成图可为空。
 * @property name 控制流图在调试和断言消息中的名称。
 * @property kind 控制流图所属结构种类。
 */
class ControlFlowGraph(
    /**
     * 该控制流图对应的声明；文件级或合成图可为空。
     */
    val declaration: CfirDeclaration?,
    /**
     * 控制流图在调试和断言消息中的名称。
     */
    val name: String,
    /**
     * 控制流图所属结构种类。
     */
    val kind: Kind,
) : FunctionCallNodeIndexOwner {
    private val pendingSemanticTransfers: MutableSet<CfirElement> =
        java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirElement, Boolean>())

    /** 尚无独立 CFG node 的实际判别也必须保留完成义务，不能因 visibleBindings 为空被漏掉。 */
    val hasPendingSemanticTransfers: Boolean get() = pendingSemanticTransfers.isNotEmpty()

    @CfgInternals
    fun requireSemanticTransfer(element: CfirElement) { pendingSemanticTransfers += element }

    /** 完成图与隔离 probe 的正式采用都保留实际 node/index 事实，生命周期不依赖 builder 栈。 */
    private val functionCallNodes: MutableMap<CfirFunctionCall, MutableList<CFGNode<*>>> = java.util.IdentityHashMap()
    private val functionCallNodeAliases: MutableMap<CFGNode<*>, MutableSet<CfirFunctionCall>> = java.util.IdentityHashMap()
    private val withdrawnFunctionCallNodes: MutableMap<CfirFunctionCall, MutableSet<CFGNode<*>>> = java.util.IdentityHashMap()

    @CfgInternals
    fun registerFunctionCallNode(node: CFGNode<*>) {
        check(node.owner === this && node is RevisableFunctionCallNode) { "Call CFG registration must belong to the actual graph" }
        val call = node.fir as? CfirFunctionCall ?: return
        addFunctionCallNodeAlias(call, node)
    }

    private fun addFunctionCallNodeAlias(call: CfirFunctionCall, node: CFGNode<*>) {
        check(node.owner === this) { "Call CFG alias belongs to another graph" }
        val nodes = functionCallNodes.getOrPut(call) { mutableListOf() }
        if (nodes.none { it === node }) nodes += node
        functionCallNodeAliases.getOrPut(node) {
            java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirFunctionCall, Boolean>())
        }.add(call)
        withdrawnFunctionCallNodes[call]?.let { withdrawn ->
            withdrawn.remove(node)
            if (withdrawn.isEmpty()) withdrawnFunctionCallNodes.remove(call)
        }
    }

    /** 完整 AST 回滚恢复同一节点的真实成员集合，新 alias 键撤销后仍保留拒绝凭证。 */
    @CfgInternals
    override fun captureFunctionCallNodeIndex(node: CFGNode<*>): RevisableFunctionCallNodeSnapshot {
        check(node.owner === this) { "Call CFG snapshot belongs to another graph" }
        val before = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirFunctionCall, Boolean>()).apply {
            addAll(checkNotNull(functionCallNodeAliases[node]) { "Call CFG node has no actual graph registration" })
        }
        return object : RevisableFunctionCallNodeSnapshot {
            override fun validateRestore() {
                check(node.owner === this@ControlFlowGraph && functionCallNodeAliases.containsKey(node)) {
                    "Call CFG snapshot lost its actual graph/node owner"
                }
            }

            override fun restore() {
                validateRestore()
                val aliases = checkNotNull(functionCallNodeAliases[node])
                for (call in aliases.toList()) {
                    if (call in before) continue
                    val nodes = checkNotNull(functionCallNodes[call])
                    check(nodes.removeAll { it === node }) { "Call CFG reverse index differs from its actual nodes" }
                    if (nodes.isEmpty()) functionCallNodes.remove(call)
                    aliases.remove(call)
                    withdrawnFunctionCallNodes.getOrPut(call) {
                        java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CFGNode<*>, Boolean>())
                    }.add(node)
                }
                before.forEach { addFunctionCallNodeAlias(it, node) }
            }
        }
    }

    /** 只换本图实际 node 的 fir，既有 owner/id/level/edge/flow/subgraph 不变。 */
    @CfgInternals
    fun updateFunctionCallNodes(original: CfirFunctionCall, replacement: CfirFunctionCall) {
        if (original === replacement) return
        val nodes = functionCallNodes[original]
        if (nodes == null) {
            check(withdrawnFunctionCallNodes[original].isNullOrEmpty()) { "CFG call replacement reused a withdrawn AST identity" }
            return
        }
        check(nodes.all { it.fir === original || it.fir === replacement }) {
            "CFG call replacement reused a withdrawn AST identity: " + describeFunctionCallNodes(original)
        }
        nodes.filter { it.fir === original }.forEach {
            (it as RevisableFunctionCallNode).setResolvedFunctionCall(original, replacement)
        }
        nodes.forEach { addFunctionCallNodeAlias(replacement, it) }
    }

    fun describeFunctionCallNodes(call: CfirFunctionCall): String = functionCallNodes[call].orEmpty().joinToString {
        "${it.javaClass.simpleName}(graph=${System.identityHashCode(it.owner)},id=${it.id},fir=${System.identityHashCode(it.fir)})"
    }

    /** 当前图已经创建的节点数量，同时作为新节点 id 分配器。 */
    @set:CfgInternals
    var nodeCount: Int = 0

    /** 图完成后按稳定顺序保存的节点列表。 */
    lateinit var nodes: List<CFGNode<*>>
        private set

    /** 图入口节点。 */
    @set:CfgInternals
    lateinit var enterNode: CFGNode<*>

    /** 图出口节点。 */
    @set:CfgInternals
    lateinit var exitNode: CFGNode<*>

    /** 未处理异常的独立出口；按需创建，不进入正常返回值收集。 */
    @set:CfgInternals
    var exceptionExitNode: ExceptionExitNode? = null

    /** 当前图是否作为其他图中的子图存在。 */
    val isSubGraph: Boolean
        get() = enterNode.previousNodes.isNotEmpty()

    /** 当前图直接包含的所有子图。 */
    val subGraphs: List<ControlFlowGraph>
        get() = nodes.flatMap { (it as? CFGNodeWithSubgraphs<*>)?.subGraphs ?: emptyList() }

    /** 完成图使用冻结节点，partial 图使用只读前缀排序；访问不写入 nodes、不推进构造状态。 */
    @OptIn(CfgInternals::class)
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        declaration?.let(visitor::visitElement)
        pendingSemanticTransfers.forEach(visitor::visitElement)
        val sourceNodes = if (::nodes.isInitialized) nodes else orderNodes(isComplete = false)
        sourceNodes.forEach(visitor::visitNode)
    }

    /** 只读取实际子图；partial 图尚未冻结 nodes 时也不能改用语法或偏移推断依赖。 */
    @OptIn(CfgInternals::class)
    fun sourceSubGraphs(): List<ControlFlowGraph> =
        (if (::nodes.isInitialized) nodes else orderNodes(isComplete = false))
            .flatMap { (it as? CFGNodeWithSubgraphs<*>)?.subGraphs.orEmpty() }

    /** 已证明退休的图解除跨图边，不改变保留成员的内部边、flow 或声明/symbol。 */
    @OptIn(CfgInternals::class)
    fun detachCrossGraphEdges() {
        val sourceNodes = (if (::nodes.isInitialized) nodes else orderNodes(isComplete = false)) + enterNode + exitNode
        sourceNodes.distinct().forEach { node ->
            node.previousNodes.toList().filter { it.owner !== this }.forEach { CFGNode.removeEdge(it, node) }
            node.followingNodes.toList().filter { it.owner !== this }.forEach { CFGNode.removeEdge(node, it) }
        }
    }

    /**
     * 复制图关系数据。`nodeCount` 故意不从旧图覆盖，因为新图节点 id 在创建时已分配。
     */
    @CfgInternals
    fun copyData(from: ControlFlowGraph, mapper: ControlFlowNodeMapper) {
        pendingSemanticTransfers.addAll(from.pendingSemanticTransfers)
        if (from::nodes.isInitialized) {
            nodes = from.nodes.map(mapper::get)
        }
        if (from::enterNode.isInitialized) {
            enterNode = mapper[from.enterNode]
        }
        if (from::exitNode.isInitialized) {
            exitNode = mapper[from.exitNode]
        }
        exceptionExitNode = from.exceptionExitNode?.let(mapper::get)
        functionCallNodes.clear()
        functionCallNodeAliases.clear()
        withdrawnFunctionCallNodes.clear()
        from.functionCallNodes.forEach { (call, nodes) ->
            nodes.forEach { addFunctionCallNodeAlias(call, mapper[it]) }
        }
        from.withdrawnFunctionCallNodes.forEach { (call, nodes) ->
            withdrawnFunctionCallNodes[call] = nodes.mapTo(
                java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CFGNode<*>, Boolean>()),
                mapper::get,
            )
        }
    }

    /**
     * 标记图构造完成，并按稳定控制流顺序冻结节点列表。
     */
    @CfgInternals
    fun complete() {
        // 异常出口的全部输入在所属图完成后才确定；早期 dead 输入不能阻断后续 live 异常。
        exceptionExitNode?.updateDeadStatus()
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

    /**
     * 控制流图所属的 CFIR 结构种类。
     */
    enum class Kind {
        /** 文件级控制流图。 */
        File,
        /** 类或接口级控制流图。 */
        Class,
        /** 构造器控制流图。 */
        Constructor,
        /** 普通函数控制流图。 */
        Function,
        /** 局部函数控制流图。 */
        LocalFunction,
        /** 匿名函数控制流图。 */
        AnonymousFunction,
        /** 按 call-in-place 语义处理的匿名函数控制流图。 */
        AnonymousFunctionCalledInPlace,
        /** 字段初始化控制流图。 */
        FieldInitializer,
        /** retained 资源 initializer 的独立执行根，不是外层 try 的资源获取步骤。 */
        ResourceInitializer,
        /** 代码片段控制流图。 */
        CodeFragment,
        /** 默认参数表达式控制流图。 */
        DefaultArgument,
    }

    /**
     * 遍历当前图及其子图中的所有节点。
     */
    fun traverse(visitor: ControlFlowGraphVisitorVoid) {
        for (node in nodes) {
            node.accept(visitor)
            (node as? CFGNodeWithSubgraphs<*>)?.subGraphs?.forEach { it.traverse(visitor) }
        }
    }
}

/**
 * CFG 边描述。
 *
 * @property label 边标签，用于区分正常路径、异常路径、延期 lambda 路径等。
 * @property kind 边在 DFA/CFA 中的使用方式。
 */
data class Edge(
    /**
     * 边标签，用于区分正常路径、异常路径、延期 lambda 路径等。
     */
    val label: EdgeLabel,
    /**
     * 边在 DFA/CFA 中的使用方式。
     */
    val kind: EdgeKind,
) {
    /**
     * 常用边实例与构造工具。
     */
    companion object {
        /** 普通前向边的共享实例。 */
        val Normal_Forward: Edge = Edge(NormalPath, EdgeKind.Forward)

        /** 仅参与 DFA 的普通前向边共享实例。 */
        private val Normal_DfgForward: Edge = Edge(NormalPath, EdgeKind.DfgForward)

        /** 仅参与 CFA 的普通前向边共享实例。 */
        private val Normal_CfgForward: Edge = Edge(NormalPath, EdgeKind.CfgForward)

        /** 已死亡普通前向边共享实例。 */
        private val Normal_DeadForward: Edge = Edge(NormalPath, EdgeKind.DeadForward)

        /** 已死亡 DFA 前向边共享实例。 */
        private val Normal_DeadDfgForward: Edge = Edge(NormalPath, EdgeKind.DeadDfgForward)

        /** 已死亡 CFA 前向边共享实例。 */
        private val Normal_DeadCfgForward: Edge = Edge(NormalPath, EdgeKind.DeadCfgForward)

        /** 普通 CFA 回边共享实例。 */
        private val Normal_CfgBackward: Edge = Edge(NormalPath, EdgeKind.CfgBackward)

        /** 已死亡 CFA 回边共享实例。 */
        private val Normal_DeadCfgBackward: Edge = Edge(NormalPath, EdgeKind.DeadCfgBackward)

        /**
         * 创建边描述。
         *
         * 普通路径上的常见边会复用共享实例，减少 CFG 构造过程中的对象分配。
         */
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

/**
 * CFG 边标签。
 */
sealed interface EdgeLabel {
    /** 用于渲染和调试的标签文本；为空表示普通路径。 */
    val label: String?
}

/** 普通控制流路径。 */
object NormalPath : EdgeLabel {
    /**
     * 普通路径没有额外渲染标签。
     */
    override val label: String? get() = null
}

/**
 * match 分支条件成立后的控制流边。
 *
 * 条件出口节点自身携带对应 [CfirMatchBranch]，因此标签不重复保存树节点，避免 CFG
 * snapshot/copy 时产生跨树引用。
 */
data object MatchBranchSuccess : EdgeLabel {
    /** 成功边在 CFG 渲染中的标签。 */
    override val label: String get() = "match-success"
}

/**
 * match 分支条件不成立后的控制流边。
 *
 * 该边通向下一 case 条件或末尾 synthetic else；CFA 常量传播只在已知条件值时保留
 * success/failure 中的唯一目标边。
 */
data object MatchBranchFailure : EdgeLabel {
    /** 失败边在 CFG 渲染中的标签。 */
    override val label: String get() = "match-failure"
}

/** if 条件成立的路径，包含 then 分支。 */
data object IfBranchTrue : EdgeLabel {
    override val label: String get() = "if-true"
}

/** if 条件不成立的路径；无 else 时直接继续表达式后的正文。 */
data object IfBranchFalse : EdgeLabel {
    override val label: String get() = "if-false"
}

/** 当前 catch 类型测试失败，异常值保持不变。 */
data object ExceptionNotMatched : EdgeLabel {
    override val label: String get() = "exception-not-matched"
}

/** 未捕获异常路径。 */
object UncaughtExceptionPath : EdgeLabel {
    /**
     * 未捕获异常边在 CFG 渲染中的标签文本。
     */
    override val label: String get() = "onUncaughtException"
}

/** 延期执行路径。 */
object PostponedPath : EdgeLabel {
    /**
     * 延期执行边在 CFG 渲染中的标签文本。
     */
    override val label: String get() = "Postponed"
}

/** 按值捕获路径。 */
data object CapturedByValue : EdgeLabel {
    /**
     * 按值捕获边在 CFG 渲染中的标签文本。
     */
    override val label: String get() = "CapturedByValue"
}

/**
 * CFG 边种类。
 *
 * @property usedInDfa 是否参与普通 DFA。
 * @property usedInDeadDfa 是否参与死代码 DFA。
 * @property usedInCfa 是否参与控制流可达性分析。
 * @property isBack 是否为回边。
 * @property isDead 是否为死亡边。
 */
enum class EdgeKind(
    /**
     * 是否参与普通 DFA。
     */
    val usedInDfa: Boolean,
    /**
     * 是否参与死代码 DFA。
     */
    val usedInDeadDfa: Boolean,
    /**
     * 是否参与控制流可达性分析。
     */
    val usedInCfa: Boolean,
    /**
     * 是否为回边。
     */
    val isBack: Boolean,
    /**
     * 是否为死亡边。
     */
    val isDead: Boolean,
) {
    /** 同时参与 DFA 与 CFA 的正常前向边。 */
    Forward(usedInDfa = true, usedInDeadDfa = true, usedInCfa = true, isBack = false, isDead = false),
    /** 仅参与 DFA 的前向边。 */
    DfgForward(usedInDfa = true, usedInDeadDfa = true, usedInCfa = false, isBack = false, isDead = false),
    /** 仅参与 CFA 的前向边。 */
    CfgForward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = false, isDead = false),

    /** 已死亡的普通前向边。 */
    DeadForward(usedInDfa = false, usedInDeadDfa = true, usedInCfa = true, isBack = false, isDead = true),
    /** 已死亡的 DFA 前向边。 */
    DeadDfgForward(usedInDfa = false, usedInDeadDfa = true, usedInCfa = false, isBack = false, isDead = true),
    /** 已死亡的 CFA 前向边。 */
    DeadCfgForward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = false, isDead = true),

    /** CFA 回边。 */
    CfgBackward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = true, isDead = false),
    /** 已死亡的 CFA 回边。 */
    DeadCfgBackward(usedInDfa = false, usedInDeadDfa = false, usedInCfa = true, isBack = true, isDead = true),
    ;

    /**
     * 返回当前边种类对应的死亡边种类。
     */
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

    /**
     * 边种类构造工具。
     */
    companion object {
        /**
         * 根据 CFA/DFA 参与情况选择前向边种类。
         */
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

/**
 * 标记只允许 CFG 构造、复制和 DFA 基础设施内部使用的 API。
 */
@RequiresOptIn
annotation class CfgInternals

/**
 * 当前节点在同一图内的非回边前驱数量。
 */
private val CFGNode<*>.previousNodeCount: Int
    get() = previousNodes.count { it.owner == owner && !edgeFrom(it).kind.isBack }
