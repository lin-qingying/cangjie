package org.cangnova.cangjie.lsp.server

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.WorkspaceDiagnosticParams
import org.eclipse.lsp4j.WorkspaceDiagnosticReport
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * LSP 工作区服务。
 *
 * 这一层只负责协议事件编排，不再各自拼装 project-structure 刷新、diagnostics 重发布或
 * request context。所有这些能力统一落到 [CangjieServerContext] 和 Analysis facade。
 */
class CangjieWorkspaceService(
    private val serverContext: CangjieServerContext,
) : WorkspaceService {
    private val logger = Logger.getLogger(CangjieWorkspaceService::class.java.name)

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        logger.info("====> didChangeConfiguration")
        refreshWorkspaceSemantics()
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        logger.info("====> didChangeWatchedFiles")
        refreshWorkspaceSemantics()
    }

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        logger.info("====> didChangeWorkspaceFolders")
        serverContext.workspaceState.updateWorkspaceFolders(
            added = params.event.added,
            removed = params.event.removed,
        )
        serverContext.refreshProjectStructure()
        serverContext.analysisFacade.didChangeWorkspaceFolders(
            context = serverContext.requestContext(),
            added = params.event.added,
            removed = params.event.removed,
        )
        republishOpenDiagnostics()
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        logger.info("====> executeCommand: ${params.command}")
        return CompletableFuture.completedFuture(Any()).also {
            it.thenAccept { logger.info("<==== executeCommand") }
        }
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<Either<List<SymbolInformation>, List<WorkspaceSymbol>>> {
        logger.info("====> symbol: ${params.query}")
        if (!serverContext.enabledFeatures.workspaceSymbol) {
            return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        }
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.workspaceSymbols(serverContext.requestContext(), params)
        }.also { it.thenAccept { logger.info("<==== symbol") } }
    }

    override fun diagnostic(params: WorkspaceDiagnosticParams): CompletableFuture<WorkspaceDiagnosticReport> {
        logger.info("====> diagnostic (workspace)")
        if (!serverContext.enabledFeatures.diagnostics) {
            return CompletableFuture.completedFuture(WorkspaceDiagnosticReport(emptyList()))
        }
        return serverContext.requestExecutor.compute {
            val context = serverContext.requestContext()
            WorkspaceDiagnosticReport(serverContext.analysisFacade.collectWorkspaceDiagnostics(context))
        }.also { it.thenAccept { logger.info("<==== diagnostic (workspace)") } }
    }

    /**
     * 工作区配置或磁盘事件变化后，必须先刷新平台 project structure，
     * 再通知 Analysis facade，同步所有打开文档的语义快照与 push diagnostics。
     */
    private fun refreshWorkspaceSemantics() {
        serverContext.refreshProjectStructure()
        serverContext.analysisFacade.didRefreshProjectStructure(serverContext.requestContext())
        republishOpenDiagnostics()
    }

    /**
     * 工作区结构变化后，重新发布所有打开文档的诊断。
     *
     * 这样客户端看到的 push diagnostics 与新的 project-structure / snapshot 绑定保持一致，
     * 不会继续保留旧模块图下的结果。
     */
    private fun republishOpenDiagnostics() {
        if (!serverContext.enabledFeatures.diagnostics) return

        serverContext.documentStore.all().forEach { document ->
            serverContext.requestExecutor.compute {
                val diagnostics = serverContext.collectDiagnostics(document)
                serverContext.publishDiagnostics(document, diagnostics)
            }.join()
        }
    }
}
