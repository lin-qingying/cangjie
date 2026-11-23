/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.macro.process

import com.sun.jna.Native
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import org.cangnova.cangjie.macro.protocol.PipeTransport
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

/**
 * LSPMacroServer 的真实进程和官方帧传输。
 *
 * 编译器与 IDE 预览共用此接口；Windows HANDLE、Job 和标准流由公共进程层独占。
 * beginClose 必须先于 ExitTask，退出观察不能把已发起的正常关闭当成启动失败。
 */
interface LspMacroServerProcess : AutoCloseable {
    val transport: MacroProcessTransport
    val processId: Long

    fun isAlive(): Boolean

    /** 在发送退出协议前登记关闭意图；已经退出的进程仍按原退出事实报告。 */
    fun beginClose()

    /** 与同一关闭请求关联真实 ExitTask 发送结果；观察器不追认发送失败为正常退出。 */
    fun finishCloseRequest(exitTaskSent: Boolean)

    /** 取消阻塞协议请求时终止当前真实进程及其子进程，不经过客户端协议锁。 */
    fun terminate()

    companion object {
        /** SDK 环境、工作目录和协议端点均来自当前请求，不借用 IDE 的标准流。 */
        fun start(
            executable: File,
            sdkHome: String,
            processEnvironment: Map<String, String>,
            enableParallel: Boolean,
            logger: Logger,
            onProcessOutput: ((String) -> Unit)? = null,
        ): LspMacroServerProcess {
            check(executable.isFile && executable.canExecute()) { "LSPMacroServer 不可执行: $executable" }
            val process = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
                WindowsLspMacroServerProcess.start(executable, sdkHome, enableParallel, processEnvironment, logger, onProcessOutput)
            } else {
                UnixLspMacroServerProcess.start(executable, sdkHome, enableParallel, processEnvironment, logger, onProcessOutput)
            }
            logger.info("LSPMacroServer 已启动: $executable; pid=${process.processId}")
            return process
        }
    }
}

/** Unix 的 fd 0/1 保持官方协议用途，标准错误单独读取，不抢读协议 stdout。 */
private class UnixLspMacroServerProcess(
    private val process: Process,
    private val logger: Logger,
) : LspMacroServerProcess {
    override val transport: MacroProcessTransport =
        PipeMacroProcessTransport(PipeTransport(process.inputStream, process.outputStream))
    override val processId: Long = process.pid()
    private val lifecycleLock = Any()
    private var closeRequested = false
    private var closeRequestFinished = false
    private var exitTaskSent = false
    private var forcedTermination = false
    private var observedExitCode: Int? = null
    private var exitReported = false
    private var closed = false

    override fun isAlive(): Boolean = synchronized(lifecycleLock) { !closed && process.isAlive }

    override fun beginClose() = synchronized(lifecycleLock) {
        if (!closed) {
            if (process.isAlive) closeRequested = true else reportExit(process.exitValue())
        }
    }

    override fun finishCloseRequest(exitTaskSent: Boolean): Unit = synchronized(lifecycleLock) {
        closeRequestFinished = true
        this.exitTaskSent = exitTaskSent
        logger.info("LSPMacroServer close protocol: pid=$processId; exitTaskSent=$exitTaskSent")
        observedExitCode?.let(::reportExit)
    }

    override fun terminate() {
        beginClose()
        synchronized(lifecycleLock) {
            if (!closed && process.isAlive) {
                if (!closeRequestFinished) finishCloseRequest(false)
                forcedTermination = true
                process.destroyForcibly()
            }
        }
        close()
    }

    override fun close() {
        beginClose()
        synchronized(lifecycleLock) {
            if (closed) return
            if (!closeRequestFinished) finishCloseRequest(false)
            try {
                transport.close()
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    logger.warning("LSPMacroServer 未在 3 秒内退出，强制终止")
                    forcedTermination = true
                    process.destroyForcibly()
                    process.waitFor()
                }
                reportExit(process.exitValue())
            } finally {
                process.errorStream.close()
                closed = true
            }
        }
    }

    private fun reportExit(exitCode: Int) {
        if (exitReported) return
        observedExitCode = exitCode
        if (closeRequested && !closeRequestFinished && !forcedTermination) return
        exitReported = true
        val expectedShutdown = forcedTermination || (closeRequested && exitTaskSent && exitCode == 0)
        val message = "LSPMacroServer exited: pid=$processId; exitCode=$exitCode; shutdownRequested=$expectedShutdown"
        if (expectedShutdown) logger.info(message) else logger.severe(message)
    }

    companion object {
        fun start(
            executable: File,
            sdkHome: String,
            enableParallel: Boolean,
            processEnvironment: Map<String, String>,
            logger: Logger,
            onProcessOutput: ((String) -> Unit)?,
        ): UnixLspMacroServerProcess {
            val cjc = File(sdkHome, "bin/cjc")
            check(cjc.isFile && cjc.canExecute()) { "SDK cjc 不可执行: $cjc" }
            val command = listOf(executable.absolutePath, "0", "1", if (enableParallel) "1" else "0",
                cjc.absolutePath, ProcessHandle.current().pid().toString())
            val builder = ProcessBuilder(command).directory(File(sdkHome)).redirectErrorStream(false)
            val environment = mergeMacroProcessEnvironment(builder.environment().toMap(), processEnvironment, false).toMutableMap()
            val libraryKey = unixDynamicLibraryPathVariable(System.getProperty("os.name"))
            val directories = unixDynamicLibraryDirectories(sdkHome)
            if (directories.isNotEmpty()) {
                environment[libraryKey] = (directories + listOfNotNull(environment[libraryKey]?.takeIf(String::isNotBlank)))
                    .joinToString(File.pathSeparator)
            }
            builder.environment().clear()
            builder.environment().putAll(environment)
            val process = builder.start()
            val connection = UnixLspMacroServerProcess(process, logger)
            Thread({
                process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.forEachLine { line ->
                        logger.fine("LSPMacroServer stderr: $line")
                        onProcessOutput?.invoke(line)
                    }
                }
            }, "LSPMacroServer-stderr-reader").apply { isDaemon = true; start() }
            Thread({
                val exitCode = process.waitFor()
                synchronized(connection.lifecycleLock) { connection.reportExit(exitCode) }
            }, "LSPMacroServer-exit-watch").apply { isDaemon = true; start() }
            return connection
        }
    }
}

