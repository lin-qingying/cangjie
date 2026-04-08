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

class LspStartupIntegrationTest : AbstractLspIntegrationTest() {

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

    @Test
    fun `server should handle multiple documents without crashing`() {
        val uri1 = "file:///workspace/a.cj"
        val uri2 = "file:///workspace/b.cj"

        session.openDocument(uri1, "let a = 1")
        session.openDocument(uri2, "let b = 2")
        session.changeDocument(uri1, "let a = 10", 2)

        assertNotNull(session.initializeResult())
    }

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
