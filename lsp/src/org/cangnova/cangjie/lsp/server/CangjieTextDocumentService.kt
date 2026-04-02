package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DeclarationParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentDiagnosticParams
import org.eclipse.lsp4j.DocumentDiagnosticReport
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.FoldingRange
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior
import org.eclipse.lsp4j.PrepareRenameParams
import org.eclipse.lsp4j.PrepareRenameResult
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RelatedFullDocumentDiagnosticReport
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokensRangeParams
import org.eclipse.lsp4j.SelectionRange
import org.eclipse.lsp4j.SelectionRangeParams
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

class CangjieTextDocumentService(
    private val serverContext: CangjieServerContext,
) : TextDocumentService {
    override fun didOpen(params: DidOpenTextDocumentParams) {
        val document = serverContext.documentStore.open(params.textDocument)
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didOpen(context, document)
        if (serverContext.enabledFeatures.diagnostics) {
            publishDiagnostics(document)
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val document = serverContext.documentStore.applyChanges(params)
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didChange(context, document)
        if (serverContext.enabledFeatures.diagnostics) {
            publishDiagnostics(document)
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val document = serverContext.documentStore.close(params.textDocument.uri) ?: return
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didClose(context, document)
        if (serverContext.enabledFeatures.diagnostics) {
            serverContext.client?.publishDiagnostics(PublishDiagnosticsParams(document.uri, emptyList()))
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        val document = serverContext.documentStore.get(params.textDocument.uri) ?: return
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didSave(context, document)
        if (serverContext.enabledFeatures.diagnostics) {
            publishDiagnostics(document)
        }
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.completion) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.completion(serverContext.requestContext(), document, params)
        }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Hover(emptyList()))
        if (!serverContext.enabledFeatures.hover) return completed(Hover(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.hover(serverContext.requestContext(), document, params) ?: Hover(emptyList())
        }
    }

    override fun signatureHelp(params: SignatureHelpParams): CompletableFuture<SignatureHelp> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(SignatureHelp(emptyList(), null, null))
        if (!serverContext.enabledFeatures.signatureHelp) return completed(SignatureHelp(emptyList(), null, null))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.signatureHelp(serverContext.requestContext(), document, params)
                ?: SignatureHelp(emptyList(), null, null)
        }
    }

    override fun declaration(params: DeclarationParams): CompletableFuture<Either<List<out Location>, List<out LocationLink>>> {
        return completed(Either.forLeft(emptyList()))
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<out Location>, List<out LocationLink>>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.definition) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.definition(serverContext.requestContext(), document, params)
        }
    }

    override fun typeDefinition(params: TypeDefinitionParams): CompletableFuture<Either<List<out Location>, List<out LocationLink>>> {
        return completed(Either.forLeft(emptyList()))
    }

    override fun implementation(params: org.eclipse.lsp4j.ImplementationParams): CompletableFuture<Either<List<out Location>, List<out LocationLink>>> {
        return completed(Either.forLeft(emptyList()))
    }

    override fun references(params: ReferenceParams): CompletableFuture<List<out Location>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.references) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.references(serverContext.requestContext(), document, params)
        }
    }

    override fun documentHighlight(params: DocumentHighlightParams): CompletableFuture<List<out DocumentHighlight>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.documentHighlight) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.documentHighlight(serverContext.requestContext(), document, params)
        }
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.documentSymbol) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.documentSymbols(serverContext.requestContext(), document, params)
        }
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.codeAction) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.codeActions(serverContext.requestContext(), document, params)
        }
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<out TextEdit>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.formatting) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.formatting(serverContext.requestContext(), document, params)
        }
    }

    override fun rangeFormatting(params: org.eclipse.lsp4j.DocumentRangeFormattingParams): CompletableFuture<List<out TextEdit>> {
        return completed(emptyList())
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(WorkspaceEdit())
        if (!serverContext.enabledFeatures.rename) return completed(WorkspaceEdit())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.rename(serverContext.requestContext(), document, params) ?: WorkspaceEdit()
        }
    }

    override fun prepareRename(
        params: PrepareRenameParams,
    ): CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyPrepareRenameResult())
        if (!serverContext.enabledFeatures.rename) return completed(emptyPrepareRenameResult())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.prepareRename(
                serverContext.requestContext(),
                document,
                RenameParams(params.textDocument, params.position, null),
            ) ?: emptyPrepareRenameResult()
        }
    }

    override fun foldingRange(params: FoldingRangeRequestParams): CompletableFuture<List<FoldingRange>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.foldingRange) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.foldingRanges(serverContext.requestContext(), document, params)
        }
    }

    override fun selectionRange(params: SelectionRangeParams): CompletableFuture<List<SelectionRange>> {
        return completed(emptyList())
    }

    override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(SemanticTokens(emptyList()))
        if (!serverContext.enabledFeatures.semanticTokens) return completed(SemanticTokens(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.semanticTokensFull(serverContext.requestContext(), document, params)
                ?: SemanticTokens(emptyList())
        }
    }

    override fun semanticTokensRange(params: SemanticTokensRangeParams): CompletableFuture<SemanticTokens> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(SemanticTokens(emptyList()))
        if (!serverContext.enabledFeatures.semanticTokens) return completed(SemanticTokens(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.semanticTokensRange(serverContext.requestContext(), document, params)
                ?: SemanticTokens(emptyList())
        }
    }

    override fun inlayHint(params: InlayHintParams): CompletableFuture<List<InlayHint>> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.inlayHints) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.inlayHints(serverContext.requestContext(), document, params)
        }
    }

    override fun diagnostic(params: DocumentDiagnosticParams): CompletableFuture<DocumentDiagnosticReport> {
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(emptyList())))
        if (!serverContext.enabledFeatures.diagnostics) {
            return completed(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(emptyList())))
        }
        return serverContext.requestExecutor.compute {
            val diagnostics = serverContext.analysisFacade.collectDiagnostics(serverContext.requestContext(), document)
            DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(diagnostics))
        }
    }

    private fun publishDiagnostics(document: LspTextDocument) {
        val client = serverContext.client ?: return
        serverContext.requestExecutor.compute {
            val diagnostics = serverContext.analysisFacade.collectDiagnostics(serverContext.requestContext(), document)
            client.publishDiagnostics(PublishDiagnosticsParams(document.uri, diagnostics, document.version))
        }
    }

    private fun emptyPrepareRenameResult(): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> {
        return Either3.forFirst(Range(Position(0, 0), Position(0, 0)))
    }

    private fun <T> completed(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)
}
