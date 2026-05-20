package org.cangnova.cangjie.lsp.testkit

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.server.CangjieLanguageServer
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * 基于真实 JSON-RPC 流的 LSP 双端连接。
 *
 * 这层不是某几个测试专用的轻量包装，而是协议矩阵测试的统一入口：
 * 1. 所有请求/通知都从这里发出，保证测试经过真实的 LSP4J 编解码链；
 * 2. 统一超时、诊断收集与常见参数构造，减少重复样板；
 * 3. 当服务端协议面扩张时，只需要在这里补一个 helper，测试层即可复用。
 */
class LspIntegrationTestConnection private constructor(
    options: CangjieLspServerOptions,
) : AutoCloseable {
    private val clientInput = PipedInputStream(BUFFER_SIZE)
    private val serverOutput = PipedOutputStream(clientInput)
    private val serverInput = PipedInputStream(BUFFER_SIZE)
    private val clientOutput = PipedOutputStream(serverInput)

    private val client = RecordingLanguageClient()
    private val server: CangjieLanguageServer = CangjieLanguageServer(options)
    private val serverLauncher = LSPLauncher.createServerLauncher(server, serverInput, serverOutput)
    private val clientLauncher = LSPLauncher.createClientLauncher(client, clientInput, clientOutput)
    private val serverListening: Future<*> = serverLauncher.startListening()
    private val clientListening: Future<*> = clientLauncher.startListening()
    private val serverProxy: LanguageServer = clientLauncher.remoteProxy
    private var initializeResult: InitializeResult? = null

    init {
        server.connect(serverLauncher.remoteProxy)
    }

    fun initialize(rootUri: String = "file:///workspace"): InitializeResult {
        return initialize(InitializeParams().apply { this.rootUri = rootUri })
    }

    fun initialize(params: InitializeParams): InitializeResult {
        val result = awaitFuture(serverProxy.initialize(params))
        initializeResult = result
        return result
    }

    fun initialized() {
        serverProxy.initialized(InitializedParams())
    }

    fun initializeResult(): InitializeResult =
        initializeResult ?: error("LSP connection has not been initialized")

    fun openDocument(
        uri: String,
        text: String,
        version: Int = 1,
        languageId: String = "cangjie",
    ) {
        serverProxy.textDocumentService.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(uri, languageId, version, text),
            ),
        )
    }

    fun changeDocument(
        uri: String,
        newText: String,
        version: Int,
    ) {
        changeDocument(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier(uri, version),
                listOf(TextDocumentContentChangeEvent(newText)),
            ),
        )
    }

    fun changeDocument(params: DidChangeTextDocumentParams) {
        serverProxy.textDocumentService.didChange(params)
    }

    fun saveDocument(uri: String, text: String? = null) {
        serverProxy.textDocumentService.didSave(
            DidSaveTextDocumentParams(TextDocumentIdentifier(uri), text),
        )
    }

    fun closeDocument(uri: String) {
        serverProxy.textDocumentService.didClose(
            DidCloseTextDocumentParams(TextDocumentIdentifier(uri)),
        )
    }

    fun completion(params: CompletionParams): Either<List<CompletionItem>, CompletionList> {
        return awaitFuture(serverProxy.textDocumentService.completion(params))
    }

    fun hover(params: HoverParams): Hover = awaitFuture(serverProxy.textDocumentService.hover(params))

    fun signatureHelp(params: SignatureHelpParams): SignatureHelp {
        return awaitFuture(serverProxy.textDocumentService.signatureHelp(params))
    }

    fun declaration(params: DeclarationParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.declaration(params))
    }

    fun definition(params: DefinitionParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.definition(params))
    }

    fun typeDefinition(params: TypeDefinitionParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.typeDefinition(params))
    }

    fun implementation(params: ImplementationParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.implementation(params))
    }

    fun references(params: ReferenceParams): List<Location> {
        return awaitFuture(serverProxy.textDocumentService.references(params))
    }

    fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight> {
        return awaitFuture(serverProxy.textDocumentService.documentHighlight(params))
    }

    fun documentSymbol(params: DocumentSymbolParams): List<Either<SymbolInformation, DocumentSymbol>> {
        return awaitFuture(serverProxy.textDocumentService.documentSymbol(params))
    }

    fun codeAction(params: CodeActionParams): List<Either<Command, CodeAction>> {
        return awaitFuture(serverProxy.textDocumentService.codeAction(params))
    }

    fun formatting(params: DocumentFormattingParams): List<TextEdit> {
        return awaitFuture(serverProxy.textDocumentService.formatting(params))
    }

    fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit> {
        return awaitFuture(serverProxy.textDocumentService.rangeFormatting(params))
    }

    fun rename(params: RenameParams): WorkspaceEdit {
        return awaitFuture(serverProxy.textDocumentService.rename(params))
    }

    fun prepareRename(
        params: PrepareRenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> {
        return awaitFuture(serverProxy.textDocumentService.prepareRename(params))
    }

    fun foldingRange(params: FoldingRangeRequestParams): List<FoldingRange> {
        return awaitFuture(serverProxy.textDocumentService.foldingRange(params))
    }

    fun selectionRange(params: SelectionRangeParams): List<SelectionRange> {
        return awaitFuture(serverProxy.textDocumentService.selectionRange(params))
    }

    fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens {
        return awaitFuture(serverProxy.textDocumentService.semanticTokensFull(params))
    }

    fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens {
        return awaitFuture(serverProxy.textDocumentService.semanticTokensRange(params))
    }

    fun inlayHint(params: InlayHintParams): List<InlayHint> {
        return awaitFuture(serverProxy.textDocumentService.inlayHint(params))
    }

    fun documentDiagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport {
        return awaitFuture(serverProxy.textDocumentService.diagnostic(params))
    }

    fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        serverProxy.workspaceService.didChangeConfiguration(params)
    }

    fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        serverProxy.workspaceService.didChangeWatchedFiles(params)
    }

    fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        serverProxy.workspaceService.didChangeWorkspaceFolders(params)
    }

    fun executeCommand(params: ExecuteCommandParams): Any {
        return awaitFuture(serverProxy.workspaceService.executeCommand(params))
    }

    fun workspaceSymbol(params: WorkspaceSymbolParams): Either<List<SymbolInformation>, List<WorkspaceSymbol>> {
        return awaitFuture(serverProxy.workspaceService.symbol(params))
    }

    fun workspaceDiagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport {
        return awaitFuture(serverProxy.workspaceService.diagnostic(params))
    }

    fun notebookDidOpen(params: DidOpenNotebookDocumentParams = DidOpenNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didOpen(params)
    }

    fun notebookDidChange(params: DidChangeNotebookDocumentParams = DidChangeNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didChange(params)
    }

    fun notebookDidSave(params: DidSaveNotebookDocumentParams = DidSaveNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didSave(params)
    }

    fun notebookDidClose(params: DidCloseNotebookDocumentParams = DidCloseNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didClose(params)
    }

    fun cancelProgress(token: String = "contract-token") {
        serverProxy.cancelProgress(WorkDoneProgressCancelParams(Either.forLeft(token)))
    }

    fun completionParams(uri: String, line: Int, character: Int): CompletionParams =
        CompletionParams(TextDocumentIdentifier(uri), Position(line, character))

    fun hoverParams(uri: String, line: Int, character: Int): HoverParams =
        HoverParams(TextDocumentIdentifier(uri), Position(line, character))

    fun signatureHelpParams(uri: String, line: Int, character: Int): SignatureHelpParams =
        SignatureHelpParams(TextDocumentIdentifier(uri), Position(line, character))

    fun declarationParams(uri: String, line: Int, character: Int): DeclarationParams =
        DeclarationParams(TextDocumentIdentifier(uri), Position(line, character))

    fun definitionParams(uri: String, line: Int, character: Int): DefinitionParams =
        DefinitionParams(TextDocumentIdentifier(uri), Position(line, character))

    fun typeDefinitionParams(uri: String, line: Int, character: Int): TypeDefinitionParams =
        TypeDefinitionParams(TextDocumentIdentifier(uri), Position(line, character))

    fun implementationParams(uri: String, line: Int, character: Int): ImplementationParams =
        ImplementationParams(TextDocumentIdentifier(uri), Position(line, character))

    fun referenceParams(
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean = true,
    ): ReferenceParams = ReferenceParams(
        TextDocumentIdentifier(uri),
        Position(line, character),
        ReferenceContext(includeDeclaration),
    )

    fun documentHighlightParams(uri: String, line: Int, character: Int): DocumentHighlightParams =
        DocumentHighlightParams(TextDocumentIdentifier(uri), Position(line, character))

    fun documentSymbolParams(uri: String): DocumentSymbolParams =
        DocumentSymbolParams(TextDocumentIdentifier(uri))

    fun documentDiagnosticParams(uri: String): DocumentDiagnosticParams =
        DocumentDiagnosticParams(TextDocumentIdentifier(uri))

    fun clearPublishedDiagnostics() {
        client.publishedDiagnostics.clear()
    }

    fun publishedDiagnostics(): List<PublishDiagnosticsParams> = client.publishedDiagnostics.toList()

    fun awaitDiagnosticsCount(expectedCount: Int) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (System.nanoTime() < deadline) {
            if (client.publishedDiagnostics.size >= expectedCount) return
            Thread.sleep(20)
        }
        error("Timed out waiting for $expectedCount diagnostics notifications, actual=${client.publishedDiagnostics.size}")
    }

    fun awaitPublishedDiagnostics(
        uri: String,
        predicate: (PublishDiagnosticsParams) -> Boolean = { true },
    ): PublishDiagnosticsParams {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        var lastForUri: PublishDiagnosticsParams? = null
        while (System.nanoTime() < deadline) {
            val published = client.publishedDiagnostics.lastOrNull { diagnostics -> diagnostics.uri == uri }
            if (published != null) {
                lastForUri = published
                if (predicate(published)) return published
            }
            Thread.sleep(20)
        }
        error("Timed out waiting for diagnostics for $uri, last=$lastForUri")
    }

    override fun close() {
        runCatching { awaitFuture(serverProxy.shutdown()) }
        runCatching { serverProxy.exit() }
        // `exit` 是 notification，没有响应帧；这里补一个本地兜底，
        // 让测试能够稳定观测到 exitHandler，同时配合服务端幂等保护避免重复退出。
        runCatching { server.exit() }
        runCatching { clientInput.close() }
        runCatching { serverInput.close() }
        runCatching { clientOutput.close() }
        runCatching { serverOutput.close() }
        waitForTermination(serverListening)
        waitForTermination(clientListening)
        runCatching { server.close() }
        runCatching { clientListening.cancel(true) }
        runCatching { serverListening.cancel(true) }
    }

    private fun waitForTermination(listening: Future<*>) {
        runCatching { listening.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    private fun <T> awaitFuture(future: CompletableFuture<T>): T {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private class RecordingLanguageClient : LanguageClient {
        val publishedDiagnostics: MutableList<PublishDiagnosticsParams> = CopyOnWriteArrayList()

        override fun telemetryEvent(`object`: Any) {}

        override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
            publishedDiagnostics += diagnostics
        }

        override fun showMessage(messageParams: MessageParams) {}

        override fun showMessageRequest(requestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
            return CompletableFuture.completedFuture(MessageActionItem("ok"))
        }

        override fun logMessage(message: MessageParams) {}
    }

    companion object {
        private const val TIMEOUT_SECONDS = 30L
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB 缓冲区，避免长诊断或大响应把管道写满

        fun create(options: CangjieLspServerOptions = CangjieLspServerOptions()): LspIntegrationTestConnection {
            return LspIntegrationTestConnection(options)
        }
    }
}
