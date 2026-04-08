package org.cangnova.cangjie.lsp.framework

import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3

/**
 * 测试用例侧看到的 LSP 会话门面。
 *
 * 这层只负责把协议调用组织成测试友好的 API，不自行引入任何业务判断。
 * 测试应该断言“服务端行为”，而不是在每个测试类里反复手写 JSON-RPC 参数拼装。
 */
class LspIntegrationTestSession(
    private val connection: LspIntegrationTestConnection,
) : AutoCloseable {
    fun initializeResult(): InitializeResult = connection.initializeResult()

    fun openDocument(
        uri: String,
        text: String,
        version: Int = 1,
        languageId: String = "cangjie",
    ) {
        connection.openDocument(uri, text, version, languageId)
    }

    fun changeDocument(
        uri: String,
        newText: String,
        version: Int,
    ) {
        connection.changeDocument(uri, newText, version)
    }

    fun changeDocument(params: DidChangeTextDocumentParams) {
        connection.changeDocument(params)
    }

    fun saveDocument(uri: String, text: String? = null) {
        connection.saveDocument(uri, text)
    }

    fun closeDocument(uri: String) {
        connection.closeDocument(uri)
    }

    fun completion(params: CompletionParams): Either<List<CompletionItem>, CompletionList> = connection.completion(params)

    fun hover(params: HoverParams): Hover = connection.hover(params)

    fun signatureHelp(params: SignatureHelpParams): SignatureHelp = connection.signatureHelp(params)

    fun declaration(params: DeclarationParams): Either<List<Location>, List<LocationLink>> = connection.declaration(params)

    fun definition(params: DefinitionParams): Either<List<Location>, List<LocationLink>> = connection.definition(params)

    fun typeDefinition(params: TypeDefinitionParams): Either<List<Location>, List<LocationLink>> =
        connection.typeDefinition(params)

    fun implementation(params: ImplementationParams): Either<List<Location>, List<LocationLink>> =
        connection.implementation(params)

    fun references(params: ReferenceParams): List<Location> = connection.references(params)

    fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight> =
        connection.documentHighlight(params)

    fun documentSymbol(params: DocumentSymbolParams): List<Either<SymbolInformation, DocumentSymbol>> =
        connection.documentSymbol(params)

    fun codeAction(params: CodeActionParams): List<Either<Command, CodeAction>> = connection.codeAction(params)

    fun formatting(params: DocumentFormattingParams): List<TextEdit> = connection.formatting(params)

    fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit> = connection.rangeFormatting(params)

    fun rename(params: RenameParams): WorkspaceEdit = connection.rename(params)

    fun prepareRename(
        params: PrepareRenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> = connection.prepareRename(params)

    fun foldingRange(params: FoldingRangeRequestParams): List<FoldingRange> = connection.foldingRange(params)

    fun selectionRange(params: SelectionRangeParams): List<SelectionRange> = connection.selectionRange(params)

    fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens = connection.semanticTokensFull(params)

    fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens = connection.semanticTokensRange(params)

    fun inlayHint(params: InlayHintParams): List<InlayHint> = connection.inlayHint(params)

    fun documentDiagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport =
        connection.documentDiagnostic(params)

    fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        connection.didChangeConfiguration(params)
    }

    fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        connection.didChangeWatchedFiles(params)
    }

    fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        connection.didChangeWorkspaceFolders(params)
    }

    fun executeCommand(params: ExecuteCommandParams): Any = connection.executeCommand(params)

    fun workspaceSymbol(params: WorkspaceSymbolParams): Either<List<SymbolInformation>, List<WorkspaceSymbol>> =
        connection.workspaceSymbol(params)

    fun workspaceDiagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport =
        connection.workspaceDiagnostic(params)

    fun notebookDidOpen(params: DidOpenNotebookDocumentParams = DidOpenNotebookDocumentParams()) {
        connection.notebookDidOpen(params)
    }

    fun notebookDidChange(params: DidChangeNotebookDocumentParams = DidChangeNotebookDocumentParams()) {
        connection.notebookDidChange(params)
    }

    fun notebookDidSave(params: DidSaveNotebookDocumentParams = DidSaveNotebookDocumentParams()) {
        connection.notebookDidSave(params)
    }

    fun notebookDidClose(params: DidCloseNotebookDocumentParams = DidCloseNotebookDocumentParams()) {
        connection.notebookDidClose(params)
    }

    fun cancelProgress(token: String = "contract-token") {
        connection.cancelProgress(token)
    }

    fun completionParams(uri: String, line: Int, character: Int): CompletionParams =
        connection.completionParams(uri, line, character)

    fun hoverParams(uri: String, line: Int, character: Int): HoverParams =
        connection.hoverParams(uri, line, character)

    fun signatureHelpParams(uri: String, line: Int, character: Int): SignatureHelpParams =
        connection.signatureHelpParams(uri, line, character)

    fun declarationParams(uri: String, line: Int, character: Int): DeclarationParams =
        connection.declarationParams(uri, line, character)

    fun definitionParams(uri: String, line: Int, character: Int): DefinitionParams =
        connection.definitionParams(uri, line, character)

    fun typeDefinitionParams(uri: String, line: Int, character: Int): TypeDefinitionParams =
        connection.typeDefinitionParams(uri, line, character)

    fun implementationParams(uri: String, line: Int, character: Int): ImplementationParams =
        connection.implementationParams(uri, line, character)

    fun referenceParams(
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean = true,
    ): ReferenceParams = connection.referenceParams(uri, line, character, includeDeclaration)

    fun documentHighlightParams(uri: String, line: Int, character: Int): DocumentHighlightParams =
        connection.documentHighlightParams(uri, line, character)

    fun documentSymbolParams(uri: String): DocumentSymbolParams = connection.documentSymbolParams(uri)

    fun documentDiagnosticParams(uri: String): DocumentDiagnosticParams = connection.documentDiagnosticParams(uri)

    fun awaitDiagnosticsCount(expectedCount: Int) {
        connection.awaitDiagnosticsCount(expectedCount)
    }

    fun clearPublishedDiagnostics() {
        connection.clearPublishedDiagnostics()
    }

    fun publishedDiagnostics(): List<PublishDiagnosticsParams> = connection.publishedDiagnostics()

    override fun close() {
        connection.close()
    }
}
