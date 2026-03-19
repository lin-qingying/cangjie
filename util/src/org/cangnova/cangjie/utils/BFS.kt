package org.cangnova.cangjie.utils

object BFS {
    fun <N, R> bfs(
        nodes: Collection<N>,
        neighbors: DFS.Neighbors<N>,
        visited: DFS.Visited<N>,
        handler: NodeHandler<N, R>,
    ): R {
        val queue = ArrayDeque<N>()
        for (node in nodes) {
            if (visited.checkAndMarkVisited(node)) {
                queue.addLast(node)
            }
        }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!handler.onVisit(current)) break
            for (neighbor in neighbors.getNeighbors(current)) {
                if (visited.checkAndMarkVisited(neighbor)) {
                    queue.addLast(neighbor)
                }
            }
        }
        return handler.result()
    }

    fun <N, R> bfs(
        nodes: Collection<N>,
        neighbors: DFS.Neighbors<N>,
        handler: NodeHandler<N, R>,
    ): R = bfs(nodes, neighbors, DFS.VisitedWithSet(), handler)

    fun <N, R> bfsFromNode(
        node: N,
        neighbors: DFS.Neighbors<N>,
        visited: DFS.Visited<N>,
        handler: NodeHandler<N, R>,
    ): R = bfs(listOf(node), neighbors, visited, handler)

    fun <N, R> bfsFromNode(
        node: N,
        neighbors: DFS.Neighbors<N>,
        handler: NodeHandler<N, R>,
    ): R = bfs(listOf(node), neighbors, DFS.VisitedWithSet(), handler)

    /** 收集从起点可达的所有节点（按 BFS 顺序）。 */
    fun <N> reachable(
        nodes: Collection<N>,
        neighbors: DFS.Neighbors<N>,
    ): List<N> = bfs(nodes, neighbors, object : AbstractNodeHandler<N, List<N>>() {
        private val result = mutableListOf<N>()
        override fun onVisit(current: N): Boolean { result += current; return true }
        override fun result(): List<N> = result
    })

    /** 判断图中是否存在满足 predicate 的节点，找到即短路。 */
    fun <N> ifAny(
        nodes: Collection<N>,
        neighbors: DFS.Neighbors<N>,
        predicate: (N) -> Boolean,
    ): Boolean {
        var found = false
        bfs(nodes, neighbors, object : AbstractNodeHandler<N, Unit>() {
            override fun onVisit(current: N): Boolean {
                if (predicate(current)) found = true
                return !found
            }
            override fun result() = Unit
        })
        return found
    }

    // -------------------------------------------------------------------------
    // Handler interfaces / base classes
    // -------------------------------------------------------------------------

    interface NodeHandler<N, R> {
        /** 访问节点时调用；返回 false 则立即终止整个遍历。 */
        fun onVisit(current: N): Boolean
        fun result(): R
    }

    abstract class AbstractNodeHandler<N, R> : NodeHandler<N, R> {
        override fun onVisit(current: N): Boolean = true
    }
}

// =============================================================================
// Cycle detection — shared between DFS and BFS callers
// =============================================================================

object CycleDetector {

    // -------------------------------------------------------------------------
    // DFS-based cycle detection (works for directed graphs)
    // -------------------------------------------------------------------------

    /**
     * 检测有向图中是否存在环。
     *
     * 使用三色标记法：
     *   WHITE（未访问）→ GRAY（递归栈中）→ BLACK（已完成）
     * 若遇到 GRAY 节点，说明存在返回边，即有环。
     */
    fun <N> hasCycle(
        nodes: Iterable<N>,
        neighbors: DFS.Neighbors<N>,
    ): Boolean {
        val color = HashMap<N, Color>()

        fun dfs(node: N): Boolean {
            color[node] = Color.GRAY
            for (neighbor in neighbors.getNeighbors(node)) {
                when (color[neighbor]) {
                    Color.GRAY -> return true          // 返回边 → 有环
                    Color.BLACK -> Unit                 // 已完成，跳过
                    else -> if (dfs(neighbor)) return true
                }
            }
            color[node] = Color.BLACK
            return false
        }

        for (node in nodes) {
            if (color[node] == null && dfs(node)) return true
        }
        return false
    }

