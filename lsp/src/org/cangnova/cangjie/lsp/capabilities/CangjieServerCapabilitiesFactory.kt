package org.cangnova.cangjie.lsp.capabilities

import org.eclipse.lsp4j.CodeActionOptions
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.DocumentFormattingOptions
import org.eclipse.lsp4j.DiagnosticRegistrationOptions
import org.eclipse.lsp4j.ExecuteCommandOptions
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.RenameOptions
import org.eclipse.lsp4j.SaveOptions
import org.eclipse.lsp4j.SemanticTokensLegend
import org.eclipse.lsp4j.SemanticTokensServerFull
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.ServerInfo
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.TextDocumentSyncOptions
import org.eclipse.lsp4j.WorkspaceFoldersOptions
import org.eclipse.lsp4j.WorkspaceServerCapabilities

/**
 * 根据服务器描述和客户端能力协商结果构造 LSP 初始化能力。
 *
 * 该工厂集中维护服务端对文本同步、语义功能、诊断通道和工作区能力的声明。
 */
object CangjieServerCapabilitiesFactory {
    /**
     * 创建初始化响应对象。
     *
     * 响应包含服务端能力和服务端名称/版本信息，是 `initialize` 请求返回给客户端的协议根对象。
     */
    fun createInitializeResult(
        descriptor: CangjieLanguageServerDescriptor,
        negotiation: CangjieClientCapabilityNegotiation,
    ): InitializeResult {
        return InitializeResult().apply {
            capabilities = createCapabilities(descriptor, negotiation)
            serverInfo = ServerInfo(descriptor.name, descriptor.version ?: "1.0.0")
        }
    }

    /**
     * 根据协商后的功能集合创建 `ServerCapabilities`。
     *
     * 该方法只暴露客户端支持且服务器描述允许的能力，避免客户端收到无法处理或服务端未实现的 provider。
     */
    fun createCapabilities(
        descriptor: CangjieLanguageServerDescriptor,
        negotiation: CangjieClientCapabilityNegotiation,
    ): ServerCapabilities {
        return ServerCapabilities().apply {
            positionEncoding = negotiation.positionEncoding
            setTextDocumentSync(TextDocumentSyncOptions().apply {
                openClose = descriptor.openClose
                change = descriptor.changeSyncKind
                setSave(SaveOptions(descriptor.saveIncludeText))
            })

            val features = negotiation.features

            if (features.hover) setHoverProvider(true)
            if (features.completion) {
                completionProvider = CompletionOptions(
                    descriptor.completionResolveProvider,
                    descriptor.completionTriggerCharacters,
                )
            }
            if (features.signatureHelp) {
                signatureHelpProvider = SignatureHelpOptions(descriptor.signatureHelpTriggerCharacters)
            }
            if (features.declaration) setDeclarationProvider(true)
            if (features.definition) setDefinitionProvider(true)
            if (features.typeDefinition) setTypeDefinitionProvider(true)
            if (features.implementation) setImplementationProvider(true)
            if (features.references) setReferencesProvider(true)
            if (features.documentHighlight) setDocumentHighlightProvider(true)
            if (features.documentSymbol) setDocumentSymbolProvider(true)
            if (features.workspaceSymbol) setWorkspaceSymbolProvider(true)
            if (features.codeAction) {
                setCodeActionProvider(CodeActionOptions(descriptor.codeActionKinds).apply {
                    resolveProvider = false
                })
            }
            if (features.formatting) {
                setDocumentFormattingProvider(DocumentFormattingOptions())
                setDocumentRangeFormattingProvider(true)
            }
            if (features.rename) setRenameProvider(RenameOptions(descriptor.renamePrepareProvider))
            if (features.foldingRange) setFoldingRangeProvider(true)
            if (features.selectionRange) setSelectionRangeProvider(true)
            if (features.semanticTokens) {
                semanticTokensProvider = SemanticTokensWithRegistrationOptions(
                    SemanticTokensLegend(
                        descriptor.semanticTokenTypes,
                        descriptor.semanticTokenModifiers,
                    ),
                    SemanticTokensServerFull(true),
                    true,
                )
            }
            if (features.inlayHints) setInlayHintProvider(true)
            if (negotiation.pullDiagnostics) {
                diagnosticProvider = DiagnosticRegistrationOptions().apply {
                    identifier = descriptor.diagnosticIdentifier
                    setInterFileDependencies(true)
                    setWorkspaceDiagnostics(true)
                }
            }
            if (descriptor.executeCommands.isNotEmpty()) {
                executeCommandProvider = ExecuteCommandOptions(descriptor.executeCommands)
            }
            if (negotiation.workspaceFolders) {
                workspace = WorkspaceServerCapabilities().apply {
                    workspaceFolders = WorkspaceFoldersOptions().apply {
                        supported = true
                        setChangeNotifications(true)
                    }
                }
            }
        }
    }
}
