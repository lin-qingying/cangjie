package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacadeFactory
import org.cangnova.cangjie.lsp.testkit.ProtocolContractConfiguration
import org.eclipse.lsp4j.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 LSP 服务层对 analysis facade 的协议分发契约。
 *
 * 该测试使用可录制的协议 facade，确认各类请求在功能启用、关闭和工作区通知下的转发行为。
 */
class CangjieProtocolContractIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试为每个场景手动注入协议契约 facade。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验文本文件请求在功能启用时会路由到协议契约 facade。
     */
    @Test
    fun `text document requests route through contract facade when features are enabled`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
            options = protocolOptions(factory),
        ).use { testSession ->
            val uri = "file:///workspace/contract.cj"
            testSession.openDocument(uri, "contract body")
            testSession.saveDocument(uri)

            val completion = testSession.completion(testSession.completionParams(uri, 0, 0))
            val hover = testSession.hover(testSession.hoverParams(uri, 0, 0))
            val signatureHelp = testSession.signatureHelp(testSession.signatureHelpParams(uri, 0, 0))
            val declaration = testSession.declaration(testSession.declarationParams(uri, 0, 0))
            val definition = testSession.definition(testSession.definitionParams(uri, 0, 0))
            val typeDefinition = testSession.typeDefinition(testSession.typeDefinitionParams(uri, 0, 0))
            val implementation = testSession.implementation(testSession.implementationParams(uri, 0, 0))
            val references = testSession.references(testSession.referenceParams(uri, 0, 0))
            val highlights = testSession.documentHighlight(testSession.documentHighlightParams(uri, 0, 0))
            val documentSymbols = testSession.documentSymbol(testSession.documentSymbolParams(uri))
            val codeActions = testSession.codeAction(
                CodeActionParams(
                    TextDocumentIdentifier(uri),
                    Range(Position(0, 0), Position(0, 4)),
                    CodeActionContext(emptyList()),
                ),
            )
            val formatting = testSession.formatting(
                DocumentFormattingParams(TextDocumentIdentifier(uri), FormattingOptions(4, true)),
            )
            val rangeFormatting = testSession.rangeFormatting(
                DocumentRangeFormattingParams(
                    TextDocumentIdentifier(uri),
                    FormattingOptions(4, true),
                    Range(Position(0, 0), Position(0, 4)),
                ),
            )
            val rename = testSession.rename(RenameParams(TextDocumentIdentifier(uri), Position(0, 0), "renamedValue"))
            val prepareRename = testSession.prepareRename(PrepareRenameParams(TextDocumentIdentifier(uri), Position(0, 0)))
            val foldingRanges = testSession.foldingRange(FoldingRangeRequestParams(TextDocumentIdentifier(uri)))
            val selectionRanges = testSession.selectionRange(
                SelectionRangeParams(TextDocumentIdentifier(uri), listOf(Position(0, 0))),
            )
            val semanticTokensFull = testSession.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(uri)))
            val semanticTokensRange = testSession.semanticTokensRange(
                SemanticTokensRangeParams(TextDocumentIdentifier(uri), Range(Position(0, 0), Position(0, 4))),
            )
            val inlayHints = testSession.inlayHint(
                InlayHintParams(TextDocumentIdentifier(uri), Range(Position(0, 0), Position(0, 4))),
            )
            val documentDiagnostic = testSession.documentDiagnostic(testSession.documentDiagnosticParams(uri))

            testSession.closeDocument(uri)
            val facade = factory.requireFacade()

            assertEquals("contractCompletion", completion.left.single().label)
            assertTrue(hover.toString().contains("contractHover"))
            assertEquals("contractSignature(first: Int64, second: Int64)", signatureHelp.signatures.single().label)
            assertEquals(uri, declaration.left.single().uri)
            assertEquals(uri, definition.left.single().uri)
            assertEquals(uri, typeDefinition.left.single().uri)
            assertEquals(uri, implementation.left.single().uri)
            assertEquals(uri, references.single().uri)
            assertEquals(DocumentHighlightKind.Text, highlights.single().kind)
            assertEquals("contractDocumentSymbol", documentSymbols.single().right.name)
            assertEquals("contractCodeAction", codeActions.single().right.title)
            assertEquals("contractFormat", formatting.single().newText)
            assertTrue(rangeFormatting.isEmpty(), "rangeFormatting 当前仍是服务层中性返回，不应抛异常")
            assertEquals("renamedValue", rename.changes[uri]!!.single().newText)
            assertTrue(prepareRename.toString().contains("Position"))
            assertEquals(FoldingRangeKind.Region, foldingRanges.single().kind)
            assertNotNull(selectionRanges.single().parent)
            assertEquals(listOf(0, 0, 4, 1, 0), semanticTokensFull.data)
            assertEquals(listOf(0, 0, 4, 1, 0), semanticTokensRange.data)
            assertTrue(inlayHints.single().toString().contains("contractHint"))
            assertTrue(documentDiagnostic.toString().contains("contractDiagnostic"))

            assertTrue(facade.wasInvoked("didOpen"))
            assertTrue(facade.wasInvoked("didSave"))
            assertTrue(facade.wasInvoked("completion"))
            assertTrue(facade.wasInvoked("hover"))
            assertTrue(facade.wasInvoked("signatureHelp"))
            assertTrue(facade.wasInvoked("declaration"))
            assertTrue(facade.wasInvoked("definition"))
            assertTrue(facade.wasInvoked("typeDefinition"))
            assertTrue(facade.wasInvoked("implementation"))
            assertTrue(facade.wasInvoked("references"))
            assertTrue(facade.wasInvoked("documentHighlight"))
            assertTrue(facade.wasInvoked("documentSymbol"))
            assertTrue(facade.wasInvoked("codeAction"))
            assertTrue(facade.wasInvoked("formatting"))
            assertTrue(facade.wasInvoked("rename"))
            assertTrue(facade.wasInvoked("prepareRename"))
            assertTrue(facade.wasInvoked("foldingRange"))
            assertTrue(facade.wasInvoked("selectionRange"))
            assertTrue(facade.wasInvoked("semanticTokensFull"))
            assertTrue(facade.wasInvoked("semanticTokensRange"))
            assertTrue(facade.wasInvoked("inlayHint"))
            assertTrue(facade.wasInvoked("collectDiagnostics"))
            assertFalse(facade.wasInvoked("rangeFormatting"))
        }
    }

    /**
     * 校验功能关闭或文档缺失时服务层返回中性值且不调用 facade。
     */
    @Test
    fun `disabled features and missing documents return neutral values without invoking facade`() {
        val factory = ProtocolContractAnalysisFacadeFactory(
            ProtocolContractConfiguration(CangjieLspFeatureSet.none()),
        )
        createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
            options = protocolOptions(factory),
        ).use { testSession ->
            val missingUri = "file:///workspace/missing.cj"

            assertTrue(testSession.completion(testSession.completionParams(missingUri, 0, 0)).left.isEmpty())
            assertTrue(testSession.hover(testSession.hoverParams(missingUri, 0, 0)).toString().isNotBlank())
            assertTrue(testSession.references(testSession.referenceParams(missingUri, 0, 0)).isEmpty())
            assertTrue(testSession.documentSymbol(testSession.documentSymbolParams(missingUri)).isEmpty())
            assertTrue(
                testSession.codeAction(
                    CodeActionParams(
                        TextDocumentIdentifier(missingUri),
                        Range(Position(0, 0), Position(0, 0)),
                        CodeActionContext(emptyList()),
                    ),
                ).isEmpty(),
            )
            assertTrue(
                testSession.formatting(
                    DocumentFormattingParams(TextDocumentIdentifier(missingUri), FormattingOptions(4, true)),
                ).isEmpty(),
            )
            assertTrue(testSession.rename(RenameParams(TextDocumentIdentifier(missingUri), Position(0, 0), "renamed")).changes.isNullOrEmpty())
            assertTrue(testSession.selectionRange(SelectionRangeParams(TextDocumentIdentifier(missingUri), listOf(Position(0, 0)))).isEmpty())
            assertTrue(testSession.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(missingUri))).data.isEmpty())
            assertTrue(testSession.inlayHint(InlayHintParams(TextDocumentIdentifier(missingUri), Range(Position(0, 0), Position(0, 0)))).isEmpty())
            val workspaceSymbol = testSession.workspaceSymbol(WorkspaceSymbolParams("contract"))
            assertTrue(workspaceSymbol.left?.isEmpty() != false)
            assertTrue(workspaceSymbol.right?.isEmpty() != false)
            assertFalse(testSession.workspaceDiagnostic(WorkspaceDiagnosticParams()).toString().contains("contractDiagnostic"))

            val facade = factory.requireFacade()
            assertFalse(facade.wasInvoked("completion"))
            assertFalse(facade.wasInvoked("collectDiagnostics"))
            assertFalse(facade.wasInvoked("workspaceSymbol"))
        }
    }

    /**
     * 校验工作区通知会路由到 facade，notebook 通知不会破坏会话。
     */
    @Test
    fun `workspace notifications route through facade and notebook traffic keeps session alive`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
            options = protocolOptions(factory),
        ).use { testSession ->
            val uri = "file:///workspace/contract.cj"
            testSession.openDocument(uri, "contract body")

            testSession.didChangeConfiguration(DidChangeConfigurationParams(mapOf("mode" to "contract")))
            testSession.didChangeWatchedFiles(DidChangeWatchedFilesParams(emptyList()))
            testSession.didChangeWorkspaceFolders(
                DidChangeWorkspaceFoldersParams(
                    WorkspaceFoldersChangeEvent(
                        listOf(WorkspaceFolder("file:///workspace/added", "added")),
                        listOf(WorkspaceFolder("file:///workspace", "workspace")),
                    ),
                ),
            )
            val commandResult = testSession.executeCommand(ExecuteCommandParams("cangjie.reindex", emptyList()))
            val workspaceSymbols = testSession.workspaceSymbol(WorkspaceSymbolParams("contract"))
            val workspaceDiagnostic = testSession.workspaceDiagnostic(WorkspaceDiagnosticParams())

            testSession.notebookDidOpen()
            testSession.notebookDidChange()
            testSession.notebookDidSave()
            testSession.notebookDidClose()
            testSession.cancelProgress("contract-progress")

            val hover = testSession.hover(testSession.hoverParams(uri, 0, 0))
            val facade = factory.requireFacade()

            assertNotNull(commandResult)
            assertEquals("contractWorkspaceSymbol", workspaceSymbols.right.single().name)
            assertTrue(workspaceDiagnostic.toString().contains("contract"))
            assertTrue(hover.toString().contains("contractHover"))
            assertEquals(2, facade.invocationCount("didRefreshProjectStructure"))
            assertEquals(listOf("file:///workspace/added"), facade.lastWorkspaceFolderChange!!.added)
            assertEquals(listOf("file:///workspace"), facade.lastWorkspaceFolderChange!!.removed)
            assertTrue(facade.wasInvoked("workspaceSymbol"))
            assertTrue(facade.wasInvoked("collectWorkspaceDiagnostics"))
        }
    }

    /**
     * 构造使用协议契约 facade 的服务端选项。
     */
    private fun protocolOptions(factory: ProtocolContractAnalysisFacadeFactory): CangjieLspServerOptions {
        return defaultServerOptions().copy(analysisFacadeFactory = factory::create)
    }
}
