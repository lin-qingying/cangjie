

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCacheStorage
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSourcesSession

/**
 * 从 session cache storage 构建 session structure 图。
 */
internal object LLSessionStructureGraphBuilder {
    /**
     * Builds an [LLSessionStructureGraph] from all sessions in the given [storage].
     *
     * @param analysisRoots The list of sessions which currently have an associated cached analysis session. They are the root sessions from
     *  which resolution is started.
     */
    fun buildGraph(storage: LLCfirSessionCacheStorage, analysisRoots: List<LLCfirSession>): LLSessionStructureGraph {
        val sourceSessions = storage.sourceCache.values
        val librarySessions = storage.binaryCache.values

        val sessions = sourceSessions + librarySessions

        val nodesBySession = sessions
            .mapIndexed { index, session ->
                LLSessionStructureGraphNode(
                    index,
                    session,
                    LLSessionStatisticsCalculator.calculateSessionStatistics(session),
                )
            }
            .associateBy { it.session }

        assignDependencies(nodesBySession)

        val graph = LLSessionStructureGraph(nodesBySession)

        val analysisRootNodes = analysisRoots.mapNotNull { nodesBySession[it] }
        assignDistancesFromAnalysisRoots(analysisRootNodes)

        return graph
    }

    /**
     * 为已创建的节点填充源码 session 依赖边。
     *
     * 只使用已经初始化的 lazy dependencies，避免为了统计输出触发新的 session 计算。
     */
    private fun assignDependencies(nodesBySession: Map<LLCfirSession, LLSessionStructureGraphNode>) {
        nodesBySession.values.forEach { node ->
            val session = node.session
            if (session !is LLCfirSourcesSession) return@forEach

            // The graph should only contain already cached sessions. Furthermore, we might be outside a read action here, so we shouldn't
            // compute lazily calculated dependencies.
            if (LLCfirSourcesSession::dependencies.isLazyInitialized(session)) {
                node.dependencies = session.dependencies.mapNotNull(nodesBySession::get)
            }
        }
    }

    /**
     * 从 analysis root 节点出发计算每个节点到最近 analysis root 的距离。
     */
    private fun assignDistancesFromAnalysisRoots(analysisRootNodes: List<LLSessionStructureGraphNode>) {
        val queue = ArrayDeque<LLSessionStructureGraphNode>()

        analysisRootNodes.forEach { rootNode ->
            rootNode.analysisRootDistance = 0
            queue.add(rootNode)
        }

        while (queue.isNotEmpty()) {
            val currentNode = queue.removeFirst()
            val currentDistance = currentNode.analysisRootDistance ?: 0
            val newDistance = currentDistance + 1

            currentNode.dependencies.forEach { dependency ->
                // Only update if we haven't visited this node yet or if we found a shorter path. This also breaks cycles.
                val currentDependencyDistance = dependency.analysisRootDistance
                if (currentDependencyDistance == null || currentDependencyDistance > newDistance) {
                    dependency.analysisRootDistance = newDistance
                    queue.add(dependency)
                }
            }
        }
    }
}
