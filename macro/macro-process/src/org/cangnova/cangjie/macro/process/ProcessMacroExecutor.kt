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

import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.cangnova.cangjie.macro.protocol.PipeTransport
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.withLock

/**
 * 基于外部进程 LSPMacroServer 的宏执行器
 *
 * 启动 `LSPMacroServer` 子进程，通过匿名管道 + FlatBuffers 帧协议通信。
 *
 * ## 生命周期
 *
 * ```
 * executor.loadLibraries(paths)  // 加载宏动态库
 * executor.execute(calls)        // 展开宏（可多次调用）
 * executor.reset()               // 重置服务端状态
 * executor.close()               // 关闭进程
 * ```
 *
 * ## 跨平台
 *
 * - **Unix/Linux/macOS**：通过 stdin/stdout 通信
 * - **Windows**：通过 ProcessBuilder 的 stdin/stdout 通信
 *
 * @param macroServerPath LSPMacroServer 可执行文件路径
 * @param enableParallel 是否启用并行宏展开模式
 */
class ProcessMacroExecutor(
    private val macroServerPath: String,
    private val enableParallel: Boolean = true,
) : MacroExecutor {

    private val logger = Logger.getLogger(ProcessMacroExecutor::class.java.name)

    private val lock = ReentrantLock()
    private var process: Process? = null
    private var transport: PipeTransport? = null
    private var loadedLibPaths: Set<String> = emptySet()

    override fun isAvailable(): Boolean {
        val file = File(macroServerPath)
        return file.exists() && file.canExecute()
    }

    override fun loadLibraries(libPaths: List<String>) = lock.withLock {
        if (libPaths.isEmpty()) return@withLock
        val newPaths = libPaths.toSet()
        if (newPaths == loadedLibPaths) return@withLock

        ensureStarted()

        val t = transport ?: error("传输层未就绪")
        t.send(MacroMsgCodec.buildDefLib(libPaths))
        t.receive() // 等待确认
        loadedLibPaths = newPaths
        logger.fine("加载宏动态库: $libPaths")
    }

    override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> = lock.withLock {
        if (calls.isEmpty()) return@withLock emptyList()

        ensureStarted()
        val t = transport ?: error("传输层未就绪")

        try {
            calls.map { callInfo ->
                // LSPMacroServer 协议要求每次只发送一条 MacroCall 并接收一条 MacroResult
                val payload = MacroMsgCodec.buildMultiMacroCalls(listOf(callInfo))
                t.send(payload)

                val response = t.receive()
                val msgType = MacroMsgCodec.getMsgType(response)
                if (msgType != MacroMsgCodec.TYPE_MACRO_RESULT) {
                    MacroExpansionResult.Failure("意外的消息类型: $msgType")
                } else {
                    MacroMsgCodec.parseMacroResult(response)
                }
            }
        } catch (e: IOException) {
            logger.log(Level.WARNING, "宏展开通信失败", e)
            calls.map { MacroExpansionResult.Failure(e.message ?: "通信失败") }
        }
    }

    override fun reset() = lock.withLock {
        val t = transport ?: return@withLock
        runCatching {
            t.send(MacroMsgCodec.buildResetStageTask())
        }.onFailure { e ->
            logger.log(Level.WARNING, "发送 ResetStage 失败", e)
        }
    }

    override fun close() {
        lock.withLock {
            val t = transport
            if (t != null) {
                runCatching { t.send(MacroMsgCodec.buildExitTask()) }
                t.close()
                transport = null
            }
            loadedLibPaths = emptySet()
        }

        val proc = process
        if (proc != null) {
            if (proc.isAlive) {
                proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                if (proc.isAlive) {
                    logger.warning("LSPMacroServer 未在 3 秒内退出，强制终止")
                    proc.destroyForcibly()
                }
            }
            process = null
        }
        logger.info("ProcessMacroExecutor 已关闭")
    }

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    /**
     * 确保进程已启动
     */
    private fun ensureStarted() {
        if (process?.isAlive == true && transport != null) return

        val executable = File(macroServerPath)
        check(executable.exists()) { "LSPMacroServer 不存在: $macroServerPath" }
        check(executable.canExecute()) { "LSPMacroServer 不可执行: $macroServerPath" }

        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        val command = buildList {
            add(executable.absolutePath)
            if (isWindows) {
                // Windows：使用 stdin/stdout（fd 0/1）
                add("0")
                add("1")
            } else {
                // Unix：使用 stdin/stdout（fd 0/1）
                add("0")
                add("1")
            }
            add(if (enableParallel) "1" else "0")
            add(executable.parent) // cjcFolder
            if (!isWindows) {
                add(ProcessHandle.current().pid().toString()) // ppid
            }
        }

        val pb = ProcessBuilder(command)
            .redirectErrorStream(false)
        // stdin/stdout 用于通信，stderr 用于日志

        val proc = pb.start()
        process = proc

        transport = PipeTransport(proc.inputStream, proc.outputStream)
        logger.info("LSPMacroServer 已启动: $macroServerPath")
    }
}
