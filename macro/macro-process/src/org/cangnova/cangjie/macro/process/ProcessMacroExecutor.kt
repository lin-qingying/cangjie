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
import org.cangnova.cangjie.macro.MacroExpansionFailureKind
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroLibraryLoadFailure
import org.cangnova.cangjie.macro.MacroLibraryLoadFailureKind
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.cangnova.cangjie.macro.protocol.PipeTransport
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.concurrent.withLock

/**
 * 外部进程宏执行器抽象基类。
 *
 * 该类只负责 [MacroExecutor] 协议编排：加载动态库、逐个发送宏调用、
 * 解析展开结果、reset 和关闭握手。具体后端如何启动进程、如何建立管道，
 * 由子类通过 [startConnection] 实现。
 *
 * 重要：这里不绑定 LSPMacroServer。LSPMacroServer 是一个具体进程后端，
 * 由 [LspMacroServerMacroExecutor] 继承本类提供。
 */
abstract class ProcessMacroExecutor : MacroExecutor {

    protected abstract val logger: java.util.logging.Logger

    private val lock = ReentrantLock()
    private var connection: ProcessMacroConnection? = null
    private var loadedLibPaths: Set<String> = emptySet()

    override fun isAvailable(): Boolean = isBackendAvailable()

    override fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult = lock.withLock {
        val requested = libPaths.filter(String::isNotBlank).distinct()
        if (requested.isEmpty()) return@withLock MacroLibraryLoadResult.Success(emptyList())
        val unloaded = requested.filterNot { it in loadedLibPaths }
        if (unloaded.isEmpty()) return@withLock MacroLibraryLoadResult.Success(requested)

        try {
            ensureStarted()

            val t = connection?.transport ?: return@withLock MacroLibraryLoadResult.Failure(
                listOf(unloaded.protocolFailure("宏执行器传输层未就绪")),
            )
            t.send(MacroMsgCodec.buildDefLib(unloaded))
            val ack = parseDefLibAck(t.receive(), unloaded)
            if (ack is MacroLibraryLoadResult.Success) {
                loadedLibPaths = loadedLibPaths + unloaded
                logger.fine("加载宏动态库: $unloaded")
            }
            ack
        } catch (e: IOException) {
            logger.log(Level.WARNING, "宏动态库加载通信失败", e)
            MacroLibraryLoadResult.Failure(
                listOf(
                    MacroLibraryLoadFailure(
                        libPath = unloaded.joinToString(";"),
                        kind = MacroLibraryLoadFailureKind.SERVER_DISCONNECTED,
                        message = e.message ?: "宏执行器连接已断开",
                    )
                )
            )
        }
    }

    override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> = lock.withLock {
        if (calls.isEmpty()) return@withLock emptyList()

        ensureStarted()
        val t = connection?.transport ?: error("传输层未就绪")

        try {
            calls.map { callInfo ->
                // 外部宏执行协议要求每次只发送一条 MacroCall 并接收一条 MacroResult。
                val payload = MacroMsgCodec.buildMultiMacroCalls(listOf(callInfo))
                t.send(payload)

                val response = t.receive()
                val msgType = runCatching { MacroMsgCodec.getMsgType(response) }.getOrElse { error ->
                    return@map MacroExpansionResult.Failure(
                        message = error.message ?: "宏执行器返回了无法解析的协议消息。",
                        kind = MacroExpansionFailureKind.PROTOCOL_ERROR,
                    )
                }
                if (msgType != MacroMsgCodec.TYPE_MACRO_RESULT) {
                    MacroExpansionResult.Failure(
                        message = "意外的消息类型: $msgType",
                        kind = MacroExpansionFailureKind.PROTOCOL_ERROR,
                    )
                } else {
                    MacroMsgCodec.parseMacroResult(response)
                }
            }
        } catch (e: IOException) {
            logger.log(Level.WARNING, "宏展开通信失败", e)
            calls.map {
                MacroExpansionResult.Failure(
                    message = e.message ?: "通信失败",
                    kind = MacroExpansionFailureKind.SERVER_DISCONNECTED,
                )
            }
        }
    }

    override fun reset() = lock.withLock {
        val t = connection?.transport ?: return@withLock
        runCatching {
            t.send(MacroMsgCodec.buildResetStageTask())
        }.onFailure { e ->
            logger.log(Level.WARNING, "发送 ResetStage 失败", e)
        }
        loadedLibPaths = emptySet()
    }

