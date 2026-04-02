package org.cangnova.cangjie.lsp.framework

import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.PublishDiagnosticsParams

/**
 * 测试用例侧看到的 LSP 会话门面。
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

    fun closeDocument(uri: String) {
        connection.closeDocument(uri)
    }

    fun awaitDiagnosticsCount(expectedCount: Int) {
        connection.awaitDiagnosticsCount(expectedCount)
    }

    fun publishedDiagnostics(): List<PublishDiagnosticsParams> = connection.publishedDiagnostics()

    override fun close() {
        connection.close()
    }
}
