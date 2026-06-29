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
    /**
     * 工作区服务共享的 LSP 运行时上下文。
     */
    private val serverContext: CangjieServerContext,
) : WorkspaceService {
    /**
     * 工作区服务日志记录器。
     */
    private val logger = Logger.getLogger(CangjieWorkspaceService::class.java.name)

    /**
     * 处理客户端配置变更通知。
     *
     * 配置变更可能影响项目结构和诊断，因此统一触发工作区语义刷新。
     */
    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        logger.info("====> didChangeConfiguration")
        refreshWorkspaceSemantics()
    }

    /**
     * 处理文件监听变更通知。
     *
     * 磁盘文件变化会影响 unopened 文件和依赖关系，必须刷新项目结构并重发打开文档诊断。
     */
    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        logger.info("====> didChangeWatchedFiles")
        refreshWorkspaceSemantics()
    }

    /**
     * 处理工作区目录增删通知。
     *
     * 方法同步 workspace folder 状态、刷新 Analysis API 项目结构，并通知 analysis facade 执行语义层更新。
     */
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
        serverContext.republishOpenDiagnostics()
    }

    /**
     * 处理 workspace/executeCommand 请求。
     *
     * 当前未定义具体命令语义，因此返回空对象作为协议占位并保留日志。
     */
    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        logger.info("====> executeCommand: ${params.command}")
        return CompletableFuture.completedFuture(Any()).also {
            it.thenAccept { logger.info("<==== executeCommand") }
        }
    }

    /**
     * 处理 workspace/symbol 查询。
     *
     * 功能未启用时返回空结果；启用时通过请求执行器串行调用 analysis facade。
     */
    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<Either<List<SymbolInformation>, List<WorkspaceSymbol>>> {
        logger.info("====> symbol: ${params.query}")
        if (!serverContext.enabledFeatures.workspaceSymbol) {
            return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        }
        return serverContext.requestExecutor.compute {
            serverContext.analysisFacade.workspaceSymbols(serverContext.requestContext(), params)
        }.also { it.thenAccept { logger.info("<==== symbol") } }
    }

    /**
     * 处理 workspace diagnostics pull 请求。
     *
     * 功能未启用时返回空报告；启用时按当前请求上下文收集工作区诊断。
     */
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
        serverContext.republishOpenDiagnostics()
    }
}
