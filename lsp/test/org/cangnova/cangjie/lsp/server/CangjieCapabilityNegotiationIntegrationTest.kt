package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacadeFactory
import org.eclipse.lsp4j.PositionEncodingKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CangjieCapabilityNegotiationIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @Test
    fun `minimal client keeps core providers and hides advanced providers`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.minimal(),
            options = protocolOptions(factory),
        ).use { testSession ->
            val capabilities = testSession.initializeResult().capabilities

            assertNotNull(capabilities.completionProvider)
            assertTrue(capabilities.hoverProvider.isLeft)
            assertNotNull(capabilities.signatureHelpProvider)
            assertTrue(capabilities.definitionProvider.isLeft)
            assertTrue(capabilities.referencesProvider.isLeft)
            assertTrue(capabilities.documentHighlightProvider.isLeft)
            assertTrue(capabilities.documentSymbolProvider.isLeft)
            assertTrue(capabilities.workspaceSymbolProvider.isLeft)
            assertNotNull(capabilities.codeActionProvider)
            assertNotNull(capabilities.documentFormattingProvider)
            assertNotNull(capabilities.renameProvider)

            assertNull(capabilities.declarationProvider)
            assertNull(capabilities.typeDefinitionProvider)
            assertNull(capabilities.implementationProvider)
            assertNull(capabilities.foldingRangeProvider)
            assertNull(capabilities.selectionRangeProvider)
            assertNull(capabilities.semanticTokensProvider)
            assertNull(capabilities.inlayHintProvider)
            assertNull(capabilities.workspace)
        }
    }

    @Test
    fun `full featured client exposes advanced providers pull diagnostics and workspace metadata`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
            options = protocolOptions(
                factory = factory,
                descriptor = CangjieLanguageServerDescriptor(
                    executeCommands = listOf("cangjie.reindex"),
                    pullDiagnosticsEnabled = true,
                ),
            ),
        ).use { testSession ->
            val capabilities = testSession.initializeResult().capabilities

            assertTrue(capabilities.declarationProvider.isLeft)
            assertTrue(capabilities.typeDefinitionProvider.isLeft)
            assertTrue(capabilities.implementationProvider.isLeft)
            assertTrue(capabilities.foldingRangeProvider.isLeft)
            assertTrue(capabilities.selectionRangeProvider.isLeft)
            assertNotNull(capabilities.semanticTokensProvider)
            assertTrue(capabilities.inlayHintProvider.isLeft)
            assertNotNull(capabilities.diagnosticProvider)
            assertEquals(listOf("cangjie.reindex"), capabilities.executeCommandProvider.commands)
            assertTrue(capabilities.workspace.workspaceFolders.supported)
            assertEquals(PositionEncodingKind.UTF16, capabilities.positionEncoding)
        }
    }

    @Test
    fun `server leaves position encoding unset when client omits utf16 and preferred encoding`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.withPositionEncodings(listOf(PositionEncodingKind.UTF8)),
            options = protocolOptions(factory),
        ).use { testSession ->
            assertNull(testSession.initializeResult().capabilities.positionEncoding)
        }
    }

    private fun protocolOptions(
        factory: ProtocolContractAnalysisFacadeFactory,
        descriptor: CangjieLanguageServerDescriptor = CangjieLanguageServerDescriptor(),
    ): CangjieLspServerOptions {
        return defaultServerOptions().copy(
            descriptor = descriptor,
            analysisFacadeFactory = factory::create,
        )
    }
}
