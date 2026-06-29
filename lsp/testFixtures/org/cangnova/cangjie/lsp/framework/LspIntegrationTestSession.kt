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
    /**
     * 底层真实 JSON-RPC 测试连接。
     */
    private val connection: LspIntegrationTestConnection,
) : AutoCloseable {
    /**
     * 返回 initialize 请求的结果。
     */
    fun initializeResult(): InitializeResult = connection.initializeResult()

    /**
     * 发送 textDocument/didOpen 通知。
     */
    fun openDocument(
        uri: String,
        text: String,
        version: Int = 1,
        languageId: String = "cangjie",
    ) {
        connection.openDocument(uri, text, version, languageId)
    }

    /**
     * 发送整篇文档替换式 didChange 通知。
     */
    fun changeDocument(
        uri: String,
        newText: String,
        version: Int,
    ) {
        connection.changeDocument(uri, newText, version)
    }

    /**
     * 发送自定义 didChange 通知。
     */
    fun changeDocument(params: DidChangeTextDocumentParams) {
        connection.changeDocument(params)
    }

    /**
     * 发送 textDocument/didSave 通知。
     */
    fun saveDocument(uri: String, text: String? = null) {
        connection.saveDocument(uri, text)
    }

    /**
     * 发送 textDocument/didClose 通知。
     */
    fun closeDocument(uri: String) {
        connection.closeDocument(uri)
    }

    /** 发起 completion 请求。 */
    fun completion(params: CompletionParams): Either<List<CompletionItem>, CompletionList> = connection.completion(params)

    /** 发起 hover 请求。 */
    fun hover(params: HoverParams): Hover = connection.hover(params)

    /** 发起 signatureHelp 请求。 */
    fun signatureHelp(params: SignatureHelpParams): SignatureHelp = connection.signatureHelp(params)

    /** 发起 declaration 请求。 */
    fun declaration(params: DeclarationParams): Either<List<Location>, List<LocationLink>> = connection.declaration(params)

    /** 发起 definition 请求。 */
    fun definition(params: DefinitionParams): Either<List<Location>, List<LocationLink>> = connection.definition(params)

    /** 发起 typeDefinition 请求。 */
    fun typeDefinition(params: TypeDefinitionParams): Either<List<Location>, List<LocationLink>> =
        connection.typeDefinition(params)

    /** 发起 implementation 请求。 */
    fun implementation(params: ImplementationParams): Either<List<Location>, List<LocationLink>> =
        connection.implementation(params)

    /** 发起 references 请求。 */
    fun references(params: ReferenceParams): List<Location> = connection.references(params)

    /** 发起 documentHighlight 请求。 */
    fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight> =
        connection.documentHighlight(params)

    /** 发起 documentSymbol 请求。 */
    fun documentSymbol(params: DocumentSymbolParams): List<Either<SymbolInformation, DocumentSymbol>> =
        connection.documentSymbol(params)

    /** 发起 codeAction 请求。 */
    fun codeAction(params: CodeActionParams): List<Either<Command, CodeAction>> = connection.codeAction(params)

    /** 发起 formatting 请求。 */
    fun formatting(params: DocumentFormattingParams): List<TextEdit> = connection.formatting(params)

    /** 发起 rangeFormatting 请求。 */
    fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit> = connection.rangeFormatting(params)

    /** 发起 rename 请求。 */
    fun rename(params: RenameParams): WorkspaceEdit = connection.rename(params)

    /** 发起 prepareRename 请求。 */
    fun prepareRename(
        params: PrepareRenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> = connection.prepareRename(params)

    /** 发起 foldingRange 请求。 */
    fun foldingRange(params: FoldingRangeRequestParams): List<FoldingRange> = connection.foldingRange(params)

    /** 发起 selectionRange 请求。 */
    fun selectionRange(params: SelectionRangeParams): List<SelectionRange> = connection.selectionRange(params)

    /** 发起 semanticTokens/full 请求。 */
    fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens = connection.semanticTokensFull(params)

    /** 发起 semanticTokens/range 请求。 */
    fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens = connection.semanticTokensRange(params)

    /** 发起 inlayHint 请求。 */
    fun inlayHint(params: InlayHintParams): List<InlayHint> = connection.inlayHint(params)

    /** 发起 textDocument/diagnostic 请求。 */
    fun documentDiagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport =
        connection.documentDiagnostic(params)

    /** 发送 workspace/didChangeConfiguration 通知。 */
    fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        connection.didChangeConfiguration(params)
    }

    /** 发送 workspace/didChangeWatchedFiles 通知。 */
    fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        connection.didChangeWatchedFiles(params)
    }

    /** 发送 workspace/didChangeWorkspaceFolders 通知。 */
    fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        connection.didChangeWorkspaceFolders(params)
    }

    /** 发起 workspace/executeCommand 请求。 */
    fun executeCommand(params: ExecuteCommandParams): Any = connection.executeCommand(params)

    /** 发起 workspace/symbol 请求。 */
    fun workspaceSymbol(params: WorkspaceSymbolParams): Either<List<SymbolInformation>, List<WorkspaceSymbol>> =
        connection.workspaceSymbol(params)

    /** 发起 workspace/diagnostic 请求。 */
    fun workspaceDiagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport =
        connection.workspaceDiagnostic(params)

    /** 发送 notebook/didOpen 通知。 */
    fun notebookDidOpen(params: DidOpenNotebookDocumentParams = DidOpenNotebookDocumentParams()) {
        connection.notebookDidOpen(params)
    }

    /** 发送 notebook/didChange 通知。 */
    fun notebookDidChange(params: DidChangeNotebookDocumentParams = DidChangeNotebookDocumentParams()) {
        connection.notebookDidChange(params)
    }

    /** 发送 notebook/didSave 通知。 */
    fun notebookDidSave(params: DidSaveNotebookDocumentParams = DidSaveNotebookDocumentParams()) {
        connection.notebookDidSave(params)
    }

    /** 发送 notebook/didClose 通知。 */
    fun notebookDidClose(params: DidCloseNotebookDocumentParams = DidCloseNotebookDocumentParams()) {
        connection.notebookDidClose(params)
    }

    /** 发送 cancelProgress 通知。 */
    fun cancelProgress(token: String = "contract-token") {
        connection.cancelProgress(token)
    }

    /** 构造 completion 参数。 */
    fun completionParams(uri: String, line: Int, character: Int): CompletionParams =
        connection.completionParams(uri, line, character)

    /** 构造 hover 参数。 */
    fun hoverParams(uri: String, line: Int, character: Int): HoverParams =
        connection.hoverParams(uri, line, character)

    /** 构造 signatureHelp 参数。 */
    fun signatureHelpParams(uri: String, line: Int, character: Int): SignatureHelpParams =
        connection.signatureHelpParams(uri, line, character)

    /** 构造 declaration 参数。 */
    fun declarationParams(uri: String, line: Int, character: Int): DeclarationParams =
        connection.declarationParams(uri, line, character)

    /** 构造 definition 参数。 */
    fun definitionParams(uri: String, line: Int, character: Int): DefinitionParams =
        connection.definitionParams(uri, line, character)

    /** 构造 typeDefinition 参数。 */
    fun typeDefinitionParams(uri: String, line: Int, character: Int): TypeDefinitionParams =
        connection.typeDefinitionParams(uri, line, character)

    /** 构造 implementation 参数。 */
    fun implementationParams(uri: String, line: Int, character: Int): ImplementationParams =
        connection.implementationParams(uri, line, character)

    /** 构造 references 参数。 */
    fun referenceParams(
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean = true,
    ): ReferenceParams = connection.referenceParams(uri, line, character, includeDeclaration)

    /** 构造 documentHighlight 参数。 */
    fun documentHighlightParams(uri: String, line: Int, character: Int): DocumentHighlightParams =
        connection.documentHighlightParams(uri, line, character)

    /** 构造 documentSymbol 参数。 */
    fun documentSymbolParams(uri: String): DocumentSymbolParams = connection.documentSymbolParams(uri)

    /** 构造 documentDiagnostic 参数。 */
    fun documentDiagnosticParams(uri: String): DocumentDiagnosticParams = connection.documentDiagnosticParams(uri)

    /** 等待收到指定数量的 diagnostics 通知。 */
    fun awaitDiagnosticsCount(expectedCount: Int) {
        connection.awaitDiagnosticsCount(expectedCount)
    }

    /** 等待指定 URI 的 diagnostics 通知满足条件。 */
    fun awaitPublishedDiagnostics(
        uri: String,
        predicate: (PublishDiagnosticsParams) -> Boolean = { true },
    ): PublishDiagnosticsParams = connection.awaitPublishedDiagnostics(uri, predicate)

    /** 清空已记录的 diagnostics 通知。 */
    fun clearPublishedDiagnostics() {
        connection.clearPublishedDiagnostics()
    }

    /** 返回已记录的 diagnostics 通知快照。 */
    fun publishedDiagnostics(): List<PublishDiagnosticsParams> = connection.publishedDiagnostics()

    /**
     * 关闭底层 LSP 连接。
     */
    override fun close() {
        connection.close()
    }
}
