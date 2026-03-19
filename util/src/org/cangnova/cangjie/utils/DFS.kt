package org.cangnova.cangjie.utils

object DFS {
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

    fun <N, R> dfs(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        handler: NodeHandler<N, R>,
    ): R {
        return dfs(nodes, neighbors, VisitedWithSet(), handler)
    }

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

    fun <N, R> dfsFromNode(
        node: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, R>,
    ): R {
        doDfs(node, neighbors, visited, handler)
        return handler.result()
    }

    fun <N> dfsFromNode(
        node: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
    ) {
        dfsFromNode(node, neighbors, visited, object : AbstractNodeHandler<N, Unit?>() {
            override fun result(): Unit? = null
        })
    }

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

    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: Neighbors<N>,
    ): List<N> {
        return topologicalOrder(nodes, neighbors, VisitedWithSet())
    }

    // Convenience overload used by local Kotlin call sites.
    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: (N) -> Iterable<N>,
    ): List<N> {
        return topologicalOrder(nodes, object : Neighbors<N> {
            override fun getNeighbors(current: N): Iterable<N> = neighbors(current)
        })
    }

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

    interface NodeHandler<N, R> {
        fun beforeChildren(current: N): Boolean
        fun afterChildren(current: N)
        fun result(): R
    }

    interface Neighbors<N> {
        fun getNeighbors(current: N): Iterable<N>
    }

    interface Visited<N> {
        fun checkAndMarkVisited(current: N): Boolean
    }

    abstract class AbstractNodeHandler<N, R> : NodeHandler<N, R> {
        override fun beforeChildren(current: N): Boolean = true
        override fun afterChildren(current: N) = Unit
    }

    class VisitedWithSet<N>(
        private val visited: MutableSet<N> = hashSetOf(),
    ) : Visited<N> {
        override fun checkAndMarkVisited(current: N): Boolean = visited.add(current)
    }

    abstract class CollectingNodeHandler<N, R, C : MutableCollection<R>>(
        protected val collected: C,
    ) : AbstractNodeHandler<N, C>() {
        override fun result(): C = collected
    }

    abstract class NodeHandlerWithListResult<N, R> : CollectingNodeHandler<N, R, ArrayDeque<R>>(ArrayDeque())

    class TopologicalOrder<N> : AbstractNodeHandler<N, List<N>>() {
        private val result = ArrayDeque<N>()

        override fun afterChildren(current: N) {
            result.addFirst(current)
        }

        override fun result(): List<N> = result.toList()
    }
}