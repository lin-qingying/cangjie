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

import MacroMsgFormat.MacroMsg
import MacroMsgFormat.MacroResult
import MacroMsgFormat.Position
import MacroMsgFormat.Token
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExpansionFailureKind
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroLibraryLoadFailureKind
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.logging.Logger

/**
 * 覆盖进程宏执行器的协议编排、库加载缓存、连接关闭和 LSPMacroServer 可用性判断。
 */
class ProcessMacroExecutorTest {
    /**
     * 当前测试用例使用的临时目录，用于构造可执行文件探测场景。
     */
    @TempDir
    lateinit var tempDir: Path

    /**
     * 验证进程宏执行器保持抽象基类形态，并由 LSPMacroServer 执行器继承。
     */
    @Test
    fun processMacroExecutorIsAbstractAndLspMacroServerExecutorInheritsIt() {
        assertTrue(Modifier.isAbstract(ProcessMacroExecutor::class.java.modifiers))
        assertTrue(ProcessMacroExecutor::class.java.isAssignableFrom(LspMacroServerMacroExecutor::class.java))
    }

    /**
     * 验证 LSPMacroServer 执行器的可用性只由配置文件是否存在且可执行决定。
     */
    @Test
    fun lspMacroServerExecutorAvailabilityFollowsExecutableFileState() {
        val missing = LspMacroServerMacroExecutor(tempDir.resolve("missing-LSPMacroServer").toString())
        assertFalse(missing.isAvailable())

        val executableName = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            "LSPMacroServer.exe"
        } else {
            "LSPMacroServer"
        }
        val executable = tempDir.resolve(executableName)
        Files.write(executable, byteArrayOf(1, 2, 3))
        executable.toFile().setExecutable(true)

