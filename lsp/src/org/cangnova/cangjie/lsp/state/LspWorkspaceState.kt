package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder

class LspWorkspaceState {
    @Volatile
    private var initializeParams: InitializeParams? = null

    @Volatile
    private var projectConfiguration: LspProjectConfiguration = LspProjectConfiguration(
        workspaceModules = emptyList(),
        stdlibSearchPaths = emptyList(),
        librarySearchPaths = emptyList(),
    )

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

        projectConfiguration = LspProjectConfiguration.fromInitializeParams(params).also {
            it.applyLibrarySearchProperties()
        }
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
        initializeParams?.let { params ->
            projectConfiguration = LspProjectConfiguration.fromInitializeParams(
                params = params,
                workspaceFoldersOverride = workspaceFolders.values.toList(),
            ).also { configuration ->
                configuration.applyLibrarySearchProperties()
            }
        }
    }

    @Synchronized
    fun workspaceFolders(): List<WorkspaceFolder> = workspaceFolders.values.toList()

    fun initializeParams(): InitializeParams? = initializeParams

    fun projectConfiguration(): LspProjectConfiguration = projectConfiguration

    /**
     * `publishDiagnostics.versionSupport` 是客户端显式协商位。
     *
     * 若客户端未声明支持，服务端必须省略通知里的 `version` 字段，
     * 否则可能导致成熟客户端忽略整条诊断通知。
     */
    fun supportsPublishDiagnosticsVersion(): Boolean {
        return initializeParams
            ?.capabilities
            ?.textDocument
            ?.publishDiagnostics
            ?.versionSupport == true
    }

    private fun inferWorkspaceName(uri: String): String {
        val trimmed = uri.trimEnd('/')
        val slashIndex = trimmed.lastIndexOf('/')
        return if (slashIndex >= 0) trimmed.substring(slashIndex + 1) else trimmed
    }
}
