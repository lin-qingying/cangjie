package org.cangnova.cangjie.lsp.testkit

import org.eclipse.lsp4j.*

/**
 * LSP 客户端 capability 构造器。
 *
 * 这里把协议矩阵测试常用的客户端形态收敛成少量稳定入口：
 * - `minimal`：几乎不声明能力，用来验证服务端的保守协商策略；
 * - `core`：只声明被当前实现视为“核心能力”的字段；
 * - `advanced`：在 core 基础上补上 3.6+/3.14+/3.15+/3.17+ 的扩展能力；
 * - `fullFeatured`：用于真实语义测试，显式打开当前测试会用到的所有协商位。
 */
object LspClientCapabilitiesBuilder {
    fun minimal(): ClientCapabilities = ClientCapabilities()

    fun core(): ClientCapabilities = createClientCapabilities().apply {
        textDocument = TextDocumentClientCapabilities().apply {
            completion = CompletionCapabilities()
            hover = HoverCapabilities()
            signatureHelp = SignatureHelpCapabilities()
            references = ReferencesCapabilities()
            documentHighlight = DocumentHighlightCapabilities()
            documentSymbol = DocumentSymbolCapabilities()
            definition = DefinitionCapabilities()
            formatting = FormattingCapabilities()
            rangeFormatting = RangeFormattingCapabilities()
            codeAction = CodeActionCapabilities()
            rename = RenameCapabilities()
            publishDiagnostics = PublishDiagnosticsCapabilities()
        }
        workspace = WorkspaceClientCapabilities().apply {
            didChangeConfiguration = DidChangeConfigurationCapabilities()
            didChangeWatchedFiles = DidChangeWatchedFilesCapabilities()
            symbol = SymbolCapabilities()
            executeCommand = ExecuteCommandCapabilities()
        }
    }

    fun advanced(): ClientCapabilities = core().also { capabilities ->
        capabilities.textDocument = capabilities.textDocument.apply {
            declaration = DeclarationCapabilities()
            typeDefinition = TypeDefinitionCapabilities()
            implementation = ImplementationCapabilities()
            foldingRange = FoldingRangeCapabilities()
            selectionRange = SelectionRangeCapabilities()
            semanticTokens = SemanticTokensCapabilities()
            inlayHint = InlayHintCapabilities()
            diagnostic = DiagnosticCapabilities().apply {
                relatedDocumentSupport = true
                relatedInformation = true
            }
        }
    }

    fun publishDiagnosticsVersioned(): ClientCapabilities = core().also { capabilities ->
        capabilities.textDocument = capabilities.textDocument.apply {
            publishDiagnostics = PublishDiagnosticsCapabilities().apply {
                versionSupport = true
            }
        }
    }

    fun workspaceFolders(): ClientCapabilities = core().also { capabilities ->
        capabilities.workspace = capabilities.workspace.apply {
            workspaceFolders = true
        }
    }

    fun pullDiagnostics(): ClientCapabilities = advanced().also { capabilities ->
        capabilities.workspace = capabilities.workspace.apply {
            diagnostics = DiagnosticWorkspaceCapabilities()
        }
    }

    fun withPositionEncodings(encodings: List<String>): ClientCapabilities = fullFeatured(encodings)

    fun fullFeatured(
        positionEncodings: List<String> = listOf(PositionEncodingKind.UTF16),
    ): ClientCapabilities = pullDiagnostics().also { capabilities ->
        capabilities.textDocument = capabilities.textDocument.apply {
            publishDiagnostics = PublishDiagnosticsCapabilities().apply {
                versionSupport = true
            }
        }
        capabilities.workspace = capabilities.workspace.apply {
            workspaceFolders = true
        }
        capabilities.general = GeneralClientCapabilities().apply {
            this.positionEncodings = positionEncodings
        }
    }

    fun initializeParams(
        rootUri: String = "file:///workspace",
        capabilities: ClientCapabilities = fullFeatured(),
    ): InitializeParams = InitializeParams().apply {
        this.rootUri = rootUri
        this.capabilities = capabilities
    }

    private fun createClientCapabilities(): ClientCapabilities = ClientCapabilities().apply {
        textDocument = TextDocumentClientCapabilities()
        workspace = WorkspaceClientCapabilities()
    }
}
