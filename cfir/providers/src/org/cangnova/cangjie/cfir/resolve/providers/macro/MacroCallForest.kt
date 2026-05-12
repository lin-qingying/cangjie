package org.cangnova.cangjie.cfir.resolve.providers.macro

/**
 * Macro 调用 forest（baseline 第 8 节 + 第 12 节 Batch 7）。
 *
 * 当 `@A(@B(...))` 形态出现时，宏调用形成嵌套关系；本 forest 将所有
 * macro surface 按嵌套关系组织成多棵树：
 * - 每棵树的 root 是源码中最外层未被任何更外层宏包含的 surface；
 * - 每个 node 的 children 是直接被包含在其 attr/input 范围内的 surface；
 * - root 按 baseline 第 8 节"确定性 source / token order"排序（先按
 *   sourceRange.startOffset，再按 surfaceId）。
 *
 * Evaluator 按 **child-first** 顺序展开（先展内层），并在 child 完成后
 * 用结果 token 流刷新 parent 的 args。
 */
class MacroCallForest internal constructor(
    val roots: List<MacroCallNode>,
) {
    /** 全部节点（按 child-first 拓扑顺序）。 */
    val allNodes: List<MacroCallNode>
        get() {
            val out = mutableListOf<MacroCallNode>()
            for (root in roots) collectChildFirst(root, out)
            return out
        }

    private fun collectChildFirst(node: MacroCallNode, out: MutableList<MacroCallNode>) {
        for (child in node.children) collectChildFirst(child, out)
        out += node
    }
}

/**
 * Forest 内单个节点：包装一个 [MacroSurface]，并维护父子关系。
 */
class MacroCallNode internal constructor(
    val surface: MacroSurface,
    val parent: MacroCallNode?,
    children: List<MacroCallNode>,
) {
    private val _children: MutableList<MacroCallNode> = children.toMutableList()
    private val _childEdges: MutableList<MacroCallEdge> = mutableListOf()
    val children: List<MacroCallNode> get() = _children
    val childEdges: List<MacroCallEdge> get() = _childEdges

    internal fun addChild(child: MacroCallNode, edge: MacroCallEdge) {
        _children += child
        _childEdges += edge
    }

    /** Baseline 第 7 节"parentNames"：从 root 到自身路径上各级 macro 名（去重）。 */
    val parentNames: List<String>
        get() {
            val acc = mutableListOf<String>()
            var current: MacroCallNode? = this.parent
            while (current != null) {
                current.surface.qualifiedName?.asString()?.let(acc::add)
                current = current.parent
            }
            return acc.reversed()
    }
}

/**
 * Child surface 位于 parent payload 的哪条 token 通道。
 */
enum class MacroPayloadChannel {
    ATTR,
    INPUT,
    UNRESOLVED,
}

/**
 * Parent -> direct child 的稳定替换边。
 *
 * [replaceRange] 是 child macro surface 在宿主源码中的完整范围；[channel]
 * 由 parent 的 attr/input token 覆盖关系判定，用于后续只替换对应 payload
 * 通道中的 token 段，禁止退化为 flatten。
 */
data class MacroCallEdge(
    val parent: MacroCallNode,
    val child: MacroCallNode,
    val channel: MacroPayloadChannel,
    val replaceRange: MacroSurfaceSourceRange?,
)

/**
 * Macro forest 构造器。
 *
 * Surface 之间的嵌套关系通过 [MacroSurface.sourceRange] 的 start/end offset
 * 推导：若 child 的源范围完全位于 parent 的源范围内，则视为 parent 的子节点。
 *
 * Baseline 第 8 节"deterministic root order"：roots 按
 * `(sourceRange.startOffset, surfaceId)` 排序，无 sourceRange 的 surface
 * 退到尾部（保持稳定，依赖 surfaceId 顺序）。
 */
object MacroCallForestBuilder {
    fun build(surfaces: List<MacroSurface>): MacroCallForest {
        if (surfaces.isEmpty()) return MacroCallForest(emptyList())

        // 1) 计算每个 surface 的有效 source range（缺失时记 -1 / +inf 推到尾）
        val ranges: List<Triple<Int, Int, MacroSurface>> = surfaces.map { s ->
            val start = s.sourceRange?.startOffset ?: Int.MAX_VALUE
            val end = s.sourceRange?.endOffset ?: Int.MIN_VALUE
            Triple(start, end, s)
        }

        // 2) 按嵌套关系建图：parent 是包含 child 范围的最近 surface
        val sorted = ranges.sortedWith(compareBy({ it.first }, { -it.second }, { it.third.surfaceId }))
        val nodeByIdentity = LinkedHashMap<MacroSurface, MacroCallNode>()
        for ((_, _, surface) in sorted) {
            nodeByIdentity[surface] = MacroCallNode(surface = surface, parent = null, children = emptyList())
        }

        val stack = ArrayDeque<Triple<Int, Int, MacroSurface>>()
        // 简易扫线：把每条 surface 按 start 升序遍历，借助 stack 维护当前嵌套链。
        for (entry in sorted) {
            val (start, end, surface) = entry
            // 弹出所有不能容纳 surface 的栈顶（其 end < surface.start）
            while (stack.isNotEmpty()) {
                val (_, topEnd, topSurface) = stack.last()
                if (topEnd >= end && topEnd >= start) break
                stack.removeLast()
            }

            val parentEntry = stack.lastOrNull { (parentStart, parentEnd, _) ->
                parentStart <= start && parentEnd >= end
            }
            val node = nodeByIdentity.getValue(surface)
            if (parentEntry != null) {
                val parentNode = nodeByIdentity.getValue(parentEntry.third)
                val merged = MacroCallNode(surface = surface, parent = parentNode, children = node.children)
                nodeByIdentity[surface] = merged
                parentNode.addChild(
                    child = merged,
                    edge = MacroCallEdge(
                        parent = parentNode,
                        child = merged,
                        channel = parentNode.surface.payloadChannelFor(surface),
                        replaceRange = surface.sourceRange,
                    ),
                )
            }
            stack.addLast(entry)
        }

        val roots = nodeByIdentity.values
            .filter { it.parent == null }
            .sortedWith(
                compareBy(
                    { it.surface.sourceRange?.startOffset ?: Int.MAX_VALUE },
                    { it.surface.surfaceId },
                ),
            )
        return MacroCallForest(roots)
    }

