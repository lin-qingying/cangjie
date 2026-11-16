package org.cangnova.cangjie.lsp.process

import java.nio.charset.StandardCharsets
import org.cangnova.cangjie.lsp.testkit.LspProcessHarness
import org.cangnova.cangjie.lsp.testkit.LspProcessSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 进程级传输与协议纯净性。
 *
 * 这些用例跑的是**独立 JVM**，因此覆盖的是进程内连接测不到的部分：
 * `LspIoManager` 的标准流隔离、`Content-Length` 的 UTF-8 字节语义、连续消息、
 * stdout 是否被日志污染，以及 shutdown/exit 的真实进程退出码。
 */
class CangjieLspProcessTransportTest {
    /**
     * stdio：初始化 → 打开含 CJK/emoji 的文档 → 后续请求仍能对齐应答。
     *
     * 非 ASCII 帧是这里的核心：若服务端把 `Content-Length` 当字符数，didOpen 帧会被
     * 截断或多吃字节，紧接着的 hover 请求就无法对齐解析，表现为永远等不到响应。
     */
    @Test
    fun `stdio initialize survives non ascii frames and keeps stdout protocol pure`() {
        LspProcessHarness.startStdio().use { server ->
            val session = server.session
            session.writeFrame(initializePayload(1))
            session.awaitResponse(1).assertContains("capabilities")

            val uri = "file:///workspace/unicode.cj"
            val text = "func 计算(): Int64 {\n    return 1  // 😀 中文\n}\n"
            assertTrue(
                text.toByteArray(StandardCharsets.UTF_8).size > text.length,
                "测试文本必须包含多字节字符，否则覆盖不到帧长度语义",
            )
            session.writeOpenDocument(uri, text)

            session.writeFrame(
                jsonRpcRequest(
                    id = 2,
                    method = "textDocument/hover",
                    params = """{"textDocument":{"uri":"$uri"},"position":{"line":0,"character":5}}""",
                ),
            )
            val hover = session.awaitResponse(2).assertContains("contractHover")
            assertDeclaredLengthIsUtf8Bytes(hover.body, hover.declaredLength)

            session.writeFrame(jsonRpcRequest(id = 3, method = "shutdown"))
            session.awaitResponse(3)
            session.writeFrame(jsonRpcNotification("exit"))

            assertEquals(0, session.awaitExit())
            session.awaitStdoutDrained()
            session.assertProtocolPureStdout()
        }
    }

    /**
     * stdio：一次写入多帧（initialize + shutdown），服务端必须逐帧解析并各自应答。
     *
     * 不断言响应先后：initialize 走串行语义执行器，shutdown 立即返回，JSON-RPC 允许响应乱序。
     * 也**不把 exit 塞进同一批**：exit 会立刻销毁环境，而 initialize 可能还在构造，
     * 那是客户端违约（真实客户端必然等 initialize 返回），不该在这里当成服务端缺陷。
     */
    @Test
    fun `consecutive frames in a single write are all answered`() {
        LspProcessHarness.startStdio().use { server ->
            val session = server.session
            session.writeFramesInOneWrite(
                listOf(
                    initializePayload(1),
                    jsonRpcRequest(id = 2, method = "shutdown"),
                ),
            )

            session.awaitResponse(1).assertContains("capabilities")
            session.awaitResponse(2).assertContains("\"result\":null")

            session.writeFrame(jsonRpcNotification("exit"))
            assertEquals(0, session.awaitExit(), "shutdown 之后的 exit 必须以 0 退出")
            session.awaitStdoutDrained()
            session.assertCleanClosure()
            session.assertProtocolPureStdout()
        }
    }

    /**
     * stdio：exit 前未收到 shutdown 时，进程退出码必须是 1。
     */
    @Test
    fun `exit without shutdown terminates with non zero code`() {
        LspProcessHarness.startStdio().use { server ->
            val session = server.session
            session.writeFrame(initializePayload(1))
            session.awaitResponse(1)
            session.writeFrame(jsonRpcNotification("exit"))

            assertEquals(1, session.awaitExit(), "未收到 shutdown 的 exit 必须报告异常退出码")
            session.awaitStdoutDrained()
            session.assertCleanClosure()
        }
    }

    /**
     * socket：当前服务端提供 TCP 传输，必须在连接上完成同样的初始化与退出流程。
     */
    @Test
    fun `socket transport serves initialize and shutdown`() {
        val port = LspProcessHarness.reserveFreePort()
        LspProcessHarness.startSocket(port).use { server ->
            val session = server.session
            session.writeFrame(initializePayload(1))
            session.awaitResponse(1).assertContains("Cangjie Language Server")

            session.writeFrame(jsonRpcRequest(id = 2, method = "shutdown"))
            session.awaitResponse(2)
            session.writeFrame(jsonRpcNotification("exit"))

            assertEquals(0, session.awaitExit())
            session.awaitStdoutDrained()
            // socket 由 System.exit 收尾，本端会看到 Connection reset，因此只校验帧结构。
            session.assertProtocolPureStdout()
        }
    }

    /**
     * 初始化请求：rootUri 保持 ASCII，把非 ASCII 内容留给文档正文，
     * 从而只让「文本帧」承担字节长度语义的验证。
     */
    private fun initializePayload(id: Int): String = """
        {"jsonrpc":"2.0","id":$id,"method":"initialize","params":{
          "processId":null,
          "rootUri":"file:///workspace",
          "capabilities":{},
          "workspaceFolders":[{"uri":"file:///workspace","name":"workspace"}]
        }}
    """.trimIndent()

    /**
     * 打开文档：文本按 JSON 字符串字面量写入，非 ASCII 保持原样（正是要验证的部分）。
     */
    private fun LspProcessSession.writeOpenDocument(
        uri: String,
        text: String,
    ) {
        writeFrame(
            """
            {"jsonrpc":"2.0","method":"textDocument/didOpen","params":{"textDocument":{
              "uri":"$uri","languageId":"cangjie","version":1,"text":${jsonString(text)}
            }}}
            """.trimIndent(),
        )
    }

    /** 断言帧声明长度就是它的 UTF-8 字节数而不是字符数。 */
    private fun assertDeclaredLengthIsUtf8Bytes(body: String, declaredLength: Int) {
        assertEquals(
            body.toByteArray(StandardCharsets.UTF_8).size,
            declaredLength,
            "Content-Length 必须是 UTF-8 字节数",
        )
    }

    /** 构造 JSON-RPC 请求帧。 */
    private fun jsonRpcRequest(id: Int, method: String, params: String = "{}"): String =
        """{"jsonrpc":"2.0","id":$id,"method":"$method","params":$params}"""

    /** 构造 JSON-RPC 通知帧。 */
    private fun jsonRpcNotification(method: String): String =
        """{"jsonrpc":"2.0","method":"$method"}"""

    /**
     * 把文本转成 JSON 字符串字面量。
     *
     * 只处理测试文本里会用到的转义（引号、反斜杠、换行、制表符），非 ASCII 保持原样。
     */
    private fun jsonString(text: String): String = buildString {
        append('"')
        text.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
