package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder

/**
 * 保存 LSP 会话级工作区状态。
 *
 * 该状态管理 initialize 参数、workspace folders、工程配置和 shutdown 请求标记，
 * 是服务端上下文和 Analysis API 项目结构刷新时的状态来源。
 */
class LspWorkspaceState {
    /**
     * 最近一次 initialize 请求参数。
     */
    @Volatile
    private var initializeParams: InitializeParams? = null

    /**
     * 当前根据 initialize/workspace folder 状态解析出的工程配置。
     */
    @Volatile
    private var projectConfiguration: LspProjectConfiguration = LspProjectConfiguration(
        workspaceModules = emptyList(),
        stdlibSearchPaths = emptyList(),
        librarySearchPaths = emptyList(),
    )

    /**
     * 客户端是否已经请求 shutdown。
     */
    @Volatile
    private var shutdownRequested: Boolean = false

    /**
     * 以 URI 为键维护的当前 workspace folder 集合。
     */
    private val workspaceFolders = linkedMapOf<String, WorkspaceFolder>()

    /**
     * 用 initialize 参数初始化工作区状态。
     *
     * 方法重置 shutdown 标记、重建 workspace folders，并应用初始化阶段解析出的库搜索路径。
     */
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

    /**
     * 标记客户端已发送 shutdown 请求。
     */
    @Synchronized
    fun markShutdownRequested() {
        shutdownRequested = true
    }

    /**
     * 返回客户端是否已经请求 shutdown。
     */
    fun isShutdownRequested(): Boolean = shutdownRequested

    /**
     * 根据 workspace/didChangeWorkspaceFolders 更新目录集合和工程配置。
     */
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

    /**
     * 返回当前 workspace folder 的稳定快照。
     */
    @Synchronized
    fun workspaceFolders(): List<WorkspaceFolder> = workspaceFolders.values.toList()

    /**
     * 返回最近一次 initialize 参数。
     */
    fun initializeParams(): InitializeParams? = initializeParams

    /**
     * 返回当前工程配置。
     */
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

    /**
     * 从 URI 尾段推导 workspace folder 名称。
     */
    private fun inferWorkspaceName(uri: String): String {
        val trimmed = uri.trimEnd('/')
        val slashIndex = trimmed.lastIndexOf('/')
        return if (slashIndex >= 0) trimmed.substring(slashIndex + 1) else trimmed
    }
}