    private fun MacroSurface.payloadChannelFor(child: MacroSurface): MacroPayloadChannel {
        val range = child.sourceRange ?: return MacroPayloadChannel.UNRESOLVED
        return when {
            inputTokens.containsSurfaceRange(range) -> MacroPayloadChannel.INPUT
            attrTokens.containsSurfaceRange(range) -> MacroPayloadChannel.ATTR
            else -> MacroPayloadChannel.UNRESOLVED
        }
    }

    private fun List<MacroSurfaceToken>.containsSurfaceRange(range: MacroSurfaceSourceRange): Boolean {
        return any { token -> token.startOffset >= range.startOffset && token.endOffset <= range.endOffset }
    }
}

/**
 * 用于 cycle 检测的指纹（baseline 第 12 节 Batch 7：fingerprint cycle detection）。
 *
 * 一次展开循环出现的标志是"同一 fingerprint 在 forest evaluator 多次出现"。
 * Fingerprint = (qualifiedName, parentNames, normalized attr tokens, normalized input tokens)
 */
data class MacroExpansionFingerprint(
    val qualifiedName: String?,
    val parentNames: List<String>,
    val attrTokensHash: Int,
    val inputTokensHash: Int,
) {
    companion object {
        fun of(
            node: MacroCallNode,
            childResults: Map<MacroCallNode, List<MacroSurfaceToken>> = emptyMap(),
        ): MacroExpansionFingerprint {
            val surface = node.surface
            return MacroExpansionFingerprint(
                qualifiedName = surface.qualifiedName?.asString(),
                parentNames = node.parentNames,
                attrTokensHash = surface.attrTokens.textHashWithChildResults(childResults),
                inputTokensHash = surface.inputTokens.textHashWithChildResults(childResults),
            )
        }

        private fun List<MacroSurfaceToken>.textHashWithChildResults(
            childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
        ): Int {
            if (childResults.isEmpty()) return map { it.text }.hashCode()
            return buildList {
                addAll(this@textHashWithChildResults.map { it.text })
                for ((child, result) in childResults) {
                    add(child.surface.surfaceId.toString())
                    addAll(result.map { it.text })
                }
            }.hashCode()
        }
    }
}

/**
 * Macro forest 评估期可观察的循环。
 */
data class MacroExpansionCycle(
    val fingerprint: MacroExpansionFingerprint,
    val nodes: List<MacroCallNode>,
)

/**
 * Forest evaluator：child-first 调度 + cycle 检测 + iteration limit。
 *
 * 真实展开调用由 [expand] lambda 承担（接收当前节点 + 已展开 child 的 token 流，
 * 返回展开后 token 流）。
 *
 * Batch 7 阶段 evaluator 主体可见，真实 fragment parse / re-eval 留给 Batch 8：
 * 该入口的回调签名足以支撑 Batch 8 fragment parser 接入。
 */
class MacroForestEvaluator(
    private val maxIterations: Int = 16,
) {
    /**
     * 执行 child-first 求值。
     *
     * @param forest 待求值的 forest
     * @param expand 给定节点 + child 展开后 token，返回该节点展开后 token；
     *               返回 null 表示不可展开（保持原样）
     * @param onCycle 检测到 fingerprint 循环时被回调，调用方可写入 registry
     */
    fun evaluate(
        forest: MacroCallForest,
        expand: (node: MacroCallNode, childResults: Map<MacroCallNode, List<MacroSurfaceToken>>) -> List<MacroSurfaceToken>?,
        onCycle: (MacroExpansionCycle) -> Unit = {},
    ): Map<MacroCallNode, List<MacroSurfaceToken>> {
        val results = LinkedHashMap<MacroCallNode, List<MacroSurfaceToken>>()
        val seenFingerprints = LinkedHashMap<MacroExpansionFingerprint, MutableList<MacroCallNode>>()
        val maxPerNode = maxIterations.coerceAtLeast(1)

        for (node in forest.allNodes) {
            val childResults = node.children.mapNotNull { child ->
                results[child]?.let { child to it }
            }.toMap()
            if (childResults.size != node.children.size) {
                continue
            }

            val fingerprint = MacroExpansionFingerprint.of(node, childResults)
            val history = seenFingerprints.getOrPut(fingerprint) { mutableListOf() }
            history += node
            if (history.size > maxPerNode) {
                onCycle(MacroExpansionCycle(fingerprint, history.toList()))
                continue
            }

            val expanded = expand(node, childResults) ?: continue
            results[node] = expanded
        }

        return results
    }
}
