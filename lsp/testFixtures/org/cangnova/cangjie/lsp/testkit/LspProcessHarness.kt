package org.cangnova.cangjie.lsp.testkit

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 真实进程级 LSP 会话。
 *
 * 与进程内的 `LspIntegrationTestSession` 不同，这里跑的是**独立 JVM**：
 * stdin/stdout（或 socket）就是协议通道，因此可以验证 stdio/socket 传输、
 * `Content-Length` 的字节语义、连续消息、stdout 纯净性这些只有跨进程才暴露的问题。
 *
 * 会话只依赖两根流 + 一个 stderr 文件，所以 stdio 与 socket 两种传输共用同一份帧编解码。
 */
class LspProcessSession internal constructor(
    /** 被测服务端进程。 */
    private val process: Process,
    /** 协议输入方向（服务端发出的帧）。 */
    private val protocolInput: InputStream,
    /** 协议输出方向（发给服务端的帧）。 */
    private val protocolOutput: OutputStream,
    /** 服务端 stderr 落盘文件。 */
    private val stderrFile: Path,
) : AutoCloseable {
    /** 已解析的响应。 */
    private val responses = LinkedBlockingQueue<Frame>()

    /**
     * 已到达但还没被认领的响应。
     *
     * JSON-RPC 允许响应乱序（并发处理），因此先到的其它 id 必须缓存而不是判失败。
     */
    private val pendingResponses = java.util.concurrent.ConcurrentHashMap<Int, Frame>()

    /** 已解析的通知（无 id 的消息）。 */
    private val notifications: MutableList<Frame> = java.util.Collections.synchronizedList(mutableListOf())

    /** 每个成功解析帧的「声明长度 / 实际字节数」。 */
    private val frameLengths: MutableList<Pair<Int, Int>> = java.util.Collections.synchronizedList(mutableListOf())

    /** stdout 上出现的非协议字节或畸形帧。 */
    private val purityViolations: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())

    /** 读取线程是否已经读到 EOF。 */
    private val stdoutDrained = AtomicBoolean(false)

    /**
     * 协议流读取异常（如 socket 被 `System.exit` 直接掐断）。
     *
     * 与「帧结构违规」分开记：socket 模式下的 `Connection reset` 是退出方式的副作用，
     * 不等于帧损坏；两种语义混在一起就会把正常退出判成失败。
     */
    private val streamFailure = java.util.concurrent.atomic.AtomicReference<String?>(null)

    /** 帧读取线程。 */
    private val readerThread: Thread = Thread({ readFrames() }, "lsp-process-frame-reader").apply {
        isDaemon = true
        start()
    }

    /** 服务端进程 PID，用于诊断信息。 */
    val pid: Long get() = process.pid()

    /**
     * 写入一帧：协议要求 `Content-Length` 是 **UTF-8 字节数**，不是字符数。
     */
    fun writeFrame(payload: String) {
        writeRaw(frameBytes(payload))
    }

    /**
     * 连续写入多帧（同一次 flush），用于验证服务端不依赖「一帧一个读事件」。
     */
    fun writeFramesInOneWrite(payloads: List<String>) {
        val buffer = ByteArrayOutputStream()
        payloads.forEach { payload -> buffer.write(frameBytes(payload)) }
        writeRaw(buffer.toByteArray())
    }

    /**
     * 等待指定 id 的响应。
     */
    fun awaitResponse(id: Int, timeoutSeconds: Long = DEFAULT_RESPONSE_TIMEOUT_SECONDS): Frame {
        pendingResponses.remove(id)?.let { return it }

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) {
                failProcess(
                    "等待响应 id=$id 超时（已收到 ${frameLengths.size} 帧；" +
                        "stdout 违规=${purityViolations}；stderr=${stderrTail()}）",
                )
            }
            val frame = responses.poll(remaining, TimeUnit.NANOSECONDS) ?: continue
            if (frame.id == id) return frame
            frame.id?.let { pendingResponses[it] = frame }
        }
    }

    /**
     * 等待进程退出并返回退出码。
     */
    fun awaitExit(timeoutSeconds: Long = DEFAULT_EXIT_TIMEOUT_SECONDS): Int {
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            failProcess("服务端进程在 ${timeoutSeconds}s 内未退出；stderr=${stderrTail()}")
        }
        return process.exitValue()
    }

    /**
     * 等待 stdout 读到 EOF（协议通道干净关闭）。
     */
    fun awaitStdoutDrained(timeoutSeconds: Long = 30) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (!stdoutDrained.get() && System.nanoTime() < deadline) {
            Thread.sleep(20)
        }
        assertTrue(stdoutDrained.get(), "stdout 未在 ${timeoutSeconds}s 内关闭；stderr=${stderrTail()}")
    }

    /**
     * 断言协议流是正常关闭的（读到 EOF 而非连接被重置）。
     *
     * 只对 stdio 有意义：socket 传输由 `System.exit` 收尾时本端会看到 `Connection reset`。
     */
    fun assertCleanClosure() {
        assertTrue(streamFailure.get() == null, "协议流异常关闭：${streamFailure.get()}")
    }

    /**
     * 断言 stdout 只有协议帧：没有多余文本、没有畸形帧、长度声明与实际字节数一致。
     */
    fun assertProtocolPureStdout() {
        assertTrue(purityViolations.isEmpty(), "stdout 出现协议外内容：${purityViolations.take(5)}")
        assertTrue(frameLengths.isNotEmpty(), "stdout 没有解析出任何协议帧")
        frameLengths.forEachIndexed { index, (declared, actual) ->
            assertTrue(
                declared == actual,
                "第 ${index + 1} 帧 Content-Length=$declared 与实际 UTF-8 字节数 $actual 不一致",
            )
        }
    }

    /** 已收到的全部帧数（响应 + 通知）。 */
    fun receivedFrameCount(): Int = frameLengths.size

    /** 已收到的通知快照。 */
    fun notificationFrames(): List<Frame> = notifications.toList()

    /** 服务端 stderr 全文（日志流），用于失败诊断。 */
    fun stderrText(): String = runCatching { Files.readString(stderrFile) }.getOrDefault("")

    /** 服务端 stderr 尾部若干行，用于断言消息里带上下文。 */
    fun stderrTail(lines: Int = 10): String =
        stderrText().lineSequence().toList().takeLast(lines).joinToString("\n")

    /**
     * 关闭协议写端并回收读取线程；进程由外层 [LspServerProcess] 负责。
     */
    override fun close() {
        runCatching { protocolOutput.close() }
        runCatching { protocolInput.close() }
        readerThread.join(TimeUnit.SECONDS.toMillis(5))
    }

    /**
     * 单帧字节（头 + UTF-8 体）。
     */
    private fun frameBytes(payload: String): ByteArray {
        val body = payload.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${body.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        return header + body
    }

    /**
     * 写原始字节并 flush。
     */
    private fun writeRaw(bytes: ByteArray) {
        synchronized(protocolOutput) {
            protocolOutput.write(bytes)
            protocolOutput.flush()
        }
    }

    /**
     * 逐帧读取协议流。
     *
     * 任何「头部不是 `Key: Value`」或「头部缺少 Content-Length」都记为违规：
     * 这类字节在真实客户端上就是协议损坏，不能被当成噪音忽略。
     */
    private fun readFrames() {
        val input = BufferedInputStream(protocolInput)
        try {
            while (true) {
                val headerLines = readHeaderLines(input) ?: break
                val contentLength = headerLines
                    .firstOrNull { it.substringBefore(':').trim().equals("Content-Length", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.toIntOrNull()
                if (contentLength == null) {
                    purityViolations += "帧头缺少合法 Content-Length：$headerLines"
                    break
                }
                val body = readExactly(input, contentLength) ?: run {
                    purityViolations += "帧体被截断：缺少 $contentLength 字节"
                    break
                }
                val text = String(body, StandardCharsets.UTF_8)
                frameLengths += contentLength to body.size
                if (!text.trimStart().startsWith("{")) {
                    purityViolations += "帧体不是 JSON 对象：${text.take(80)}"
                    break
                }
                val id = JSON_ID.find(text)?.groupValues?.get(1)?.toIntOrNull()
                if (id == null) {
                    notifications += Frame(null, text, contentLength)
                } else {
                    responses += Frame(id, text, contentLength)
                }
            }
        } catch (failure: Throwable) {
            streamFailure.set("${failure.javaClass.simpleName}: ${failure.message}")
        } finally {
            stdoutDrained.set(true)
        }
    }

    /**
     * 读取头部行，直到空行为止；流结束时返回 null。
     */
    private fun readHeaderLines(input: InputStream): List<String>? {
        val lines = mutableListOf<String>()
        val current = ByteArrayOutputStream()
        while (true) {
            val byte = input.read()
            if (byte < 0) return if (lines.isEmpty() && current.size() == 0) null else lines
            if (byte == '\n'.code) {
                val line = String(current.toByteArray(), StandardCharsets.US_ASCII).trimEnd('\r')
                current.reset()
                if (line.isEmpty()) return lines
                if (!HEADER_LINE.matches(line)) purityViolations += "非法帧头行：$line"
                lines += line
            } else {
                current.write(byte)
            }
        }
    }

    /**
     * 按声明长度读取定长帧体。
     */
    private fun readExactly(input: InputStream, length: Int): ByteArray? {
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val chunk = input.read(body, read, length - read)
            if (chunk < 0) return null
            read += chunk
        }
        return body
    }

    companion object {
        /** 响应等待上限：进程内要启动 headless 分析环境，慢机器上需要余量。 */
        private const val DEFAULT_RESPONSE_TIMEOUT_SECONDS: Long = 180L

        /** 进程退出等待上限。 */
        private const val DEFAULT_EXIT_TIMEOUT_SECONDS: Long = 120L

        /** 帧头行形态：`Key: Value`。 */
        private val HEADER_LINE = Regex("^[A-Za-z][A-Za-z0-9-]*: .*$")

        /** 顶层的 JSON-RPC `id` 字段（响应才带 id）。 */
        private val JSON_ID = Regex("\"id\"\\s*:\\s*(\\d+)")
    }
}

/**
 * 已解析的一帧。
 */
data class Frame(
    /** 响应 id；通知为 null。 */
    val id: Int?,
    /** 原始 JSON 文本。 */
    val body: String,
    /** 帧头声明的 `Content-Length`（UTF-8 字节数）。 */
    val declaredLength: Int,
) {
    /** 断言帧体包含指定片段。 */
    fun assertContains(fragment: String): Frame {
        assertTrue(body.contains(fragment), "帧体应包含 `$fragment`：$body")
        return this
    }
}

/**
 * 一个真实服务端进程 + 它的协议会话。
 */
class LspServerProcess internal constructor(
    /** 服务端进程。 */
    val process: Process,
    /** 协议会话。 */
    val session: LspProcessSession,
    /** socket 传输下建立的连接；stdio 为 null。 */
    private val socket: Socket?,
) : AutoCloseable {
    /** 服务端 stderr 全文。 */
    fun stderrText(): String = session.stderrText()

    /** 服务端 stderr 尾部。 */
    fun stderrTail(lines: Int = 20): String = session.stderrTail(lines)

    /**
     * 关闭会话、连接与进程。
     */
    override fun close() {
        runCatching { session.close() }
        runCatching { socket?.close() }
        if (process.isAlive) {
            process.destroy()
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly()
        }
    }
}

/**
 * 进程级断言失败：用 [AssertionError] 而不是 JUnit 的泛型 `fail`，
 * 因为这里多数调用点是语句位置，泛型返回类型无法推断。
 */
private fun failProcess(message: String): Nothing = throw AssertionError(message)

/**
 * 启动真实服务端进程。
 *
 * 服务端 main 来自测试源集（`LspProcessServerMain`）：它复用生产的
 * `LspIoManager` 与 `CangjieLspServerLauncher`，只把分析环境与 facade 换成可控实现，
 * 因此测到的仍是真实的传输与启动链路。
 */
object LspProcessHarness {
    /** 测试源集里的进程入口类。 */
    const val SERVER_MAIN_CLASS: String = "org.cangnova.cangjie.lsp.process.LspProcessServerMainKt"

    /**
     * 以 stdio 传输启动服务端进程。
     */
    fun startStdio(): LspServerProcess {
        val (process, stderrFile) = startProcess(listOf("--transport", "stdio"))
        return LspServerProcess(
            process = process,
            session = LspProcessSession(
                process = process,
                protocolInput = process.inputStream,
                protocolOutput = process.outputStream,
                stderrFile = stderrFile,
            ),
            socket = null,
        )
    }

    /**
     * 以 socket 传输启动服务端进程：先等端口就绪，再在连接上建立会话。
     */
    fun startSocket(port: Int): LspServerProcess {
        val (process, stderrFile) = startProcess(listOf("--transport", "socket", "--port", port.toString()))
        val socket = awaitSocketReady(port, process)
        return LspServerProcess(
            process = process,
            session = LspProcessSession(
                process = process,
                protocolInput = socket.getInputStream(),
                protocolOutput = socket.getOutputStream(),
                stderrFile = stderrFile,
            ),
            socket = socket,
        )
    }

    /**
     * 取一个当前空闲的本地端口。
     *
     * 取完立刻关闭，因此存在极小的竞争窗口；socket 用例连接失败时会重试，
     * 不把端口竞争误判成协议缺陷。
     */
    fun reserveFreePort(): Int = java.net.ServerSocket(0).use { it.localPort }

    /**
     * 启动进程并返回进程与 stderr 落盘文件。
     *
     * 命令行经 **JDK argument file**（`java @args.txt`）传递：测试运行 classpath 有 300+
     * 条目、50KB 上下，直接拼进命令行会在 Windows 上以 `CreateProcess error=206` 失败。
     */
    private fun startProcess(arguments: List<String>): Pair<Process, Path> {
        val stderrFile = Files.createTempFile("cangjie-lsp-stderr-", ".log")
        val argumentsFile = Files.createTempFile("cangjie-lsp-java-", ".args")
        Files.writeString(argumentsFile, argumentFileContent(arguments), StandardCharsets.UTF_8)
        val process = ProcessBuilder(
            javaExecutable().toAbsolutePath().toString(),
            "@${argumentsFile.toAbsolutePath()}",
        )
            .redirectError(stderrFile.toFile())
            .start()
        return process to stderrFile
    }

    /**
     * 组装 argument file 内容。
     *
     * 规则：一行一个 token；含空格才加引号；反斜杠统一换成正斜杠——argfile 里 `\` 是转义符，
     * Windows 路径直接写会被吃掉一个字符。
     */
    private fun argumentFileContent(arguments: List<String>): String {
        val classpath = launchClasspath().joinToString(File.pathSeparator) { it.replace('\\', '/') }
        val tokens = buildList {
            add("-Dfile.encoding=UTF-8")
            add("-Dsun.stdout.encoding=UTF-8")
            add("-Dsun.stderr.encoding=UTF-8")
            add("-Djava.awt.headless=true")
            add("-cp")
            add(classpath)
            add(SERVER_MAIN_CLASS)
            addAll(arguments)
        }
        return tokens.joinToString("\n") { token ->
            if (token.any { it.isWhitespace() }) "\"${token.replace("\"", "\\\"")}\"" else token
        } + "\n"
    }

    /**
     * 子进程 classpath。
     *
     * 优先用构建脚本注入的测试运行 classpath：Gradle 给 test worker 的 `java.class.path`
     * 可能只是一个 classpath jar（甚至被裁剪），透传会让子进程找不到测试类。
     */
    private fun launchClasspath(): List<String> =
        rawLaunchClasspath()
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }

    /**
     * 原始 classpath 字符串：构建脚本已把它落盘（避免超长 -D 属性撑爆 Windows 命令行）。
     */
    private fun rawLaunchClasspath(): String {
        val classpathFile = System.getProperty("lsp.test.classpath.file")
            ?.let(::File)
            ?.takeIf { it.isFile }
        return classpathFile?.readText()?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getProperty("java.class.path")
    }

    /**
     * 当前 JVM 的 java 可执行文件。
     */
    private fun javaExecutable(): Path {
        val home = Path.of(System.getProperty("java.home"))
        return listOf(home.resolve("bin").resolve("java.exe"), home.resolve("bin").resolve("java"))
            .firstOrNull { Files.isRegularFile(it) }
            ?: failProcess("找不到当前 JVM 的 java 可执行文件：$home")
    }
}

/**
 * 等待 socket 端口可连接；进程提前退出时立即失败并带上 stderr。
 */
fun awaitSocketReady(
    port: Int,
    process: Process,
    timeoutSeconds: Long = 120,
): Socket {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
    var lastFailure: Throwable? = null
    while (System.nanoTime() < deadline) {
        if (!process.isAlive) {
            failProcess("socket 服务端进程提前退出（code=${process.exitValue()}，port=$port）")
        }
        try {
            return Socket("127.0.0.1", port)
        } catch (failure: Throwable) {
            lastFailure = failure
            Thread.sleep(50)
        }
    }
    failProcess("socket 传输在 ${timeoutSeconds}s 内未就绪（port=$port）：${lastFailure?.message}")
}