/**
 * Windows 平台下启动后的 LSPMacroServer 进程和匿名管道连接。
 */
private class WindowsLspMacroServerProcess(
    /**
     * 父进程侧读写宏协议消息的 Windows 管道传输。
     */
    override val transport: WindowsPipeMacroProcessTransport,
    /**
     * CreateProcessW 返回的进程和主线程句柄。
     */
    private val processInfo: WinBase.PROCESS_INFORMATION,
    /** 非继承 Job 句柄由当前连接持有；父 JVM 退出仍会触发服务器回收。 */
    private val processJob: WindowsMacroProcessJob,
    /**
     * 进程生命周期日志器。
     */
    private val logger: Logger,
) : LspMacroServerProcess {
    private val lifecycleLock = Any()
    private var closeRequested = false
    private var closeRequestFinished = false
    private var exitTaskSent = false
    private var forcedTermination = false
    private var observedExitCode: Int? = null
    private var exitReported = false
    private var closed = false
    private val startedAtNanos = System.nanoTime()
    override val processId: Long = processInfo.dwProcessId.toLong()

    /**
     * 检查 LSPMacroServer 进程是否仍在运行。
     */
    override fun isAlive(): Boolean = synchronized(lifecycleLock) {
        if (closed) false else when (Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, 0)) {
            WAIT_TIMEOUT -> true
            0 -> false
            else -> throw com.sun.jna.LastErrorException(Native.getLastError())
        }
    }

    override fun beginClose() = synchronized(lifecycleLock) {
        if (!closed) {
            when (Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, 0)) {
                WAIT_TIMEOUT -> {
                    if (!closeRequested) logger.info("LSPMacroServer close requested: pid=$processId")
                    closeRequested = true
                }
                0 -> reportExit(readExitCode(processInfo.hProcess))
                else -> throw com.sun.jna.LastErrorException(Native.getLastError())
            }
        }
    }

    override fun finishCloseRequest(exitTaskSent: Boolean): Unit = synchronized(lifecycleLock) {
        closeRequestFinished = true
        this.exitTaskSent = exitTaskSent
        logger.info("LSPMacroServer close protocol: pid=$processId; exitTaskSent=$exitTaskSent")
        observedExitCode?.let(::reportExit)
    }

    override fun terminate() {
        beginClose()
        synchronized(lifecycleLock) {
            if (!closed && Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, 0) == WAIT_TIMEOUT) {
                if (!closeRequestFinished) finishCloseRequest(false)
                forcedTermination = true
                checkWindows("TerminateProcess(LSPMacroServer)") { Kernel32.INSTANCE.TerminateProcess(processInfo.hProcess, 1) }
            }
        }
        close()
    }

    /** 关闭观察器独占复制的 HANDLE；连接资源的释放不会让观察器读到无效句柄。 */
    private fun startExitWatcher() {
        val observedHandle = WinNT.HANDLEByReference()
        val currentProcess = Kernel32.INSTANCE.GetCurrentProcess()
        checkWindows("DuplicateHandle(LSPMacroServer exit observer)") {
            Kernel32.INSTANCE.DuplicateHandle(currentProcess, processInfo.hProcess, currentProcess,
                observedHandle, 0, false, 2)
        }
        val handle = observedHandle.value
        Thread({
            try {
                when (Kernel32.INSTANCE.WaitForSingleObject(handle, -1)) {
                    0 -> {
                        val exitCode = readExitCode(handle)
                        synchronized(lifecycleLock) { reportExit(exitCode) }
                    }
                    else -> logger.severe("WaitForSingleObject(LSPMacroServer exit observer) failed: ${Native.getLastError()}")
                }
            } finally {
                closeHandle(handle)
            }
        }, "LSPMacroServer-exit-watch").apply { isDaemon = true; start() }
    }

    private fun readExitCode(handle: WinNT.HANDLE): Int {
        val exitCode = IntByReference()
        checkWindows("GetExitCodeProcess(LSPMacroServer)") { Kernel32.INSTANCE.GetExitCodeProcess(handle, exitCode) }
        return exitCode.value
    }

    private fun reportExit(exitCode: Int) {
        if (exitReported) return
        observedExitCode = exitCode
        if (closeRequested && !closeRequestFinished && !forcedTermination) return
        exitReported = true
        val expectedShutdown = forcedTermination || (closeRequested && exitTaskSent && exitCode == 0)
        val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000
        val timing = if (elapsedMillis < EARLY_EXIT_WATCH_MILLIS) "within ${EARLY_EXIT_WATCH_MILLIS}ms" else "after ${elapsedMillis}ms"
        val message = "LSPMacroServer exited $timing: pid=$processId; exitCode=$exitCode; shutdownRequested=$expectedShutdown"
        if (expectedShutdown) logger.info(message) else logger.severe(message)
    }

    override fun close() {
        var failure: Throwable? = null
        fun release(action: () -> Unit) {
            try { action() } catch (error: Throwable) {
                val previous = failure
                if (previous == null) failure = error else previous.addSuppressed(error)
            }
        }
        release(::beginClose)
        synchronized(lifecycleLock) {
            if (closed) return
            if (!closeRequestFinished) finishCloseRequest(false)
            release(transport::close)
            release {
                when (Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, CLOSE_TIMEOUT_MILLIS)) {
                    WAIT_TIMEOUT -> {
                        logger.warning("LSPMacroServer 未在 3 秒内退出，强制终止")
                        forcedTermination = true
                        checkWindows("TerminateProcess(LSPMacroServer close)") { Kernel32.INSTANCE.TerminateProcess(processInfo.hProcess, 1) }
                        check(Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, CLOSE_TIMEOUT_MILLIS) == 0) {
                            "LSPMacroServer did not terminate"
                        }
                    }
                    0 -> Unit
                    else -> throw com.sun.jna.LastErrorException(Native.getLastError())
                }
                reportExit(readExitCode(processInfo.hProcess))
            }
            release(processJob::close)
            release { closeHandle(processInfo.hThread) }
            release { closeHandle(processInfo.hProcess) }
            closed = true
        }
        failure?.let { throw it }
    }

    companion object {
        /**
         * 创建 Windows 匿名管道、启动 LSPMacroServer，并返回父进程侧连接对象。
         */
        fun start(
            executable: File,
            sdkHome: String,
            enableParallel: Boolean,
            processEnvironment: Map<String, String>,
            logger: Logger,
            onProcessOutput: ((String) -> Unit)?,
        ): WindowsLspMacroServerProcess {
            val attributes = WinBase.SECURITY_ATTRIBUTES().apply {
                dwLength = DWORD(size().toLong())
                bInheritHandle = true
            }
            val ownedHandles = mutableListOf<WinNT.HANDLE>()
            val processInfo = WinBase.PROCESS_INFORMATION()
            var processCreated = false
            var processJob: WindowsMacroProcessJob? = null
            fun pipe(name: String): Pair<WinNT.HANDLE, WinNT.HANDLE> {
                val read = WinNT.HANDLEByReference()
                val write = WinNT.HANDLEByReference()
                checkWindows("CreatePipe($name)") { Kernel32.INSTANCE.CreatePipe(read, write, attributes, 0) }
                return Pair(read.value, write.value).also { ownedHandles += it.first; ownedHandles += it.second }
            }
            fun closeOwned(handle: WinNT.HANDLE) {
                check(ownedHandles.remove(handle)) { "Macro startup no longer owns HANDLE" }
                closeHandle(handle)
            }
            fun drain(handle: WinNT.HANDLE, errorStream: Boolean) {
                check(ownedHandles.remove(handle))
                Thread({
                    try {
                        val buffer = ByteArray(4096)
                        val read = IntByReference()
                        while (Kernel32.INSTANCE.ReadFile(handle, buffer, buffer.size, read, null)) {
                            if (read.value > 0) {
                                val text = String(buffer, 0, read.value, Charsets.UTF_8)
                                if (errorStream) logger.severe("LSPMacroServer stderr: ${text.trim()}")
                                else logger.fine("LSPMacroServer stdout: ${text.trim()}")
                                onProcessOutput?.invoke(text)
                            }
                        }
                    } finally { closeHandle(handle) }
                }, if (errorStream) "LSPMacroServer-stderr-reader" else "LSPMacroServer-stdout-reader")
                    .apply { isDaemon = true; start() }
            }
            try {
                val (parentRead, childWrite) = pipe("macro output")
                val (childRead, parentWrite) = pipe("macro input")
                val (stderrRead, stderrWrite) = pipe("stderr")
                val (stdinRead, stdinWrite) = pipe("stdin")
                val (stdoutRead, stdoutWrite) = pipe("stdout")
                listOf(parentRead, parentWrite, stderrRead, stdinWrite, stdoutRead).forEach { handle ->
                    checkWindows("SetHandleInformation(parent macro endpoint)") {
                        Kernel32.INSTANCE.SetHandleInformation(handle, 1, 0)
                    }
                }
                processJob = WindowsMacroProcessJob()
                val commandLine = buildCommandLine(executable, sdkHome, childRead, childWrite, enableParallel)
                val environment = buildWindowsEnvironment(processEnvironment)
                environment.use {
                    WindowsMacroProcessStartup(listOf(childRead, childWrite, stdinRead, stdoutWrite, stderrWrite)).use { startup ->
                        startup.createProcess(commandLine, it, sdkHome, stdinRead, stdoutWrite, stderrWrite, processInfo)
                    }
                }
                processCreated = true
                processJob.attach(processInfo.hProcess)
                processJob.resume(processInfo.hThread)
                closeOwned(childRead)
                closeOwned(childWrite)
                closeOwned(stderrWrite)
                closeOwned(stdinRead)
                closeOwned(stdoutWrite)
                // stdin 不承载宏协议；父写端关闭后保持一个有效的 EOF 标准输入。
                closeOwned(stdinWrite)
                val connection = WindowsLspMacroServerProcess(
                    WindowsPipeMacroProcessTransport(parentRead, parentWrite), processInfo, processJob, logger)
                connection.startExitWatcher()
                drain(stderrRead, true)
                drain(stdoutRead, false)
                check(ownedHandles.remove(parentRead) && ownedHandles.remove(parentWrite))
                return connection
            } catch (failure: Throwable) {
                fun release(action: () -> Unit) {
                    try { action() } catch (error: Throwable) { failure.addSuppressed(error) }
                }
                if (processCreated) {
                    release { checkWindows("TerminateProcess(failed macro startup)") {
                        Kernel32.INSTANCE.TerminateProcess(processInfo.hProcess, 1)
                    } }
                }
                processJob?.let { job -> release(job::close) }
                if (processCreated) {
                    release { closeHandle(processInfo.hThread) }
                    release { closeHandle(processInfo.hProcess) }
                }
                ownedHandles.asReversed().forEach { handle -> release { closeHandle(handle) } }
                throw failure
            }
        }

        /**
         * 构造 Windows LSPMacroServer 官方参数格式的命令行。
         */

        private fun buildCommandLine(
            executable: File,
            sdkHome: String,
            childRead: WinNT.HANDLE,
            childWrite: WinNT.HANDLE,
            enableParallel: Boolean,
        ): String = buildString {
            append(quoteCommandArgument(executable.absolutePath))
            append(' ')
            append(handleValue(childRead))
            append(' ')
            append(handleValue(childWrite))
            append(' ')
            append(if (enableParallel) "1" else "0")
            append(' ')
            append(quoteCommandArgument(cjcExecutable(sdkHome).absolutePath))
        }

        /**
         * 将 Windows HANDLE 转成命令行可传递的整数句柄值。
         */
        private fun handleValue(handle: WinNT.HANDLE): String =
            Pointer.nativeValue(handle.pointer).toString()

        /**
         * 构造 LSPMacroServer 运行所需的 Windows 环境变量块。
         */
        private fun buildWindowsEnvironment(processEnvironment: Map<String, String>): Memory {
            val environment = mergeMacroProcessEnvironment(
                inheritedEnvironment = System.getenv(),
                configuredEnvironment = processEnvironment,
                caseInsensitiveKeys = true,
            )

            val block = environment.entries
                .sortedBy { it.key.uppercase() }
                .joinToString(separator = "\u0000", postfix = "\u0000\u0000") { (key, value) -> "$key=$value" }
            return block.toWideCharMemory()
        }
    }
}

