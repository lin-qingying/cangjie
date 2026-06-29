package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.eclipse.lsp4j.WorkspaceFolder

/**
 * Analysis facade 生命周期级上下文。
 *
 * 该上下文在 facade 创建时固定，提供环境、服务端描述、工作区状态和文档存储等长期依赖。
 */
data class CangjieAnalysisLifecycleContext(
    /**
     * 当前 LSP 会话的编译器和 IntelliJ 环境。
     */
    val environment: CangjieLspEnvironment,

    /**
     * 当前服务端的静态能力描述。
     */
    val descriptor: CangjieLanguageServerDescriptor,

    /**
     * 当前工作区初始化和配置状态。
     */
    val workspaceState: LspWorkspaceState,

    /**
     * 当前打开文档的快照存储。
     */
    val documentStore: LspDocumentStore,
)

/**
 * 单次 Analysis facade 请求上下文。
 *
 * 该上下文在协议请求处理时创建，保证语义操作读取请求开始时的最新 workspace/document 状态。
 */
data class CangjieAnalysisRequestContext(
    /**
     * 当前 LSP 会话的编译器和 IntelliJ 环境。
     */
    val environment: CangjieLspEnvironment,

    /**
     * 当前服务端的静态能力描述。
     */
    val descriptor: CangjieLanguageServerDescriptor,

    /**
     * 当前工作区初始化和配置状态。
     */
    val workspaceState: LspWorkspaceState,

    /**
     * 当前打开文档的快照存储。
     */
    val documentStore: LspDocumentStore,
) {
    /**
     * 查询指定 URI 当前打开的文档快照。
     */
    fun openedDocument(uri: String): LspTextDocument? = documentStore.get(uri)

    /**
     * 返回当前工作区目录列表。
     */
    fun workspaceFolders(): List<WorkspaceFolder> = workspaceState.workspaceFolders()
}
