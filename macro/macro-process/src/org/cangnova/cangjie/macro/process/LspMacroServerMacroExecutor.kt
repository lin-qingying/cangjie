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
import java.util.logging.Logger

/**
 * 基于外部 `LSPMacroServer` 进程的宏执行器。
 *
 * [ProcessMacroExecutor] 负责通用宏执行协议，本类只负责 LSPMacroServer
 * 的可执行文件校验、启动参数、进程生命周期和官方管道绑定。
 *
 * @param macroServerPath LSPMacroServer 可执行文件路径。
 * @param enableParallel 是否启用服务端并行宏展开模式。
 */
class LspMacroServerMacroExecutor(
    private val macroServerPath: String,
    private val enableParallel: Boolean = true,
) : ProcessMacroExecutor() {
    override val logger: Logger = Logger.getLogger(LspMacroServerMacroExecutor::class.java.name)

    override fun isBackendAvailable(): Boolean {
        val executable = File(macroServerPath)
        return executable.exists() && executable.canExecute()
    }

    override fun startConnection(): ProcessMacroConnection {
        val executable = File(macroServerPath)
        check(executable.exists()) { "LSPMacroServer 不存在: $macroServerPath" }
        check(executable.canExecute()) { "LSPMacroServer 不可执行: $macroServerPath" }
        if (isWindows()) {
            return startWindowsConnection(executable)
        }

        val command = buildCommand(executable)
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        logger.info("LSPMacroServer 已启动: $macroServerPath")
        return ProcessMacroConnection(
            transport = PipeMacroProcessTransport(PipeTransport(process.inputStream, process.outputStream)),
            isAlive = { process.isAlive },
            close = { closeProcess(process) },
        )
    }

    private fun buildCommand(executable: File): List<String> {
        return buildList {
            add(executable.absolutePath)
            add("0")
            add("1")
            add(if (enableParallel) "1" else "0")
            add(executable.parent)
            if (!isWindows()) {
                add(ProcessHandle.current().pid().toString())
            }
        }
    }

    /**
     * 官方 LSPMacroServer 不走 stdin/stdout；Windows 版本要求 argv 传入
     * 两个可继承匿名管道 HANDLE：server read 与 server write。
     */
    private fun startWindowsConnection(executable: File): ProcessMacroConnection {
        val connection = WindowsLspMacroServerConnection.start(
            executable = executable,
            enableParallel = enableParallel,
            logger = logger,
        )
        logger.info("LSPMacroServer 已启动: $macroServerPath")
        return ProcessMacroConnection(
            transport = connection.transport,
            isAlive = connection::isAlive,
            close = connection::close,
        )
    }

    private fun closeProcess(process: Process) {
        if (!process.isAlive) return
        process.waitFor(3, TimeUnit.SECONDS)
        if (process.isAlive) {
            logger.warning("LSPMacroServer 未在 3 秒内退出，强制终止")
            process.destroyForcibly()
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").contains("Windows", ignoreCase = true)
}

private class WindowsLspMacroServerConnection(
    val transport: WindowsPipeMacroProcessTransport,
    private val processInfo: WinBase.PROCESS_INFORMATION,
    private val logger: Logger,
) {
    fun isAlive(): Boolean =
        Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, 0) == WAIT_TIMEOUT

    fun close() {
        transport.close()
        if (isAlive()) {
            val waitResult = Kernel32.INSTANCE.WaitForSingleObject(processInfo.hProcess, CLOSE_TIMEOUT_MILLIS)
            if (waitResult == WAIT_TIMEOUT) {
                logger.warning("LSPMacroServer 未在 3 秒内退出，强制终止")
                Kernel32.INSTANCE.TerminateProcess(processInfo.hProcess, 1)
            }
        }
        Kernel32.INSTANCE.CloseHandle(processInfo.hThread)
        Kernel32.INSTANCE.CloseHandle(processInfo.hProcess)
    }

    companion object {
        fun start(
            executable: File,
            enableParallel: Boolean,
            logger: Logger,
        ): WindowsLspMacroServerConnection {
            val pipeAttributes = WinBase.SECURITY_ATTRIBUTES().apply {
                dwLength = DWORD(size().toLong())
                bInheritHandle = true
            }

            val parentReadRef = WinNT.HANDLEByReference()
            val childWriteRef = WinNT.HANDLEByReference()
            val childReadRef = WinNT.HANDLEByReference()
            val parentWriteRef = WinNT.HANDLEByReference()

            checkWindows("CreatePipe(parentRead, childWrite)") {
                Kernel32.INSTANCE.CreatePipe(parentReadRef, childWriteRef, pipeAttributes, 0)
            }
            try {
                checkWindows("CreatePipe(childRead, parentWrite)") {
                    Kernel32.INSTANCE.CreatePipe(childReadRef, parentWriteRef, pipeAttributes, 0)
                }
            } catch (error: Throwable) {
                closeHandle(parentReadRef.value)
                closeHandle(childWriteRef.value)
                throw error
            }

            val startupInfo = WinBase.STARTUPINFO().apply {
                cb = DWORD(size().toLong())
            }
            val processInfo = WinBase.PROCESS_INFORMATION()
            val environment = buildWindowsEnvironment(executable)
            val commandLine = buildCommandLine(
                executable = executable,
                childRead = childReadRef.value,
                childWrite = childWriteRef.value,
                enableParallel = enableParallel,
            )
            val created = Kernel32.INSTANCE.CreateProcessW(
                executable.absolutePath,
                commandLine.toCharArrayWithTerminator(),
                null,
                null,
                true,
                DWORD(CREATE_UNICODE_ENVIRONMENT.toLong()),
                environment,
                executable.parent,
                startupInfo,
                processInfo,
            )
            if (!created) {
                closeHandle(parentReadRef.value)
                closeHandle(childWriteRef.value)
                closeHandle(childReadRef.value)
                closeHandle(parentWriteRef.value)
                error("CreateProcess(LSPMacroServer) failed: ${Native.getLastError()}")
            }

            closeHandle(childReadRef.value)
            closeHandle(childWriteRef.value)

            logger.fine("LSPMacroServer Windows pipes established.")
            return WindowsLspMacroServerConnection(
                transport = WindowsPipeMacroProcessTransport(
                    readHandle = parentReadRef.value,
                    writeHandle = parentWriteRef.value,
                ),
                processInfo = processInfo,
                logger = logger,
            )
        }

        private fun buildCommandLine(
            executable: File,
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
            append(quoteCommandArgument(executable.parent))
        }

        private fun handleValue(handle: WinNT.HANDLE): String =
            Pointer.nativeValue(handle.pointer).toString()

        private fun buildWindowsEnvironment(executable: File): Pointer {
            val sdkHome = executable.parentFile?.parentFile?.parentFile
            val pathEntries = buildList {
                if (sdkHome != null) {
                    add(File(sdkHome, "runtime/lib/windows_x86_64_cjnative").path)
                    add(File(sdkHome, "third_party/llvm/bin").path)
                    add(File(sdkHome, "tools/bin").path)
                    add(File(sdkHome, "bin").path)
                }
            }
            val environment = linkedMapOf<String, String>()
            for ((key, value) in System.getenv()) {
                environment[key] = value
            }
            if (sdkHome != null) {
                environment["CANGJIE_HOME"] = sdkHome.path
            }
            val currentPathKey = environment.keys.firstOrNull { it.equals("PATH", ignoreCase = true) } ?: "PATH"
            val currentPath = environment[currentPathKey].orEmpty()
            environment[currentPathKey] = (pathEntries + currentPath)
                .filter(String::isNotBlank)
                .joinToString(separator = File.pathSeparator)

            val block = environment.entries
                .sortedBy { it.key.uppercase() }
                .joinToString(separator = "\u0000", postfix = "\u0000\u0000") { (key, value) -> "$key=$value" }
            return block.toWideCharMemory()
        }
    }
}

private class WindowsPipeMacroProcessTransport(
    private val readHandle: WinNT.HANDLE,
    private val writeHandle: WinNT.HANDLE,
) : MacroProcessTransport {
    private var closed: Boolean = false

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

    override fun receive(): ByteArray {
        val lenBuf = ByteArray(SIZE_PREFIX_BYTES)
        readFully(lenBuf)
        val length = ByteBuffer.wrap(lenBuf).order(ByteOrder.LITTLE_ENDIAN).getLong().toInt()
        if (length <= 0) throw IOException("LSPMacroServer 返回非法消息长度: $length")

        val payload = ByteArray(length)
        readFully(payload)
        return payload
    }

    override fun close() {
        if (closed) return
        closed = true
        closeHandle(readHandle)
        closeHandle(writeHandle)
    }

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

private fun quoteCommandArgument(value: String): String =
    "\"" + value.replace("\"", "\\\"") + "\""

private fun String.toCharArrayWithTerminator(): CharArray =
    (this + "\u0000").toCharArray()

private fun String.toWideCharMemory(): Memory {
    val memory = Memory(((length + 1) * Native.WCHAR_SIZE).toLong())
    memory.clear()
    memory.setWideString(0, this)
    return memory
}

private fun checkWindows(operation: String, action: () -> Boolean) {
    if (!action()) error("$operation failed: ${Native.getLastError()}")
}

private fun closeHandle(handle: WinNT.HANDLE?) {
    if (handle == null || WinBase.INVALID_HANDLE_VALUE == handle) return
    Kernel32.INSTANCE.CloseHandle(handle)
}

private const val CHUNK_SIZE: Int = 4096
private const val SIZE_PREFIX_BYTES: Int = 8
private const val CREATE_UNICODE_ENVIRONMENT: Int = 0x00000400
private const val WAIT_TIMEOUT: Int = 0x00000102
private const val CLOSE_TIMEOUT_MILLIS: Int = 3000