/**
 * 合并父进程与 SDK 环境，确保 Windows 环境变量名按不区分大小写的规则覆盖。
 */
internal fun mergeMacroProcessEnvironment(
    inheritedEnvironment: Map<String, String>,
    configuredEnvironment: Map<String, String>,
    caseInsensitiveKeys: Boolean,
): Map<String, String> = buildMap {
    fun putEnvironmentVariable(key: String, value: String) {
        val previousKey = if (caseInsensitiveKeys) {
            keys.firstOrNull { it.equals(key, ignoreCase = true) }
        } else {
            key.takeIf { it in this }
        }
        if (previousKey != null) remove(previousKey)
        put(key, value)
    }

    inheritedEnvironment.forEach(::putEnvironmentVariable)
    configuredEnvironment.forEach(::putEnvironmentVariable)
}

/**
 * 使用 Windows HANDLE 实现的宏协议传输层。
 */
private class WindowsPipeMacroProcessTransport(
    /**
     * 父进程用于读取服务端消息的管道句柄。
     */
    private val readHandle: WinNT.HANDLE,
    /**
     * 父进程用于写入服务端消息的管道句柄。
     */
    private val writeHandle: WinNT.HANDLE,
) : MacroProcessTransport {
    /**
     * 标记底层 HANDLE 是否已经关闭，保证 close 幂等。
     */
    private val closed = AtomicBoolean(false)

    /**
     * 向服务端写入带 8 字节长度前缀的宏协议消息。
     */
    override fun send(payload: ByteArray) {
        val lenBuf = ByteBuffer.allocate(SIZE_PREFIX_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        lenBuf.putLong(payload.size.toLong())
        writeFully(lenBuf.array())

        var offset = 0
        while (offset < payload.size) {
            val length = minOf(CHUNK_SIZE, payload.size - offset)
            writeFully(payload.copyOfRange(offset, offset + length))
            offset += length
        }
    }

    /**
     * 从服务端读取带 8 字节长度前缀的宏协议消息。
     */
    override fun receive(): ByteArray {
        val lenBuf = ByteArray(SIZE_PREFIX_BYTES)
        readFully(lenBuf)
        val length = ByteBuffer.wrap(lenBuf).order(ByteOrder.LITTLE_ENDIAN).getLong().toInt()
        if (length <= 0) throw IOException("LSPMacroServer 返回非法消息长度: $length")

        val payload = ByteArray(length)
        readFully(payload)
        return payload
    }

    /**
     * 关闭父进程侧读写 HANDLE。
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        closeHandle(readHandle)
        closeHandle(writeHandle)
    }

    /**
     * 使用 WriteFile 循环写完整个字节数组。
     */
    private fun writeFully(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val written = IntByReference()
            val chunk = bytes.copyOfRange(offset, bytes.size)
            val ok = Kernel32.INSTANCE.WriteFile(writeHandle, chunk, chunk.size, written, null)
            if (!ok) throw IOException("WriteFile(LSPMacroServer) failed: ${Native.getLastError()}")
            if (written.value <= 0) throw IOException("WriteFile(LSPMacroServer) wrote 0 bytes")
            offset += written.value
        }
    }

    /**
     * 使用 ReadFile 循环读满目标字节数组。
     */
    private fun readFully(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val read = IntByReference()
            val chunk = ByteArray(bytes.size - offset)
            val ok = Kernel32.INSTANCE.ReadFile(readHandle, chunk, chunk.size, read, null)
            if (!ok) throw IOException("ReadFile(LSPMacroServer) failed: ${Native.getLastError()}")
            if (read.value <= 0) throw IOException("LSPMacroServer pipe closed")
            System.arraycopy(chunk, 0, bytes, offset, read.value)
            offset += read.value
        }
    }
}

