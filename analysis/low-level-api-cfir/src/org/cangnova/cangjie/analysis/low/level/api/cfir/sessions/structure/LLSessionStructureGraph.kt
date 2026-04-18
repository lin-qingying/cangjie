/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession

/**
 * A graph of [LLCfirSession]s following the dependency structure of the currently cached sessions. Only sessions which exist in the cache
 * are included in the graph.
 *
 * The purpose of the graph, once written to GraphML with [LLSessionStructureWriter], is to visualize and analyze the structure of
 * cached sessions and their memory usage.
 *
 * The graph has the following features:
 *
 * - Each node in the graph represents a session with relevant statistics like its weight. The label of the node is the session module's
 *   name.
 * - Directed edges represent dependencies between sessions.
 */
internal class LLSessionStructureGraph(
    val nodesBySession: Map<LLCfirSession, LLSessionStructureGraphNode>,
)

/**
 * Represents a node in the [LLSessionStructureGraph].
 *
 * Each node corresponds to an [LLCfirSession] with its associated [LLSessionStatistics] and dependencies. Dependencies are represented as
 * child nodes, forming a directed graph structure.
 *
 * @property id A unique numeric ID. It is used to link the node in GraphML.
 */
internal class LLSessionStructureGraphNode(
    val id: Int,
    val session: LLCfirSession,
    val statistics: LLSessionStatistics,
) {
    /**
     * The graph can be circular, so we need to assign dependencies some time after all nodes have been created.
     */
    var dependencies: List<LLSessionStructureGraphNode> = emptyList()

    /**
     * The session's distance from the nearest session that has a corresponding analysis session.
     *
     * A value of 0 means that this session has a corresponding analysis session.
     */
    var analysisRootDistance: Int? = null

    val label: String
        get() = when (val module = session.ktModule) {
            is CaSourceModule -> "[SRC] ${module.name}"
            is CaLibraryModule -> "[LIB] ${module.libraryName}"
            else -> module.moduleDescription
        }

    /**
     * Returns true if this node should be included in the GraphML output.
     *
     * Even nodes with no weight need to be included in [LLSessionStructureGraph] to properly calculate session properties, like the
     * distance to the nearest analysis session.
     */
    val isSignificant: Boolean
        get() = statistics.weight > 0
}