        val available = LspMacroServerMacroExecutor(executable.toString())
        assertEquals(executable.toFile().canExecute(), available.isAvailable())
    }

    /**
     * 验证抽象进程执行器负责发送 DefLib、发送 MacroCall 并解析返回 token。
     */
    @Test
    fun abstractProcessExecutorOwnsProtocolAndParsesExpansionTokens() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(
                listOf(
                    ackPayload(),
                    macroResultPayload(
                        TokenInfo(
                            kind = 1u,
                            value = "expanded",
                            begin = SourcePosition(fileId = 1, line = 2, column = 3),
                            end = SourcePosition(fileId = 1, line = 2, column = 11),
                        ),
                    ),
                ),
            ),
        )

        executor.loadLibraries(listOf("macro.so"))
        val result = executor.execute(
            listOf(
                MacroCallInfo(
                    idName = "demo",
                    methodName = "demo",
                    libPath = "macro.so",
                    argTokens = listOf(TokenInfo(kind = 1u, value = "arg")),
                ),
            ),
        ).single()

        assertTrue(result is MacroExpansionResult.Success)
        result as MacroExpansionResult.Success
        assertEquals(listOf("expanded"), result.tokens.map { it.value })
        assertEquals(
            listOf(MacroMsgCodec.TYPE_DEF_LIB, MacroMsgCodec.TYPE_MULTI_CALLS),
            executor.transport.sent.map(MacroMsgCodec::getMsgType),
        )
    }

    /**
     * 验证库加载请求会去重，并且已经成功加载的库不会重复发送给服务端。
     */
    @Test
    fun loadLibrariesDeduplicatesRequestsAndCachesLoadedLibraries() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf(ackPayload())),
        )

        val first = executor.loadLibraries(listOf("macro.so", "macro.so", "", "other.so"))
        val second = executor.loadLibraries(listOf("macro.so", "other.so"))

        assertTrue(first is MacroLibraryLoadResult.Success)
        first as MacroLibraryLoadResult.Success
        assertEquals(listOf("macro.so", "other.so"), first.loadedLibPaths)
        assertTrue(second is MacroLibraryLoadResult.Success)
        second as MacroLibraryLoadResult.Success
        assertEquals(listOf("macro.so", "other.so"), second.loadedLibPaths)
        assertEquals(1, executor.transport.sent.size)
        assertEquals(MacroMsgCodec.TYPE_DEF_LIB, MacroMsgCodec.getMsgType(executor.transport.sent.single()))
    }

    /**
     * 验证 FlatBuffers DefLib ack 中的失败路径会映射为无法打开动态库错误。
     */
    @Test
    fun loadLibrariesMapsDefLibAckFailedPathsToCannotOpenLibFailures() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf(MacroMsgCodec.buildDefLib(listOf("missing.so")))),
        )

        val result = executor.loadLibraries(listOf("missing.so"))

        assertTrue(result is MacroLibraryLoadResult.Failure)
        result as MacroLibraryLoadResult.Failure
        val failure = result.failures.single()
        assertEquals("missing.so", failure.libPath)
        assertEquals(MacroLibraryLoadFailureKind.CANNOT_OPEN_LIB, failure.kind)
    }

    /**
     * 验证官方文本 DefLib ack 能被兼容解析为动态库加载失败。
     */
    @Test
    fun loadLibrariesAcceptsOfficialTextDefLibAck() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf("RespondFindDef broken.dll".toByteArray())),
        )

        val result = executor.loadLibraries(listOf("broken.dll"))

        assertTrue(result is MacroLibraryLoadResult.Failure)
        result as MacroLibraryLoadResult.Failure
        val failure = result.failures.single()
        assertEquals("broken.dll", failure.libPath)
        assertEquals(MacroLibraryLoadFailureKind.CANNOT_OPEN_LIB, failure.kind)
    }

    /**
     * 验证库加载阶段收到非 DefLib ack 时会返回协议错误。
     */
    @Test
    fun loadLibrariesReportsProtocolErrorForUnexpectedAckType() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf(macroResultPayload(TokenInfo(kind = 1u, value = "not-deflib")))),
        )

        val result = executor.loadLibraries(listOf("macro.so"))

        assertTrue(result is MacroLibraryLoadResult.Failure)
        result as MacroLibraryLoadResult.Failure
        val failure = result.failures.single()
        assertEquals("macro.so", failure.libPath)
        assertEquals(MacroLibraryLoadFailureKind.PROTOCOL_ERROR, failure.kind)
    }

    /**
     * 验证宏展开阶段收到非 MacroResult 响应时会返回协议错误。
     */
    @Test
    fun executeReportsProtocolErrorForUnexpectedResponseType() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf(ackPayload())),
        )

        val result = executor.execute(
            listOf(MacroCallInfo(idName = "demo", methodName = "demo")),
        ).single()

        assertTrue(result is MacroExpansionResult.Failure)
        result as MacroExpansionResult.Failure
        assertEquals(MacroExpansionFailureKind.PROTOCOL_ERROR, result.kind)
    }

    /**
     * 验证 reset 会清空已加载库缓存，并向服务端发送 ResetStage 任务。
     */
    @Test
    fun resetClearsLoadedLibraryCacheAndSendsResetStageTask() {
        val executor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf(ackPayload(), ackPayload())),
        )

        executor.loadLibraries(listOf("macro.so"))
        executor.reset()
        executor.loadLibraries(listOf("macro.so"))

        assertEquals(
            listOf(
                MacroMsgCodec.TYPE_DEF_LIB,
                MacroMsgCodec.TYPE_EXIT_TASK,
                MacroMsgCodec.TYPE_DEF_LIB,
            ),
            executor.transport.sent.map(MacroMsgCodec::getMsgType),
        )
    }

    /**
     * 验证未启动连接时 close 不产生副作用，已启动连接时 close 会发送退出任务并关闭连接。
     */
    @Test
    fun closeSendsExitTaskAndClosesConnection() {
        val executor = TestProcessMacroExecutor(responses = ArrayDeque())

        executor.execute(emptyList())
        executor.close()

        assertFalse(executor.closed)
        assertTrue(executor.transport.sent.isEmpty())

        val startedExecutor = TestProcessMacroExecutor(
            responses = ArrayDeque(listOf(macroResultPayload(TokenInfo(kind = 1u, value = "ok")))),
        )
        startedExecutor.execute(listOf(MacroCallInfo(idName = "demo", methodName = "demo")))
        startedExecutor.close()

        assertTrue(startedExecutor.closed)
        assertEquals(MacroMsgCodec.TYPE_EXIT_TASK, MacroMsgCodec.getMsgType(startedExecutor.transport.sent.last()))
    }

    /**
     * 可控的进程宏执行器测试替身。
     */
    private class TestProcessMacroExecutor(
        /**
         * 传输层按顺序返回的服务端响应队列。
         */
        responses: ArrayDeque<ByteArray>,
    ) : ProcessMacroExecutor() {
        /**
         * 测试替身日志器。
         */
        override val logger: Logger = Logger.getLogger(TestProcessMacroExecutor::class.java.name)
        /**
         * 内存队列传输层，用于记录发送消息并返回预置响应。
         */
        val transport = QueueMacroProcessTransport(responses)
        /**
         * 记录后端连接是否已经执行关闭回调。
         */
        var closed: Boolean = false
        /**
         * 模拟后端连接存活状态。
         */
        private var alive: Boolean = false

        /**
         * 测试替身始终声明后端可用。
         */
        override fun isBackendAvailable(): Boolean = true

        /**
         * 建立基于内存队列传输层的测试连接。
         */
        override fun startConnection(): ProcessMacroConnection {
            alive = true
            return ProcessMacroConnection(
                transport = transport,
                isAlive = { alive },
                close = {
                    closed = true
                    alive = false
                },
            )
        }
    }

    /**
     * 使用内存队列模拟宏进程传输层。
     */
    private class QueueMacroProcessTransport(
        /**
         * 接收时依次弹出的预置响应。
         */
        private val responses: ArrayDeque<ByteArray>,
    ) : MacroProcessTransport {
        /**
         * 记录测试期间发送给服务端的所有消息。
         */
        val sent: MutableList<ByteArray> = mutableListOf()

        /**
         * 记录一条发送消息。
         */
        override fun send(payload: ByteArray) {
            sent += payload
        }

        /**
         * 返回下一条预置响应。
         */
        override fun receive(): ByteArray = responses.removeFirst()

        /**
         * 内存传输层无需释放外部资源。
         */
        override fun close() = Unit
    }

    /**
     * 构造成功的 DefLib ack 消息。
     */
    private fun ackPayload(): ByteArray = MacroMsgCodec.buildDefLib(emptyList())

    /**
     * 构造包含指定 token 的成功 MacroResult 消息。
     */
    private fun macroResultPayload(vararg tokens: TokenInfo): ByteArray {
        val builder = FlatBufferBuilder(256)
        val tokenOffsets = tokens.map { token -> buildToken(builder, token) }.toIntArray()
        val tokensVector = MacroResult.createTksVector(builder, tokenOffsets)
        val result = MacroResult.createMacroResult(
            builder,
            0,
            MacroMsgCodec.STATUS_SUCCESS,
            tokensVector,
            0,
            0,
            0,
        )
        val message = MacroMsg.createMacroMsg(builder, MacroMsgCodec.TYPE_MACRO_RESULT, result)
        builder.finish(message)
        return builder.sizedByteArray()
    }

    /**
     * 将测试 token 写入 FlatBuffers builder，并返回 token 偏移。
     */
    private fun buildToken(builder: FlatBufferBuilder, token: TokenInfo): Int {
        val value = builder.createString(token.value)
        Token.startToken(builder)
        Token.addKind(builder, token.kind)
        Token.addValue(builder, value)
        Token.addBegin(
            builder,
            Position.createPosition(builder, token.begin.fileId.toUInt(), token.begin.line, token.begin.column),
        )
        Token.addEnd(
            builder,
            Position.createPosition(builder, token.end.fileId.toUInt(), token.end.line, token.end.column),
        )
        Token.addDelimiterNum(builder, token.delimiterNum.toUInt())
        return Token.endToken(builder)
    }
}
