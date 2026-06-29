package org.cangnova.cangjie.utils

/**
 * 面向图结构的深度优先遍历工具。
 *
 * 调用方通过 [Neighbors] 提供边关系，通过 [Visited] 控制去重，通过 [NodeHandler] 聚合遍历结果。
 */
object DFS {
    /**
     * 从多个起点执行深度优先遍历。
     */
    fun <N, R> dfs(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, R>,
    ): R {
        for (node in nodes) {
            doDfs(node, neighbors, visited, handler)
        }
        return handler.result()
    }

    /**
     * 使用默认 visited 集合从多个起点执行深度优先遍历。
     */
    fun <N, R> dfs(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        handler: NodeHandler<N, R>,
    ): R {
        return dfs(nodes, neighbors, VisitedWithSet(), handler)
    }

    /**
     * 判断从给定起点集合可达的节点中是否存在满足 [predicate] 的节点。
     */
    fun <N> ifAny(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        predicate: (N) -> Boolean,
    ): Boolean {
        var found = false
        return dfs(nodes, neighbors, object : AbstractNodeHandler<N, Boolean>() {
            override fun beforeChildren(current: N): Boolean {
                if (predicate(current)) {
                    found = true
                }
                return !found
            }

            override fun result(): Boolean = found
        })
    }

    /**
     * 从单个起点执行深度优先遍历，并使用调用方提供的 visited 策略。
     */
    fun <N, R> dfsFromNode(
        node: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, R>,
    ): R {
        doDfs(node, neighbors, visited, handler)
        return handler.result()
    }

    /**
     * 从单个起点执行深度优先遍历，不关心遍历结果。
     */
    fun <N> dfsFromNode(
        node: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
    ) {
        dfsFromNode(node, neighbors, visited, object : AbstractNodeHandler<N, Unit?>() {
            override fun result(): Unit? = null
        })
    }

    /**
     * 使用 DFS 后序结果计算拓扑顺序。
     */
    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
    ): List<N> {
        val handler = TopologicalOrder<N>()
        for (node in nodes) {
            doDfs(node, neighbors, visited, handler)
        }
        return handler.result()
    }

    /**
     * 使用默认 visited 集合计算拓扑顺序。
     */
    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: Neighbors<N>,
    ): List<N> {
        return topologicalOrder(nodes, neighbors, VisitedWithSet())
    }

    /**
     * 使用函数式邻接点提供器计算拓扑顺序。
     */
    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: (N) -> Iterable<N>,
    ): List<N> {
        return topologicalOrder(nodes, object : Neighbors<N> {
            override fun getNeighbors(current: N): Iterable<N> = neighbors(current)
        })
    }

    /**
     * 执行单个节点的递归 DFS。
     *
     * 该函数先调用 [NodeHandler.beforeChildren]，再递归访问邻接点，最后调用 [NodeHandler.afterChildren]。
     */
    fun <N> doDfs(
        current: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, *>,
    ) {
        if (!visited.checkAndMarkVisited(current)) return
        if (!handler.beforeChildren(current)) return

        for (neighbor in neighbors.getNeighbors(current)) {
            doDfs(neighbor, neighbors, visited, handler)
        }
        handler.afterChildren(current)
    }

    /**
     * DFS 节点访问回调。
     */
    interface NodeHandler<N, R> {
        /**
         * 子节点遍历前调用；返回 false 会跳过该节点的子树。
         */
        fun beforeChildren(current: N): Boolean
        /**
         * 子节点遍历后调用。
         */
        fun afterChildren(current: N)
        /**
         * 返回遍历聚合结果。
         */
        fun result(): R
    }

    /**
     * 图节点的邻接点提供器。
     */
    interface Neighbors<N> {
        /**
         * 返回 [current] 的直接邻接节点。
         */
        fun getNeighbors(current: N): Iterable<N>
    }

    /**
     * DFS/BFS 共享的访问标记策略。
     */
    interface Visited<N> {
        /**
         * 检查节点是否已经访问，并在未访问时标记为已访问。
         */
        fun checkAndMarkVisited(current: N): Boolean
    }

    /**
     * DFS 节点访问回调的默认基类。
     */
    abstract class AbstractNodeHandler<N, R> : NodeHandler<N, R> {
        /**
         * 默认继续遍历子节点。
         */
        override fun beforeChildren(current: N): Boolean = true
        /**
         * 默认不处理子节点遍历后的回调。
         */
        override fun afterChildren(current: N) = Unit
    }

    /**
     * 基于可变集合实现的 visited 策略。
     */
    class VisitedWithSet<N>(
        /**
         * 保存已访问节点的集合。
         */
        private val visited: MutableSet<N> = hashSetOf(),
    ) : Visited<N> {
        /**
         * 如果 [current] 尚未出现则标记并返回 true。
         */
        override fun checkAndMarkVisited(current: N): Boolean = visited.add(current)
    }

    /**
     * 将遍历结果收集到可变集合的节点处理器基类。
     */
    abstract class CollectingNodeHandler<N, R, C : MutableCollection<R>>(
        /**
         * 保存遍历结果的目标集合。
         */
        protected val collected: C,
    ) : AbstractNodeHandler<N, C>() {
        /**
         * 返回已收集的结果集合。
         */
        override fun result(): C = collected
    }

    /**
     * 以列表形式返回结果的节点处理器基类。
     */
    abstract class NodeHandlerWithListResult<N, R> : CollectingNodeHandler<N, R, ArrayDeque<R>>(ArrayDeque())

    /**
     * 通过后序入队计算拓扑顺序的节点处理器。
     */
    class TopologicalOrder<N> : AbstractNodeHandler<N, List<N>>() {
        /**
         * 保存反向后序结果的队列。
         */
        private val result = ArrayDeque<N>()

        /**
         * 在子节点处理完成后把当前节点插入结果头部。
         */
        override fun afterChildren(current: N) {
            result.addFirst(current)
        }

        /**
         * 返回拓扑排序结果。
         */
        override fun result(): List<N> = result.toList()
    }
}
