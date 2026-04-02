package org.cangnova.cangjie.lsp.server

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.WorkspaceDiagnosticParams
import org.eclipse.lsp4j.WorkspaceDiagnosticReport
import org.eclipse.lsp4j.WorkspaceDocumentDiagnosticReport
import org.eclipse.lsp4j.WorkspaceFullDocumentDiagnosticReport
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class CangjieWorkspaceService(
    private val serverContext: CangjieServerContext,
) : WorkspaceService {
    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {}

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {}

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        serverContext.workspaceState.updateWorkspaceFolders(
            added = params.event.added,
            removed = params.event.removed,
        )
        serverContext.analysisFacade.didChangeWorkspaceFolders(
            context = serverContext.requestContext(),
            added = params.event.added,
            removed = params.event.removed,
        )
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(Any())
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<Either<List<out SymbolInformation>, List<out WorkspaceSymbol>>> {
        if (!serverContext.enabledFeatures.workspaceSymbol) {
            return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        }
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.workspaceSymbols(serverContext.requestContext(), params)
        }
    }

    override fun diagnostic(params: WorkspaceDiagnosticParams): CompletableFuture<WorkspaceDiagnosticReport> {
        if (!serverContext.enabledFeatures.diagnostics) {
            return CompletableFuture.completedFuture(WorkspaceDiagnosticReport(emptyList()))
        }
        return serverContext.requestExecutor.compute {
            val context = serverContext.requestContext()
            val items = serverContext.documentStore.all().map { document ->
                val diagnostics = serverContext.analysisFacade.collectDiagnostics(context, document)
                WorkspaceDocumentDiagnosticReport(
                    WorkspaceFullDocumentDiagnosticReport(diagnostics, document.uri, document.version),
                )
            }
            WorkspaceDiagnosticReport(items)
        }
    }
}
