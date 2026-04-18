/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCacheStorage
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSourcesSession

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

    private fun assignDistancesFromAnalysisRoots(analysisRootNodes: List<LLSessionStructureGraphNode>) {
        val queue = ArrayDeque<LLSessionStructureGraphNode>()

        analysisRootNodes.forEach { rootNode ->
            rootNode.analysisRootDistance = 0
            queue.add(rootNode)
        }

        while (queue.isNotEmpty()) {
            val currentNode = queue.removeCfirst()
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
