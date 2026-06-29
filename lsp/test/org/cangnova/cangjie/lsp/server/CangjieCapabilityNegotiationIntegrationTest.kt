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

/**
 * 校验 LSP 初始化阶段的客户端能力协商结果。
 *
 * 该测试覆盖最小客户端、全功能客户端和位置编码协商失败三类初始化能力面。
 */
class CangjieCapabilityNegotiationIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试按能力组合手动创建会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验最小客户端保留核心 provider，并隐藏需要显式声明的高级 provider。
     */
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

    /**
     * 校验全功能客户端会暴露高级 provider、pull diagnostics 和工作区元数据。
     */
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

    /**
     * 校验客户端不支持服务端偏好位置编码时，初始化结果不显式声明编码。
     */
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

    /**
     * 构造使用协议契约 facade 的服务端选项。
     */
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
