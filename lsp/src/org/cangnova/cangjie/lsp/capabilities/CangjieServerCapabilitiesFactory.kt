package org.cangnova.cangjie.lsp.capabilities

import org.eclipse.lsp4j.CodeActionOptions
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.DefinitionOptions
import org.eclipse.lsp4j.DiagnosticRegistrationOptions
import org.eclipse.lsp4j.DocumentFormattingOptions
import org.eclipse.lsp4j.DocumentHighlightOptions
import org.eclipse.lsp4j.DocumentRangeFormattingOptions
import org.eclipse.lsp4j.DocumentSymbolOptions
import org.eclipse.lsp4j.ExecuteCommandOptions
import org.eclipse.lsp4j.FoldingRangeProviderOptions
import org.eclipse.lsp4j.HoverOptions
import org.eclipse.lsp4j.ImplementationRegistrationOptions
import org.eclipse.lsp4j.InlayHintRegistrationOptions
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ReferenceOptions
import org.eclipse.lsp4j.RenameOptions
import org.eclipse.lsp4j.SaveOptions
import org.eclipse.lsp4j.SelectionRangeRegistrationOptions
import org.eclipse.lsp4j.SemanticTokensLegend
import org.eclipse.lsp4j.SemanticTokensServerFull
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.ServerInfo
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.TextDocumentSyncOptions
import org.eclipse.lsp4j.TypeDefinitionRegistrationOptions
import org.eclipse.lsp4j.WorkspaceFoldersOptions
import org.eclipse.lsp4j.WorkspaceServerCapabilities
import org.eclipse.lsp4j.WorkspaceSymbolOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either

object CangjieServerCapabilitiesFactory {
    fun createInitializeResult(
        descriptor: CangjieLanguageServerDescriptor,
        features: CangjieLspFeatureSet,
    ): InitializeResult {
        return InitializeResult(
            createCapabilities(descriptor, features),
            ServerInfo(descriptor.name, descriptor.version),
        )
    }

    fun createCapabilities(
        descriptor: CangjieLanguageServerDescriptor,
        features: CangjieLspFeatureSet,
    ): ServerCapabilities {
        return ServerCapabilities().apply {
            positionEncoding = descriptor.positionEncoding
            setTextDocumentSync(TextDocumentSyncOptions().apply {
                openClose = descriptor.openClose
                change = descriptor.changeSyncKind
                setSave(SaveOptions(descriptor.saveIncludeText))
            })

            if (features.hover) setHoverProvider(HoverOptions())
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
            if (features.definition) setDefinitionProvider(DefinitionOptions())
            if (features.typeDefinition) setTypeDefinitionProvider(TypeDefinitionRegistrationOptions())
            if (features.implementation) setImplementationProvider(ImplementationRegistrationOptions())
            if (features.references) setReferencesProvider(ReferenceOptions())
            if (features.documentHighlight) setDocumentHighlightProvider(DocumentHighlightOptions())
            if (features.documentSymbol) setDocumentSymbolProvider(DocumentSymbolOptions())
            if (features.workspaceSymbol) setWorkspaceSymbolProvider(WorkspaceSymbolOptions(false))
            if (features.codeAction) {
                setCodeActionProvider(CodeActionOptions(descriptor.codeActionKinds).apply {
                    resolveProvider = false
                })
            }
            if (features.formatting) {
                setDocumentFormattingProvider(DocumentFormattingOptions())
                setDocumentRangeFormattingProvider(DocumentRangeFormattingOptions())
            }
            if (features.rename) setRenameProvider(RenameOptions(descriptor.renamePrepareProvider))
            if (features.foldingRange) setFoldingRangeProvider(FoldingRangeProviderOptions())
            if (features.selectionRange) setSelectionRangeProvider(SelectionRangeRegistrationOptions())
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
            if (features.inlayHints) setInlayHintProvider(InlayHintRegistrationOptions())
            if (features.diagnostics) {
                diagnosticProvider = DiagnosticRegistrationOptions().apply {
                    identifier = descriptor.diagnosticIdentifier
                    setInterFileDependencies(true)
                    setWorkspaceDiagnostics(true)
                }
            }
            if (descriptor.executeCommands.isNotEmpty()) {
                executeCommandProvider = ExecuteCommandOptions(descriptor.executeCommands)
            }
            workspace = WorkspaceServerCapabilities().apply {
                workspaceFolders = WorkspaceFoldersOptions().apply {
                    supported = descriptor.workspaceFoldersSupported
                    setChangeNotifications(Either.forLeft(descriptor.workspaceFolderChangeNotificationsId))
                }
            }
        }
    }
}
