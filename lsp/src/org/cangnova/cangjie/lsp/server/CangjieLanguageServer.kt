package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieClientCapabilityNegotiator
import org.cangnova.cangjie.lsp.capabilities.CangjieServerCapabilitiesFactory
import org.cangnova.cangjie.lsp.state.LspProjectConfiguration
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 仓颉语言服务器主入口。
 *
 * 这里负责把 JSON-RPC 生命周期与平台上下文对齐：
 * 1. `connect()` 只建立客户端连接，不抢先初始化任何分析服务；
 * 2. `initialize()` 才真正创建 server context、环境和能力协商结果；
 * 3. `shutdown()/exit()` 只围绕已经初始化的上下文收尾，不反向触发惰性初始化。
 */
class CangjieLanguageServer(
    private val options: CangjieLspServerOptions = CangjieLspServerOptions(),
) : LanguageServer, LanguageClientAware, AutoCloseable {
    private val logger = Logger.getLogger(CangjieLanguageServer::class.java.name)

    /**
     * `connect()` 只保存 client，不触发 server context 初始化。
     */
    @Volatile
    private var client: LanguageClient? = null

    /**
     * 本地生命周期状态，避免在 `shutdown/exit` 时为了读取状态而反向触发惰性初始化。
     */
    private val shutdownRequested = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val exited = AtomicBoolean(false)

    private val environmentLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        options.environmentFactory()
    }
    private val environment by environmentLazy

    private val serverContextLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CangjieServerContext(
            descriptor = options.descriptor,
            environment = environment,
            analysisFacadeFactory = options.analysisFacadeFactory,
        )
    }
    private val serverContext by serverContextLazy

    private val textDocumentServiceLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CangjieTextDocumentService(serverContext)
    }
    private val textDocumentService by textDocumentServiceLazy

    private val workspaceServiceLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CangjieWorkspaceService(serverContext)
    }
    private val workspaceService by workspaceServiceLazy

    private val notebookDocumentService = CangjieNotebookDocumentService()

    override fun connect(client: LanguageClient) {
        logger.info("====> connect")
        this.client = client
        logger.info("<==== connect")
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        logger.info("====> initialize")

        val connectedClient = client
            ?: return CompletableFuture.failedFuture<InitializeResult>(
                IllegalStateException("LanguageClient not connected before initialize"),
            ).also { future ->
                future.whenComplete { _, ex ->
                    logger.log(Level.SEVERE, "<==== initialize failed", ex)
                }
            }

        // 在构造 server context 之前先落库标准库/库搜索路径，避免任何懒初始化的 analysis 组件捕获到空搜索路径。
        LspProjectConfiguration.fromInitializeParams(params).applyLibrarySearchProperties()

        return serverContext.requestExecutor.compute {
            serverContext.client = connectedClient
            serverContext.workspaceState.initialize(params)
            serverContext.refreshProjectStructure()
            val negotiation = CangjieClientCapabilityNegotiator.negotiate(
                params = params,
                serverFeatures = serverContext.enabledFeatures,
                descriptor = options.descriptor,
            )

            CangjieServerCapabilitiesFactory.createInitializeResult(
                descriptor = options.descriptor,
                negotiation = negotiation,
            )
        }.also { future ->
            future.whenComplete { result, ex ->
                if (ex != null) {
                    logger.log(Level.SEVERE, "<==== initialize failed", ex)
                } else {
                    logger.info("<==== initialize")
                    logger.fine("InitializeResult: $result")
                }
            }
        }
    }

    override fun initialized(params: InitializedParams) {
        logger.info("====> initialized")
        initialized.set(true)
        logger.info("<==== initialized")
    }

    override fun shutdown(): CompletableFuture<Any?> {
        logger.info("====> shutdown")
        shutdownRequested.set(true)

        if (serverContextLazy.isInitialized()) {
            try {
                serverContext.workspaceState.markShutdownRequested()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to mark shutdown requested", e)
            }
        }

        return CompletableFuture.completedFuture<Any?>(null).also { future ->
            future.whenComplete { _, ex ->
                if (ex != null) {
                    logger.log(Level.SEVERE, "<==== shutdown failed", ex)
                } else {
                    logger.info("<==== shutdown")
                }
            }
        }
    }

    override fun exit() {
        if (!exited.compareAndSet(false, true)) return
        logger.info("====> exit")

        try {
            close()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during close in exit", e)
        } finally {
            val exitCode = if (shutdownRequested.get()) 0 else 1
            logger.info("<==== exit (code $exitCode)")
            options.exitHandler(exitCode)
        }
    }

    override fun cancelProgress(params: WorkDoneProgressCancelParams) {
        logger.fine("cancelProgress: $params")
    }

    override fun getNotebookDocumentService(): NotebookDocumentService = notebookDocumentService

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (serverContextLazy.isInitialized()) {
            serverContext.close()
        }
    }
}
