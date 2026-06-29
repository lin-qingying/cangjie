package org.cangnova.cangjie.lsp

import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

/**
 * 彻底简化的 LSP I/O 管理器。
 * 
 * 核心设计：
 * 1. 物理隔离：直接使用 FileDescriptor.in/out/err，不依赖 System.in/out/err 的全局状态。
 * 2. 原始传输：协议流保持纯字节传输，不经过任何字符集转换。
 * 3. 强制编码：日志流直接写向原始 stderr，并显式指定 UTF-8 编码。
 */
object LspIoManager {

    // 记录原始引用。这是最基础、最不容易出错的 I/O 持有方式。
    /**
     * 直接绑定进程标准输入文件描述符的协议输入流。
     *
     * 该流绕过 `System.in`，避免后续标准流重定向影响 LSP 消息读取。
     */
    private val rawIn: InputStream = FileInputStream(FileDescriptor.`in`)

    /**
     * 直接绑定进程标准输出文件描述符的协议输出流。
     *
     * 该流绕过 `System.out`，确保 JSON-RPC 协议响应不会被日志或普通打印污染。
     */
    private val rawOut: OutputStream = FileOutputStream(FileDescriptor.out)

    /**
     * 获取用于协议传输的输入流。
     */
    val inputStream: InputStream get() = rawIn

    /**
     * 获取用于协议传输的输出流。
     */
    val outputStream: OutputStream get() = rawOut

    /**
     * 日志流，直接写向物理 stderr。
     */
    val logStream: PrintStream by lazy {
        try {
            PrintStream(FileOutputStream(FileDescriptor.err), true, "UTF-8")
        } catch (e: Exception) {
            System.err
        }
    }

    /**
     * 仅重定向 System.out/err，防止业务 println 污染 stdout。
     */
    fun setupStandardIo() {
        val logger = java.util.logging.Logger.getLogger("CangjieLsp")
        logger.info("Setting up standard I/O redirection")
        System.setOut(logStream)
        System.setErr(logStream)
    }
}