    override fun close() {
        val closingConnection = lock.withLock {
            val current = connection
            if (current != null) {
                runCatching { current.transport.send(MacroMsgCodec.buildExitTask()) }
                current.transport.close()
                connection = null
            }
            loadedLibPaths = emptySet()
            current
        }

        closingConnection?.close()
        logger.info("${this::class.simpleName} 已关闭")
    }

    protected abstract fun isBackendAvailable(): Boolean

    protected abstract fun startConnection(): ProcessMacroConnection

    private fun ensureStarted() {
        if (connection?.isAlive() == true) return
        connection = startConnection()
    }

    private fun parseDefLibAck(response: ByteArray, requested: List<String>): MacroLibraryLoadResult {
        parseTextDefLibAck(response, requested)?.let { return it }

        val msgType = runCatching { MacroMsgCodec.getMsgType(response) }.getOrElse { error ->
            return MacroLibraryLoadResult.Failure(
                listOf(requested.protocolFailure(error.message ?: "无法解析 DefLib ack。")),
            )
        }
        if (msgType != MacroMsgCodec.TYPE_DEF_LIB) {
            return MacroLibraryLoadResult.Failure(
                listOf(requested.protocolFailure("DefLib ack 消息类型错误: $msgType")),
            )
        }

        val failedPaths = runCatching { MacroMsgCodec.parseDefLibPaths(response) }.getOrElse { error ->
            return MacroLibraryLoadResult.Failure(
                listOf(requested.protocolFailure(error.message ?: "无法解析 DefLib ack 内容。")),
            )
        }
        if (failedPaths.isEmpty()) return MacroLibraryLoadResult.Success(requested)
        return MacroLibraryLoadResult.Failure(
            failedPaths.map { path ->
                MacroLibraryLoadFailure(
                    libPath = path,
                    kind = MacroLibraryLoadFailureKind.CANNOT_OPEN_LIB,
                    message = "宏执行器无法打开动态库: $path",
                )
            }
        )
    }

    private fun parseTextDefLibAck(response: ByteArray, requested: List<String>): MacroLibraryLoadResult? {
        val text = response.toString(StandardCharsets.UTF_8).trimEnd('\u0000', '\r', '\n')
        if (!text.startsWith(RESPOND_FIND_DEF)) return null
        val failedPath = text.removePrefix(RESPOND_FIND_DEF).trim()
        return if (failedPath.isBlank()) {
            MacroLibraryLoadResult.Success(requested)
        } else {
            MacroLibraryLoadResult.Failure(
                listOf(
                    MacroLibraryLoadFailure(
                        libPath = failedPath,
                        kind = MacroLibraryLoadFailureKind.CANNOT_OPEN_LIB,
                        message = "宏执行器无法打开动态库: $failedPath",
                    )
                )
            )
        }
    }

    private fun List<String>.protocolFailure(message: String): MacroLibraryLoadFailure =
        MacroLibraryLoadFailure(
            libPath = joinToString(";"),
            kind = MacroLibraryLoadFailureKind.PROTOCOL_ERROR,
            message = message,
        )

    companion object {
        private const val RESPOND_FIND_DEF: String = "RespondFindDef "
    }

    /**
     * 已建立的进程通信连接。
     *
     * [transport] 负责帧协议读写，[isAlive] / [close] 由具体后端绑定到
     * 真实进程、守护服务或测试内存连接。
     */
    protected class ProcessMacroConnection(
        val transport: MacroProcessTransport,
        private val isAlive: () -> Boolean,
        private val close: () -> Unit,
    ) {
        fun isAlive(): Boolean = isAlive.invoke()
        fun close() = close.invoke()
    }
}

/**
 * 外部进程宏通信传输抽象。
 */
interface MacroProcessTransport : AutoCloseable {
    fun send(payload: ByteArray)
    fun receive(): ByteArray
}

/**
 * [PipeTransport] 到 [MacroProcessTransport] 的适配。
 */
class PipeMacroProcessTransport(
    private val delegate: PipeTransport,
) : MacroProcessTransport {
    override fun send(payload: ByteArray) = delegate.send(payload)

    override fun receive(): ByteArray = delegate.receive()

    override fun close() = delegate.close()
}
