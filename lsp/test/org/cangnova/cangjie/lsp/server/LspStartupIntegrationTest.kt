package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.DefinitionCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.DocumentSymbolCapabilities
import org.eclipse.lsp4j.HoverCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import org.eclipse.lsp4j.ReferencesCapabilities
import org.eclipse.lsp4j.SignatureHelpCapabilities
import org.eclipse.lsp4j.SymbolCapabilities
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 LSP 服务端启动、初始化和能力协商的基础协议行为。
 *
 * 该测试通过真实内存连接覆盖 initialize、文档生命周期和高级能力降级。
 */
class LspStartupIntegrationTest : AbstractLspIntegrationTest() {

    /**
     * 校验服务端可以完成初始化并返回核心能力。
     *
     * 该用例固定 serverInfo、文本同步、定义和悬停能力作为默认启动成功的基本信号。
     */
    @Test
    fun `server should initialize and exchange capabilities successfully`() {
        val result = session.initializeResult()

        assertNotNull(result.serverInfo, "Server should provide information")
        assertTrue(result.serverInfo.name.contains("Cangjie"), "Server name should contain 'Cangjie'")

        val caps = result.capabilities
        assertNotNull(caps.textDocumentSync, "Text document sync should be supported")
        assertNotNull(caps.definitionProvider, "Definition provider should be supported by default")
        assertNotNull(caps.hoverProvider, "Hover provider should be supported by default")
    }

    /**
     * 校验服务端能处理多个文档的打开和变更事件。
     *
     * 该用例确认会话在连续文档通知后仍保持可用，不因文档存储更新而崩溃。
     */
    @Test
    fun `server should handle multiple documents without crashing`() {
        val uri1 = "file:///workspace/a.cj"
        val uri2 = "file:///workspace/b.cj"

        session.openDocument(uri1, "let a = 1")
        session.openDocument(uri2, "let b = 2")
        session.changeDocument(uri1, "let a = 10", 2)

        assertNotNull(session.initializeResult())
    }

    /**
     * 校验客户端声明高级能力时，服务端会隐藏尚未支持的 provider。
     *
     * 该用例同时确认核心能力保留，声明、类型定义、实现、选择范围和 pull diagnostics 等高级能力不被误报。
     */
    @Test
    fun `server should negotiate away unsupported advanced capabilities`() {
        val connection = LspIntegrationTestConnection.create(defaultServerOptions())

        connection.use {
            val result = it.initialize(
                InitializeParams().apply {
                    rootUri = "file:///workspace"
                    capabilities = ClientCapabilities().apply {
                        textDocument = TextDocumentClientCapabilities().apply {
                            completion = CompletionCapabilities()
                            hover = HoverCapabilities()
                            signatureHelp = SignatureHelpCapabilities()
                            definition = DefinitionCapabilities()
                            references = ReferencesCapabilities()
                            documentHighlight = DocumentHighlightCapabilities()
                            documentSymbol = DocumentSymbolCapabilities()
                            publishDiagnostics = PublishDiagnosticsCapabilities()
                        }
                        workspace = WorkspaceClientCapabilities().apply {
                            symbol = SymbolCapabilities()
                        }
                    }
                },
            )

            val caps = result.capabilities
            assertTrue(caps.hoverProvider.isLeft)
            assertTrue(caps.definitionProvider.isLeft)
            assertTrue(caps.referencesProvider.isLeft)
            assertTrue(caps.documentHighlightProvider.isLeft)
            assertTrue(caps.documentSymbolProvider.isLeft)
            assertTrue(caps.workspaceSymbolProvider.isLeft)

            assertNull(caps.declarationProvider)
            assertNull(caps.typeDefinitionProvider)
            assertNull(caps.implementationProvider)
            assertNull(caps.selectionRangeProvider)
            assertNull(caps.diagnosticProvider)
        }
    }
}
