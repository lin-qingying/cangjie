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
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.logging.Logger

class ProcessMacroExecutorTest {
    @Test
    fun processMacroExecutorIsAbstractAndLspMacroServerExecutorInheritsIt() {
        assertTrue(Modifier.isAbstract(ProcessMacroExecutor::class.java.modifiers))
        assertTrue(ProcessMacroExecutor::class.java.isAssignableFrom(LspMacroServerMacroExecutor::class.java))
    }

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

    private class TestProcessMacroExecutor(
        responses: ArrayDeque<ByteArray>,
    ) : ProcessMacroExecutor() {
        override val logger: Logger = Logger.getLogger(TestProcessMacroExecutor::class.java.name)
        val transport = QueueMacroProcessTransport(responses)
        var closed: Boolean = false
        private var alive: Boolean = false

        override fun isBackendAvailable(): Boolean = true

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

    private class QueueMacroProcessTransport(
        private val responses: ArrayDeque<ByteArray>,
    ) : MacroProcessTransport {
        val sent: MutableList<ByteArray> = mutableListOf()

        override fun send(payload: ByteArray) {
            sent += payload
        }

        override fun receive(): ByteArray = responses.removeFirst()

        override fun close() = Unit
    }

    private fun ackPayload(): ByteArray = MacroMsgCodec.buildDefLib(emptyList())

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
