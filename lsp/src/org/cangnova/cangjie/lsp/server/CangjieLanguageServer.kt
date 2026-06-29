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
    /**
     * 语言服务器启动和运行选项。
     */
    private val options: CangjieLspServerOptions = CangjieLspServerOptions(),
) : LanguageServer, LanguageClientAware, AutoCloseable {
    /**
     * 语言服务器主日志记录器。
     */
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

    /**
     * 服务端是否已经收到 initialized 通知。
     */
    private val initialized = AtomicBoolean(false)

    /**
     * 服务端资源是否已经关闭。
     */
    private val closed = AtomicBoolean(false)

    /**
     * exit 是否已经处理过，防止重复调用退出策略。
     */
    private val exited = AtomicBoolean(false)

    /**
     * 惰性创建 LSP 编译器环境。
     *
     * 只有 initialize 成功进入上下文创建路径时才会触发环境构造。
     */
    private val environmentLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        options.environmentFactory()
    }

    /**
     * 当前服务端使用的 LSP 运行环境。
     */
    private val environment by environmentLazy

    /**
     * 惰性创建服务端运行上下文。
     */
    private val serverContextLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CangjieServerContext(
            descriptor = options.descriptor,
            environment = environment,
            analysisFacadeFactory = options.analysisFacadeFactory,
        )
    }

    /**
     * 当前服务端运行上下文。
     */
    private val serverContext by serverContextLazy

    /**
     * 惰性创建文本服务。
     */
    private val textDocumentServiceLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CangjieTextDocumentService(serverContext)
    }

    /**
     * 当前文本服务实例。
     */
    private val textDocumentService by textDocumentServiceLazy

    /**
     * 惰性创建工作区服务。
     */
    private val workspaceServiceLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CangjieWorkspaceService(serverContext)
    }

    /**
     * 当前工作区服务实例。
     */
    private val workspaceService by workspaceServiceLazy

    /**
     * Notebook 文档服务占位实例。
     */
    private val notebookDocumentService = CangjieNotebookDocumentService()

    /**
     * 接收并保存 LSP 客户端代理。
     *
     * 该方法不触发服务端上下文初始化，避免 connect 阶段提前创建分析环境。
     */
    override fun connect(client: LanguageClient) {
        logger.info("====> connect")
        this.client = client
        logger.info("<==== connect")
    }

    /**
     * 处理 initialize 请求并返回协商后的服务端能力。
     *
     * 方法在客户端连接存在时才创建上下文，随后初始化工作区状态、刷新项目结构并执行能力协商。
     */
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

    /**
     * 处理 initialized 通知。
     */
    override fun initialized(params: InitializedParams) {
        logger.info("====> initialized")
        initialized.set(true)
        logger.info("<==== initialized")
    }

    /**
     * 处理 shutdown 请求。
     *
     * 该方法只标记关闭意图，不主动退出进程；实际退出码由后续 exit 决定。
     */
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

    /**
     * 处理 exit 通知并调用可替换的退出策略。
     *
     * 如果此前收到 shutdown，则退出码为 0；否则按 LSP 生命周期语义返回 1。
     */
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

    /**
     * 处理 work done progress 取消通知。
     *
     * 当前服务端未维护进度任务，只记录调试日志。
     */
    override fun cancelProgress(params: WorkDoneProgressCancelParams) {
        logger.fine("cancelProgress: $params")
    }

    /**
     * 返回 notebook 文档服务。
     */
    override fun getNotebookDocumentService(): NotebookDocumentService = notebookDocumentService

    /**
     * 返回文本文件服务。
     */
    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    /**
     * 返回工作区服务。
     */
    override fun getWorkspaceService(): WorkspaceService = workspaceService

    /**
     * 关闭已经初始化的服务端上下文。
     *
     * 未初始化时不会反向触发环境创建，关闭过程保证幂等。
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (serverContextLazy.isInitialized()) {
            serverContext.close()
        }
    }
}
