package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.eclipse.lsp4j.WorkspaceFolder

data class CangjieAnalysisLifecycleContext(
    val environment: CangjieLspEnvironment,
    val descriptor: CangjieLanguageServerDescriptor,
    val workspaceState: LspWorkspaceState,
    val documentStore: LspDocumentStore,
)

data class CangjieAnalysisRequestContext(
    val environment: CangjieLspEnvironment,
    val descriptor: CangjieLanguageServerDescriptor,
    val workspaceState: LspWorkspaceState,
    val documentStore: LspDocumentStore,
) {
    fun openedDocument(uri: String): LspTextDocument? = documentStore.get(uri)

    fun workspaceFolders(): List<WorkspaceFolder> = workspaceState.workspaceFolders()
}
