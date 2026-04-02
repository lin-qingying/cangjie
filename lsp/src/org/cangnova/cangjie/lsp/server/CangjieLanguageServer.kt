package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieServerCapabilitiesFactory
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.WorkDoneProgressCancelParams
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.NotebookDocumentService
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class CangjieLanguageServer(
    private val options: CangjieLspServerOptions = CangjieLspServerOptions(),
) : LanguageServer, LanguageClientAware, AutoCloseable {
    private val environment = options.environmentFactory()
    private val serverContext = CangjieServerContext(
        descriptor = options.descriptor,
        environment = environment,
        analysisFacadeFactory = options.analysisFacadeFactory,
    )

    private val textDocumentService = CangjieTextDocumentService(serverContext)
    private val workspaceService = CangjieWorkspaceService(serverContext)
    private val notebookDocumentService = CangjieNotebookDocumentService()

    override fun connect(client: LanguageClient) {
        serverContext.client = client
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        return serverContext.requestExecutor.compute {
            serverContext.workspaceState.initialize(params)
            CangjieServerCapabilitiesFactory.createInitializeResult(
                descriptor = options.descriptor,
                features = serverContext.enabledFeatures,
            )
        }
    }

    override fun initialized(params: InitializedParams) {}

    override fun shutdown(): CompletableFuture<Any> {
        return serverContext.requestExecutor.compute {
            serverContext.workspaceState.markShutdownRequested()
            Any()
        }
    }

    override fun exit() {
        close()
    }

    override fun cancelProgress(params: WorkDoneProgressCancelParams) {}

    override fun getNotebookDocumentService(): NotebookDocumentService = notebookDocumentService

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService

    override fun close() {
        serverContext.close()
    }
}
