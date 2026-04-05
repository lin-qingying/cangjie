package org.cangnova.cangjie.lsp.testkit

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.server.CangjieLanguageServer
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
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
        val result = serverProxy.initialize(params).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
        serverProxy.textDocumentService.didChange(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier(uri, version),
                listOf(TextDocumentContentChangeEvent(newText)),
            ),
        )
    }

    fun closeDocument(uri: String) {
        serverProxy.textDocumentService.didClose(
            DidCloseTextDocumentParams(TextDocumentIdentifier(uri)),
        )
    }

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

    override fun close() {
        runCatching { serverProxy.shutdown().get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        runCatching { serverProxy.exit() }
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

    private class RecordingLanguageClient : LanguageClient {
        val publishedDiagnostics: MutableList<PublishDiagnosticsParams> = CopyOnWriteArrayList()

        override fun telemetryEvent(`object`: Any) {}

        override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
            publishedDiagnostics += diagnostics
        }

        override fun showMessage(messageParams: MessageParams) {}

        override fun showMessageRequest(requestParams: org.eclipse.lsp4j.ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
            return CompletableFuture.completedFuture(MessageActionItem("ok"))
        }

        override fun logMessage(message: MessageParams) {}
    }

    companion object {
        private const val TIMEOUT_SECONDS = 10L
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB 缓冲区防止死锁

        fun create(options: CangjieLspServerOptions = CangjieLspServerOptions()): LspIntegrationTestConnection {
            return LspIntegrationTestConnection(options)
        }
    }
}
