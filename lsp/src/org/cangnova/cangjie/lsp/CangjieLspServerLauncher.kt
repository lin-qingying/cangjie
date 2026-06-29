package org.cangnova.cangjie.lsp

import org.cangnova.cangjie.lsp.server.CangjieLanguageServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 仓颉 LSP 服务端的进程级启动入口。
 *
 * 该对象负责创建语言服务器、绑定 lsp4j launcher、启动监听以及 socket/stdio 两种传输模式。
 */
object CangjieLspServerLauncher {

    /**
     * 启动器日志记录器。
     *
     * 日志写入 LSP 日志流，用于观察连接建立、监听结束和启动失败。
     */
    private val logger = Logger.getLogger(CangjieLspServerLauncher::class.java.name)

    /**
     * 仅创建服务端实例，不启动监听。
     */
    fun create(options: CangjieLspServerOptions = CangjieLspServerOptions()): CangjieLanguageServer {
        return CangjieLanguageServer(options)
    }

    /**
     * 构建 Launcher，完成 server -> client 的双向绑定，但不启动监听。
     *
     * 这里显式启用 lsp4j 协议 tracing，把收发两侧的原始 JSON-RPC 报文都打印到 stderr，
     * 便于定位 initialize 阶段的 framing、JSON 结构和能力协商问题。
     */
    fun build(
        input: InputStream,
        output: OutputStream,
        options: CangjieLspServerOptions = CangjieLspServerOptions(),
    ): Pair<CangjieLanguageServer, Launcher<LanguageClient>> {
        val server = create(options)
        val launcher = LSPLauncher.createServerLauncher(
            server,
            input,
            output,
            // 真实客户端在 initialize.capabilities 上经常存在“协议允许但模型不完整”的输入。
            // 这里关闭 lsp4j 的反射式消息校验，避免把兼容性问题升级成 -32602 并中断会话。
            false,
            protocolTraceWriter(),
        )
        server.connect(launcher.remoteProxy)
        return server to launcher
    }

    /**
     * 启动监听并阻塞，直到连接断开或发生异常。
     */
    fun startAndAwait(launcher: Launcher<LanguageClient>) {
        logger.info("Starting LSP server listening...")
        val future = launcher.startListening()
        try {
            future.get()
        } catch (e: InterruptedException) {
            logger.log(Level.WARNING, "LSP server listening interrupted", e)
            Thread.currentThread().interrupt()
        } catch (e: ExecutionException) {
            logger.log(Level.SEVERE, "LSP server encountered a fatal error", e.cause ?: e)
            throw e
        }
    }

    /**
     * 一步完成：构建 + 启动 + 阻塞等待。
     */
    fun launch(
        input: InputStream,
        output: OutputStream,
        options: CangjieLspServerOptions = CangjieLspServerOptions(),
    ) {
        val (_, launcher) = build(input, output, options)
        startAndAwait(launcher)
    }

    /**
     * Socket 模式：监听指定端口，接受单个客户端连接后启动 LSP。
     */
    fun launchSocket(
        port: Int = 2088,
        options: CangjieLspServerOptions = CangjieLspServerOptions(),
    ) {
        val serverSocket = java.net.ServerSocket(port)
        logger.info("LSP server listening on TCP port $port")
        serverSocket.accept().use { socket ->
            logger.info("Client connected from ${socket.remoteSocketAddress}")
            launch(
                input = socket.getInputStream(),
                output = socket.getOutputStream(),
                options = options,
            )
        }
    }

    /**
     * 创建协议 trace 写出器。
     *
     * trace 输出到 LSP 日志流而非 stdout，避免破坏 JSON-RPC 协议通道。
     */
    private fun protocolTraceWriter(): PrintWriter = PrintWriter(LspIoManager.logStream, true)
}

/**
 * 独立进程模式的 LSP 主入口。
 *
 * 入口先隔离标准 I/O，再以 stdio 方式启动服务端；启动失败时只向日志流写入错误并退出进程。
 */
fun main(args: Array<String>) {
    LspIoManager.setupStandardIo()

    val logger = Logger.getLogger("CangjieLsp")
    logger.info("Starting Cangjie LSP Server with args: ${args.joinToString(" ")}")

    try {
        CangjieLspServerLauncher.launch(
            input = LspIoManager.inputStream,
            output = LspIoManager.outputStream,
        )
    } catch (t: Throwable) {
        val err = LspIoManager.logStream
        err.println("FATAL: LSP server crashed during startup")
        err.println("Exception: ${t.javaClass.name}: ${t.message}")
        t.printStackTrace(err)
        System.exit(1)
    }
}