/**
 * 按 Windows 命令行规则为单个参数添加引号。
 */
private fun quoteCommandArgument(value: String): String =
    "\"" + value.replace("\"", "\\\"") + "\""

/**
 * 将字符串转换为 Windows API 需要的 NUL 结尾字符数组。
 */
private fun String.toCharArrayWithTerminator(): CharArray =
    (this + "\u0000").toCharArray()

/**
 * 将字符串编码到 JNA 宽字符内存块。
 */
private fun String.toWideCharMemory(): Memory {
    val memory = Memory(((length + 1) * Native.WCHAR_SIZE).toLong())
    memory.clear()
    memory.setWideString(0, this)
    return memory
}

/**
 * 执行 Windows API 调用并在失败时附带 GetLastError。
 */
private fun checkWindows(operation: String, action: () -> Boolean) {
    if (!action()) error("$operation failed: ${Native.getLastError()}")
}

/**
 * 安全关闭 Windows HANDLE。
 */
private fun closeHandle(handle: WinNT.HANDLE?) {
    if (handle == null || WinBase.INVALID_HANDLE_VALUE == handle) return
    Kernel32.INSTANCE.CloseHandle(handle)
}

/**
 * Windows 管道分片写入大小。
 */
private const val CHUNK_SIZE: Int = 4096
/**
 * 官方协议固定使用 64 位 size_t 长度前缀。
 */
