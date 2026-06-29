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
    /**
     * 客户端侧读取服务端输出的管道输入端。
     */
    private val clientInput = PipedInputStream(BUFFER_SIZE)

    /**
     * 服务端写入客户端输入的管道输出端。
     */
    private val serverOutput = PipedOutputStream(clientInput)

    /**
     * 服务端侧读取客户端输出的管道输入端。
     */
    private val serverInput = PipedInputStream(BUFFER_SIZE)

    /**
     * 客户端写入服务端输入的管道输出端。
     */
    private val clientOutput = PipedOutputStream(serverInput)

    /**
     * 记录 server-to-client 通知的测试客户端。
     */
    private val client = RecordingLanguageClient()

    /**
     * 被测语言服务器实例。
     */
    private val server: CangjieLanguageServer = CangjieLanguageServer(options)

    /**
     * 服务端侧 LSP4J launcher。
     */
    private val serverLauncher = LSPLauncher.createServerLauncher(server, serverInput, serverOutput)

    /**
     * 客户端侧 LSP4J launcher。
     */
    private val clientLauncher = LSPLauncher.createClientLauncher(client, clientInput, clientOutput)

    /**
     * 服务端监听任务。
     */
    private val serverListening: Future<*> = serverLauncher.startListening()

    /**
     * 客户端监听任务。
     */
    private val clientListening: Future<*> = clientLauncher.startListening()

    /**
     * 客户端调用服务端的远端代理。
     */
    private val serverProxy: LanguageServer = clientLauncher.remoteProxy

    /**
     * initialize 请求的缓存结果。
     */
    private var initializeResult: InitializeResult? = null

    init {
        server.connect(serverLauncher.remoteProxy)
    }

    /**
     * 使用 rootUri 执行 initialize 请求。
     */
    fun initialize(rootUri: String = "file:///workspace"): InitializeResult {
        return initialize(InitializeParams().apply { this.rootUri = rootUri })
    }

    /**
     * 使用完整参数执行 initialize 请求。
     */
    fun initialize(params: InitializeParams): InitializeResult {
        val result = awaitFuture(serverProxy.initialize(params))
        initializeResult = result
        return result
    }

    /**
     * 发送 initialized 通知。
     */
    fun initialized() {
        serverProxy.initialized(InitializedParams())
    }

    /**
     * 返回缓存的 initialize 结果。
     */
    fun initializeResult(): InitializeResult =
        initializeResult ?: error("LSP connection has not been initialized")

    /**
     * 发送 textDocument/didOpen 通知。
     */
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

    /**
     * 发送整篇文档替换式 didChange 通知。
     */
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

    /**
     * 发送自定义 didChange 通知。
     */
    fun changeDocument(params: DidChangeTextDocumentParams) {
        serverProxy.textDocumentService.didChange(params)
    }

    /**
     * 发送 textDocument/didSave 通知。
     */
    fun saveDocument(uri: String, text: String? = null) {
        serverProxy.textDocumentService.didSave(
            DidSaveTextDocumentParams(TextDocumentIdentifier(uri), text),
        )
    }

    /**
     * 发送 textDocument/didClose 通知。
     */
    fun closeDocument(uri: String) {
        serverProxy.textDocumentService.didClose(
            DidCloseTextDocumentParams(TextDocumentIdentifier(uri)),
        )
    }

    /** 发起 completion 请求并等待结果。 */
    fun completion(params: CompletionParams): Either<List<CompletionItem>, CompletionList> {
        return awaitFuture(serverProxy.textDocumentService.completion(params))
    }

    /** 发起 hover 请求并等待结果。 */
    fun hover(params: HoverParams): Hover = awaitFuture(serverProxy.textDocumentService.hover(params))

    /** 发起 signatureHelp 请求并等待结果。 */
    fun signatureHelp(params: SignatureHelpParams): SignatureHelp {
        return awaitFuture(serverProxy.textDocumentService.signatureHelp(params))
    }

    /** 发起 declaration 请求并等待结果。 */
    fun declaration(params: DeclarationParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.declaration(params))
    }

    /** 发起 definition 请求并等待结果。 */
    fun definition(params: DefinitionParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.definition(params))
    }

    /** 发起 typeDefinition 请求并等待结果。 */
    fun typeDefinition(params: TypeDefinitionParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.typeDefinition(params))
    }

    /** 发起 implementation 请求并等待结果。 */
    fun implementation(params: ImplementationParams): Either<List<Location>, List<LocationLink>> {
        return awaitFuture(serverProxy.textDocumentService.implementation(params))
    }

    /** 发起 references 请求并等待结果。 */
    fun references(params: ReferenceParams): List<Location> {
        return awaitFuture(serverProxy.textDocumentService.references(params))
    }

    /** 发起 documentHighlight 请求并等待结果。 */
    fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight> {
        return awaitFuture(serverProxy.textDocumentService.documentHighlight(params))
    }

    /** 发起 documentSymbol 请求并等待结果。 */
    fun documentSymbol(params: DocumentSymbolParams): List<Either<SymbolInformation, DocumentSymbol>> {
        return awaitFuture(serverProxy.textDocumentService.documentSymbol(params))
    }

    /** 发起 codeAction 请求并等待结果。 */
    fun codeAction(params: CodeActionParams): List<Either<Command, CodeAction>> {
        return awaitFuture(serverProxy.textDocumentService.codeAction(params))
    }

    /** 发起 formatting 请求并等待结果。 */
    fun formatting(params: DocumentFormattingParams): List<TextEdit> {
        return awaitFuture(serverProxy.textDocumentService.formatting(params))
    }

    /** 发起 rangeFormatting 请求并等待结果。 */
    fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit> {
        return awaitFuture(serverProxy.textDocumentService.rangeFormatting(params))
    }

    /** 发起 rename 请求并等待结果。 */
    fun rename(params: RenameParams): WorkspaceEdit {
        return awaitFuture(serverProxy.textDocumentService.rename(params))
    }

    /** 发起 prepareRename 请求并等待结果。 */
    fun prepareRename(
        params: PrepareRenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> {
        return awaitFuture(serverProxy.textDocumentService.prepareRename(params))
    }

    /** 发起 foldingRange 请求并等待结果。 */
    fun foldingRange(params: FoldingRangeRequestParams): List<FoldingRange> {
        return awaitFuture(serverProxy.textDocumentService.foldingRange(params))
    }

    /** 发起 selectionRange 请求并等待结果。 */
    fun selectionRange(params: SelectionRangeParams): List<SelectionRange> {
        return awaitFuture(serverProxy.textDocumentService.selectionRange(params))
    }

    /** 发起 semanticTokens/full 请求并等待结果。 */
    fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens {
        return awaitFuture(serverProxy.textDocumentService.semanticTokensFull(params))
    }

    /** 发起 semanticTokens/range 请求并等待结果。 */
    fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens {
        return awaitFuture(serverProxy.textDocumentService.semanticTokensRange(params))
    }

    /** 发起 inlayHint 请求并等待结果。 */
    fun inlayHint(params: InlayHintParams): List<InlayHint> {
        return awaitFuture(serverProxy.textDocumentService.inlayHint(params))
    }

    /** 发起 textDocument/diagnostic 请求并等待结果。 */
    fun documentDiagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport {
        return awaitFuture(serverProxy.textDocumentService.diagnostic(params))
    }

    /** 发送 workspace/didChangeConfiguration 通知。 */
    fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        serverProxy.workspaceService.didChangeConfiguration(params)
    }

    /** 发送 workspace/didChangeWatchedFiles 通知。 */
    fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        serverProxy.workspaceService.didChangeWatchedFiles(params)
    }

    /** 发送 workspace/didChangeWorkspaceFolders 通知。 */
    fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        serverProxy.workspaceService.didChangeWorkspaceFolders(params)
    }

    /** 发起 workspace/executeCommand 请求并等待结果。 */
    fun executeCommand(params: ExecuteCommandParams): Any {
        return awaitFuture(serverProxy.workspaceService.executeCommand(params))
    }

    /** 发起 workspace/symbol 请求并等待结果。 */
    fun workspaceSymbol(params: WorkspaceSymbolParams): Either<List<SymbolInformation>, List<WorkspaceSymbol>> {
        return awaitFuture(serverProxy.workspaceService.symbol(params))
    }

    /** 发起 workspace/diagnostic 请求并等待结果。 */
    fun workspaceDiagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport {
        return awaitFuture(serverProxy.workspaceService.diagnostic(params))
    }

    /** 发送 notebook/didOpen 通知。 */
    fun notebookDidOpen(params: DidOpenNotebookDocumentParams = DidOpenNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didOpen(params)
    }

    /** 发送 notebook/didChange 通知。 */
    fun notebookDidChange(params: DidChangeNotebookDocumentParams = DidChangeNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didChange(params)
    }

    /** 发送 notebook/didSave 通知。 */
    fun notebookDidSave(params: DidSaveNotebookDocumentParams = DidSaveNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didSave(params)
    }

    /** 发送 notebook/didClose 通知。 */
    fun notebookDidClose(params: DidCloseNotebookDocumentParams = DidCloseNotebookDocumentParams()) {
        serverProxy.notebookDocumentService.didClose(params)
    }

    /** 发送 workDoneProgress/cancel 通知。 */
    fun cancelProgress(token: String = "contract-token") {
        serverProxy.cancelProgress(WorkDoneProgressCancelParams(Either.forLeft(token)))
    }

    /** 构造 completion 参数。 */
    fun completionParams(uri: String, line: Int, character: Int): CompletionParams =
        CompletionParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 hover 参数。 */
    fun hoverParams(uri: String, line: Int, character: Int): HoverParams =
        HoverParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 signatureHelp 参数。 */
    fun signatureHelpParams(uri: String, line: Int, character: Int): SignatureHelpParams =
        SignatureHelpParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 declaration 参数。 */
    fun declarationParams(uri: String, line: Int, character: Int): DeclarationParams =
        DeclarationParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 definition 参数。 */
    fun definitionParams(uri: String, line: Int, character: Int): DefinitionParams =
        DefinitionParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 typeDefinition 参数。 */
    fun typeDefinitionParams(uri: String, line: Int, character: Int): TypeDefinitionParams =
        TypeDefinitionParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 implementation 参数。 */
    fun implementationParams(uri: String, line: Int, character: Int): ImplementationParams =
        ImplementationParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 references 参数。 */
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

    /** 构造 documentHighlight 参数。 */
    fun documentHighlightParams(uri: String, line: Int, character: Int): DocumentHighlightParams =
        DocumentHighlightParams(TextDocumentIdentifier(uri), Position(line, character))

    /** 构造 documentSymbol 参数。 */
    fun documentSymbolParams(uri: String): DocumentSymbolParams =
        DocumentSymbolParams(TextDocumentIdentifier(uri))

    /** 构造 documentDiagnostic 参数。 */
    fun documentDiagnosticParams(uri: String): DocumentDiagnosticParams =
        DocumentDiagnosticParams(TextDocumentIdentifier(uri))

    /** 清空记录的 diagnostics 通知。 */
    fun clearPublishedDiagnostics() {
        client.publishedDiagnostics.clear()
    }

    /** 返回记录的 diagnostics 通知快照。 */
    fun publishedDiagnostics(): List<PublishDiagnosticsParams> = client.publishedDiagnostics.toList()

    /** 等待 diagnostics 通知达到指定数量。 */
    fun awaitDiagnosticsCount(expectedCount: Int) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (System.nanoTime() < deadline) {
            if (client.publishedDiagnostics.size >= expectedCount) return
            Thread.sleep(20)
        }
        error("Timed out waiting for $expectedCount diagnostics notifications, actual=${client.publishedDiagnostics.size}")
    }

    /** 等待指定 URI 的 diagnostics 通知满足条件。 */
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

    /**
     * 关闭客户端、服务端、管道和监听任务。
     */
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

    /**
     * 等待 LSP4J 监听任务结束。
     */
    private fun waitForTermination(listening: Future<*>) {
        runCatching { listening.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    /**
     * 等待异步 LSP 请求完成并返回结果。
     */
    private fun <T> awaitFuture(future: CompletableFuture<T>): T {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /**
     * 测试用语言客户端，记录服务端发出的通知。
     */
    private class RecordingLanguageClient : LanguageClient {
        /**
         * 已发布的 diagnostics 通知列表。
         */
        val publishedDiagnostics: MutableList<PublishDiagnosticsParams> = CopyOnWriteArrayList()

        /** 忽略 telemetry 通知。 */
        override fun telemetryEvent(`object`: Any) {}

        /** 记录 publishDiagnostics 通知。 */
        override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
            publishedDiagnostics += diagnostics
        }

        /** 忽略 showMessage 通知。 */
        override fun showMessage(messageParams: MessageParams) {}

        /** 自动响应 showMessageRequest。 */
        override fun showMessageRequest(requestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
            return CompletableFuture.completedFuture(MessageActionItem("ok"))
        }

        /** 忽略 logMessage 通知。 */
        override fun logMessage(message: MessageParams) {}
    }

    companion object {
        /**
         * 测试请求等待超时时间。
         */
        private const val TIMEOUT_SECONDS = 30L

        /**
         * 双端管道缓冲区大小。
         */
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB 缓冲区，避免长诊断或大响应把管道写满

        /**
         * 创建一条新的 LSP 集成测试连接。
         */
        fun create(options: CangjieLspServerOptions = CangjieLspServerOptions()): LspIntegrationTestConnection {
            return LspIntegrationTestConnection(options)
        }
    }
}
