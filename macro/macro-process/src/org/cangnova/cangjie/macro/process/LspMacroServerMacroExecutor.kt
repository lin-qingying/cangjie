/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.cangnova.cangjie.macro.process

import java.io.File
import java.util.logging.Logger

/** 宏执行器只编排协议；进程、管道和关闭观察统一归公共 LspMacroServerProcess。 */
class LspMacroServerMacroExecutor(
    private val macroServerPath: String,
    private val sdkHome: String,
    private val processEnvironment: Map<String, String>,
    private val enableParallel: Boolean = true,
) : ProcessMacroExecutor() {
    override val logger: Logger = Logger.getLogger(LspMacroServerMacroExecutor::class.java.name)

    override fun isBackendAvailable(): Boolean =
        File(macroServerPath).let { it.isFile && it.canExecute() }

    override fun startConnection(): ProcessMacroConnection {
        val process = LspMacroServerProcess.start(
            File(macroServerPath), sdkHome, processEnvironment, enableParallel, logger)
        return ProcessMacroConnection(
            transport = process.transport,
            isAlive = process::isAlive,
            beginClose = process::beginClose,
            finishCloseRequest = process::finishCloseRequest,
            close = process::close,
        )
    }
}