private const val SIZE_PREFIX_BYTES: Int = 8
/**
 * WaitForSingleObject 表示等待超时的返回值。
 */
private const val WAIT_TIMEOUT: Int = 0x00000102
/**
 * 关闭服务端时等待进程自然退出的超时时间。
 */
private const val CLOSE_TIMEOUT_MILLIS: Int = 3000
/**
 * 启动后观察子进程是否提前退出的窗口；命中时记录退出码用于定位管道握手失败。
 */
private const val EARLY_EXIT_WATCH_MILLIS: Int = 5000

/**
 * 非 Windows 平台动态库搜索路径变量名。
 */
internal fun unixDynamicLibraryPathVariable(osName: String): String =
    if (osName.contains("Mac", ignoreCase = true)) "DYLD_LIBRARY_PATH" else "LD_LIBRARY_PATH"

/** 官方 Windows 宏服务端 argv[4] 使用 cjc.exe 的完整路径。 */
internal fun cjcExecutable(sdkHome: String): File =
    File(sdkHome, "bin/cjc.exe").also { check(it.isFile) { "SDK cjc 不存在: $it" } }

/**
 * 非 Windows 平台需要加入动态库搜索路径的 SDK 目录。
 *
 * 覆盖 `<sdkHome>/runtime/lib/<platform>` 与 `<sdkHome>/lib/<platform>`；
 * 宏服务端 dlopen 宏库时依赖其中的仓颉运行时库。
 */
internal fun unixDynamicLibraryDirectories(sdkHome: String): List<String> =
    listOf(File(sdkHome, "runtime/lib"), File(sdkHome, "lib"))
        .filter(File::isDirectory)
        .flatMap { root -> root.listFiles()?.filter(File::isDirectory).orEmpty() }
        .map(File::getAbsolutePath)
        .distinct()
