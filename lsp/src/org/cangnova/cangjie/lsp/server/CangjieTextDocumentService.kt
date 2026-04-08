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
import org.eclipse.lsp4j.ImplementationParams
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior
import org.eclipse.lsp4j.PrepareRenameParams
import org.eclipse.lsp4j.PrepareRenameResult
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
import java.util.logging.Logger

/**
 * LSP 文档服务。
 *
 * 这一层只负责文档事件编排和协议入口转发：
 * 1. 先更新文档存储；
 * 2. 再刷新 project-structure 快照；
 * 3. 然后把文档生命周期事件通知 Analysis facade；
 * 4. 最后按统一入口发布 diagnostics。
 *
 * 这样文档生命周期、Analysis snapshot、push diagnostics 三条链就始终围绕同一份平台状态工作。
 */
class CangjieTextDocumentService(
    private val serverContext: CangjieServerContext,
) : TextDocumentService {
    private val logger = Logger.getLogger(CangjieTextDocumentService::class.java.name)

    override fun didOpen(params: DidOpenTextDocumentParams) {
        logger.info("====> didOpen: ${params.textDocument.uri}")
        val document = serverContext.documentStore.open(params.textDocument)
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didOpen(context, document)
        serverContext.refreshProjectStructure()
        if (serverContext.enabledFeatures.diagnostics) {
            serverContext.republishOpenDiagnostics()
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        logger.info("====> didChange: ${params.textDocument.uri}")
        val document = serverContext.documentStore.applyChanges(params)
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didChange(context, document)
        serverContext.refreshProjectStructure()
        if (serverContext.enabledFeatures.diagnostics) {
            serverContext.republishOpenDiagnostics()
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        logger.info("====> didClose: ${params.textDocument.uri}")
        val document = serverContext.documentStore.close(params.textDocument.uri) ?: return
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didClose(context, document)
        serverContext.refreshProjectStructure()
        if (serverContext.enabledFeatures.diagnostics) {
            serverContext.publishDiagnostics(document, emptyList())
            serverContext.republishOpenDiagnostics()
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        logger.info("====> didSave: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri) ?: return
        val context = serverContext.requestContext()
        serverContext.analysisFacade.didSave(context, document)
        serverContext.refreshProjectStructure()
        if (serverContext.enabledFeatures.diagnostics) {
            serverContext.republishOpenDiagnostics()
        }
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        logger.info("====> completion: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.completion) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.completion(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== completion") } }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover> {
        logger.info("====> hover: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Hover(emptyList()))
        if (!serverContext.enabledFeatures.hover) return completed(Hover(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.hover(serverContext.requestContext(), document, params) ?: Hover(emptyList())
        }.also { it.thenAccept { logger.info("<==== hover") } }
    }

    override fun signatureHelp(params: SignatureHelpParams): CompletableFuture<SignatureHelp> {
        logger.info("====> signatureHelp: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(SignatureHelp(emptyList(), null, null))
        if (!serverContext.enabledFeatures.signatureHelp) return completed(SignatureHelp(emptyList(), null, null))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.signatureHelp(serverContext.requestContext(), document, params)
                ?: SignatureHelp(emptyList(), null, null)
        }.also { it.thenAccept { logger.info("<==== signatureHelp") } }
    }

    override fun declaration(params: DeclarationParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        logger.info("====> declaration: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.declaration) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.declaration(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== declaration") } }
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        logger.info("====> definition: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.definition) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.definition(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== definition") } }
    }

    override fun typeDefinition(params: TypeDefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        logger.info("====> typeDefinition: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.typeDefinition) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.typeDefinition(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== typeDefinition") } }
    }

    override fun implementation(params: ImplementationParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        logger.info("====> implementation: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(Either.forLeft(emptyList()))
        if (!serverContext.enabledFeatures.implementation) return completed(Either.forLeft(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.implementation(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== implementation") } }
    }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location>> {
        logger.info("====> references: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.references) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.references(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== references") } }
    }

    override fun documentHighlight(params: DocumentHighlightParams): CompletableFuture<List<DocumentHighlight>> {
        logger.info("====> documentHighlight: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.documentHighlight) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.documentHighlight(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== documentHighlight") } }
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> {
        logger.info("====> documentSymbol: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.documentSymbol) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.documentSymbols(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== documentSymbol") } }
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>> {
        logger.info("====> codeAction: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.codeAction) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.codeActions(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== codeAction") } }
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>> {
        logger.info("====> formatting: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.formatting) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.formatting(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== formatting") } }
    }

    override fun rangeFormatting(params: org.eclipse.lsp4j.DocumentRangeFormattingParams): CompletableFuture<List<TextEdit>> {
        logger.info("====> rangeFormatting: ${params.textDocument.uri}")
        return completed(emptyList())
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> {
        logger.info("====> rename: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(WorkspaceEdit())
        if (!serverContext.enabledFeatures.rename) return completed(WorkspaceEdit())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.rename(serverContext.requestContext(), document, params) ?: WorkspaceEdit()
        }.also { it.thenAccept { logger.info("<==== rename") } }
    }

    override fun prepareRename(
        params: PrepareRenameParams,
    ): CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> {
        logger.info("====> prepareRename: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyPrepareRenameResult())
        if (!serverContext.enabledFeatures.rename) return completed(emptyPrepareRenameResult())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.prepareRename(
                serverContext.requestContext(),
                document,
                RenameParams(params.textDocument, params.position, ""),
            ) ?: emptyPrepareRenameResult()
        }.also { it.thenAccept { logger.info("<==== prepareRename") } }
    }

    override fun foldingRange(params: FoldingRangeRequestParams): CompletableFuture<List<FoldingRange>> {
        logger.info("====> foldingRange: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.foldingRange) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.foldingRanges(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== foldingRange") } }
    }

    override fun selectionRange(params: SelectionRangeParams): CompletableFuture<List<SelectionRange>> {
        logger.info("====> selectionRange: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.selectionRange) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.selectionRanges(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== selectionRange") } }
    }

    override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> {
        logger.info("====> semanticTokensFull: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(SemanticTokens(emptyList()))
        if (!serverContext.enabledFeatures.semanticTokens) return completed(SemanticTokens(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.semanticTokensFull(serverContext.requestContext(), document, params)
                ?: SemanticTokens(emptyList())
        }.also { it.thenAccept { logger.info("<==== semanticTokensFull") } }
    }

    override fun semanticTokensRange(params: SemanticTokensRangeParams): CompletableFuture<SemanticTokens> {
        logger.info("====> semanticTokensRange: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(SemanticTokens(emptyList()))
        if (!serverContext.enabledFeatures.semanticTokens) return completed(SemanticTokens(emptyList()))
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.semanticTokensRange(serverContext.requestContext(), document, params)
                ?: SemanticTokens(emptyList())
        }.also { it.thenAccept { logger.info("<==== semanticTokensRange") } }
    }

    override fun inlayHint(params: InlayHintParams): CompletableFuture<List<InlayHint>> {
        logger.info("====> inlayHint: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(emptyList())
        if (!serverContext.enabledFeatures.inlayHints) return completed(emptyList())
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.inlayHints(serverContext.requestContext(), document, params)
        }.also { it.thenAccept { logger.info("<==== inlayHint") } }
    }

    override fun diagnostic(params: DocumentDiagnosticParams): CompletableFuture<DocumentDiagnosticReport> {
        logger.info("====> diagnostic: ${params.textDocument.uri}")
        val document = serverContext.documentStore.get(params.textDocument.uri)
            ?: return completed(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(emptyList())))
        if (!serverContext.enabledFeatures.diagnostics) {
            return completed(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(emptyList())))
        }
        return serverContext.requestExecutor.compute {
            val diagnostics = serverContext.collectDiagnostics(document)
            DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(diagnostics))
        }.also { it.thenAccept { logger.info("<==== diagnostic") } }
    }

    private fun emptyPrepareRenameResult(): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> {
        return Either3.forFirst(Range(Position(0, 0), Position(0, 0)))
    }

    private fun <T> completed(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)
}
