package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder

class LspWorkspaceState {
    @Volatile
    private var initializeParams: InitializeParams? = null

    @Volatile
    private var shutdownRequested: Boolean = false

    private val workspaceFolders = linkedMapOf<String, WorkspaceFolder>()

    @Synchronized
    fun initialize(params: InitializeParams) {
        initializeParams = params
        shutdownRequested = false
        workspaceFolders.clear()

        val folders = params.workspaceFolders
            ?: params.rootUri?.let { listOf(WorkspaceFolder(it, inferWorkspaceName(it))) }
            ?: emptyList()
        folders.forEach { workspaceFolders[it.uri] = it }
    }

    @Synchronized
    fun markShutdownRequested() {
        shutdownRequested = true
    }

    fun isShutdownRequested(): Boolean = shutdownRequested

    @Synchronized
    fun updateWorkspaceFolders(
        added: List<WorkspaceFolder>,
        removed: List<WorkspaceFolder>,
    ) {
        removed.forEach { workspaceFolders.remove(it.uri) }
        added.forEach { workspaceFolders[it.uri] = it }
    }

    @Synchronized
    fun workspaceFolders(): List<WorkspaceFolder> = workspaceFolders.values.toList()

    fun initializeParams(): InitializeParams? = initializeParams

    private fun inferWorkspaceName(uri: String): String {
        val trimmed = uri.trimEnd('/')
        val slashIndex = trimmed.lastIndexOf('/')
        return if (slashIndex >= 0) trimmed.substring(slashIndex + 1) else trimmed
    }
}
