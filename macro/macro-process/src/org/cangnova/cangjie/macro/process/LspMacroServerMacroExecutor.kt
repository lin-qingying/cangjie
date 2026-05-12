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

import org.cangnova.cangjie.macro.protocol.PipeTransport
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * 基于外部 `LSPMacroServer` 进程的宏执行器。
 *
 * [ProcessMacroExecutor] 负责通用宏执行协议，本类只负责 LSPMacroServer
 * 的可执行文件校验、启动参数、进程生命周期和 stdin/stdout 管道绑定。
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
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        return buildList {
            add(executable.absolutePath)
            add("0")
            add("1")
            add(if (enableParallel) "1" else "0")
            add(executable.parent)
            if (!isWindows) {
                add(ProcessHandle.current().pid().toString())
            }
        }
    }

    private fun closeProcess(process: Process) {
        if (!process.isAlive) return
        process.waitFor(3, TimeUnit.SECONDS)
        if (process.isAlive) {
            logger.warning("LSPMacroServer 未在 3 秒内退出，强制终止")
            process.destroyForcibly()
        }
    }
}