    /**
     * 找出有向图中所有构成环的节点。
     * 返回值是这些节点的集合（不保证顺序，也不还原具体环路径）。
     */
    fun <N> cycleNodes(
        nodes: Iterable<N>,
        neighbors: DFS.Neighbors<N>,
    ): Set<N> {
        val color = HashMap<N, Color>()
        val result = HashSet<N>()

        fun dfs(node: N, stack: LinkedHashSet<N>) {
            color[node] = Color.GRAY
            stack.add(node)
            for (neighbor in neighbors.getNeighbors(node)) {
                when (color[neighbor]) {
                    Color.GRAY -> {
                        // neighbor 是环的起点，stack 中从 neighbor 到 node 均在环上
                        var inCycle = false
                        for (n in stack) {
                            if (n == neighbor) inCycle = true
                            if (inCycle) result.add(n)
                        }
                    }
                    Color.BLACK -> Unit
                    else -> dfs(neighbor, stack)
                }
            }
            stack.remove(node)
            color[node] = Color.BLACK
        }

        for (node in nodes) {
            if (color[node] == null) dfs(node, LinkedHashSet())
        }
        return result
    }

    /**
     * 拓扑排序，若图中存在环则抛出 [CycleException]。
     * 这是对 [DFS.topologicalOrder] 的安全版本。
     */
    fun <N> topologicalOrderOrThrow(
        nodes: Iterable<N>,
        neighbors: DFS.Neighbors<N>,
    ): List<N> {
        if (hasCycle(nodes, neighbors)) {
            val cycles = cycleNodes(nodes, neighbors)
            throw CycleException("Graph contains a cycle involving nodes: $cycles")
        }
        return DFS.topologicalOrder(nodes, neighbors)
    }

    // -------------------------------------------------------------------------
    // Kahn's algorithm — BFS-based topological sort with cycle detection
    // -------------------------------------------------------------------------

    /**
     * 用 Kahn 算法（BFS）做拓扑排序。
     *
     * 相比 DFS 版本的优势：
     * - 天然迭代，无栈溢出风险
     * - 拓扑排序和环检测一步完成
     *
     * 若存在环，返回 [TopologicalResult.HasCycle]，否则返回 [TopologicalResult.Success]。
     */
    fun <N> kahnSort(
        nodes: Iterable<N>,
        neighbors: DFS.Neighbors<N>,
    ): TopologicalResult<N> {
        val allNodes = nodes.toList()

        // 计算每个节点的入度
        val inDegree = HashMap<N, Int>()
        for (node in allNodes) inDegree.getOrPut(node) { 0 }
        for (node in allNodes) {
            for (neighbor in neighbors.getNeighbors(node)) {
                inDegree[neighbor] = (inDegree[neighbor] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<N>()
        for ((node, deg) in inDegree) {
            if (deg == 0) queue.addLast(node)
        }

        val sorted = mutableListOf<N>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            sorted += current
            for (neighbor in neighbors.getNeighbors(current)) {
                val newDeg = (inDegree[neighbor] ?: 0) - 1
                inDegree[neighbor] = newDeg
                if (newDeg == 0) queue.addLast(neighbor)
            }
        }

        return if (sorted.size == allNodes.size) {
            TopologicalResult.Success(sorted)
        } else {
            // 入度始终 > 0 的节点就在环上
            val cycleNodes = inDegree.filter { it.value > 0 }.keys
            TopologicalResult.HasCycle(cycleNodes)
        }
    }

    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

    private enum class Color { GRAY, BLACK }

    sealed class TopologicalResult<N> {
        data class Success<N>(val order: List<N>) : TopologicalResult<N>()
        data class HasCycle<N>(val cycleNodes: Set<N>) : TopologicalResult<N>()
    }

    class CycleException(message: String) : IllegalStateException(message)
}