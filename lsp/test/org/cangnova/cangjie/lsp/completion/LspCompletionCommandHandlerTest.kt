package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextEdit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

/**
 * `applyCompletion` 命令的服务端契约（设计 §7.11.3/§7.11.4）。
 *
 * 两端分工是「服务端算编辑 + 客户端落盘」，因此这里断言的是**服务端发出的编辑内容**
 * 与**失败时的明确错误**，而不是服务端的写事务（服务端根本不写文档）。
 *
 * 用真实 LSP 连接（JSON-RPC 双端）而不是直接调处理器：要验证的正是
 * 「命令被正确路由、能力被正确声明」这两条协议层事实。
 */
class LspCompletionCommandHandlerTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 接受候选：服务端发出一条 `workspace/applyEdit`，其中的编辑应用到原文后得到预期文本。
     */
    @Test
    fun applyCompletionSendsApplyEditWithComputedEdits() {
        withConnection { connection, uri, text ->
            val caret = text.indexOf(PARTIAL) + PARTIAL.length
            val session = connection.server.context.completionSessions.create(
                uri = uri,
                version = DOCUMENT_VERSION,
                replaceStart = caret - PARTIAL.length,
                replaceEnd = caret,
                items = listOf(candidate("buildUser", callable = true)),
            )

            connection.executeCommand(applyCommand(session.key, index = 0))

            val applied = connection.appliedEdits()
            assertEquals(1, applied.size, "接受候选必须恰好发出一条 applyEdit，实际 ${applied.size}")
            val edits = applied.single().edit.changes[uri].orEmpty()
            assertTrue(edits.isNotEmpty(), "applyEdit 必须携带编辑")

            val updated = applyEdits(text, edits)
            assertTrue(updated.contains("buildUser()"), "括号形态按共享插入策略补齐，实际 $updated")
            assertTrue(updated.contains("func consume()"), "既有声明不得被改写，实际 $updated")
        }
    }

    /**
     * 会话过期（键不存在或被淘汰）→ 明确错误，且**不发** applyEdit。
     */
    @Test
    fun expiredSessionFailsWithoutSendingEdits() {
        withConnection { connection, _, _ ->
            val failure = assertThrows(ExecutionException::class.java) {
                connection.executeCommand(applyCommand(sessionKey = "cj-completion-9999", index = 0))
            }

            assertTrue(
                failure.cause?.message?.contains("已过期") == true,
                "过期必须报「重新触发补全」这类明确错误，实际 ${failure.cause?.message}",
            )
            assertTrue(connection.appliedEdits().isEmpty(), "过期会话不得发 applyEdit")
        }
    }

    /**
     * 候选下标越界 → 明确错误（客户端可能回传过时下标）。
     */
    @Test
    fun outOfRangeIndexFails() {
        withConnection { connection, uri, text ->
            val caret = text.indexOf(PARTIAL) + PARTIAL.length
            val session = connection.server.context.completionSessions.create(
                uri = uri,
                version = DOCUMENT_VERSION,
                replaceStart = caret - PARTIAL.length,
                replaceEnd = caret,
                items = listOf(candidate("buildUser")),
            )

            val failure = assertThrows(ExecutionException::class.java) {
                connection.executeCommand(applyCommand(session.key, index = 7))
            }

            assertTrue(
                failure.cause?.message?.contains("没有第 7 个候选") == true,
                "越界下标必须明确报错，实际 ${failure.cause?.message}",
            )
            assertTrue(connection.appliedEdits().isEmpty(), "越界不得发 applyEdit")
        }
    }

    /**
     * 文档变更 → 该文档的会话被丢弃，命令报「已过期」且不发 applyEdit。
     *
     * 这是**第一道**防线（在源头丢弃）：`didChange` 时就失效，不必等到应用时才发现。
     */
    @Test
    fun changedDocumentDropsSession() {
        withConnection { connection, uri, text ->
            val caret = text.indexOf(PARTIAL) + PARTIAL.length
            val session = connection.server.context.completionSessions.create(
                uri = uri,
                version = DOCUMENT_VERSION,
                replaceStart = caret - PARTIAL.length,
                replaceEnd = caret,
                items = listOf(candidate("buildUser")),
            )
            connection.changeDocument(uri, text.replace("return 1", "return 2"), DOCUMENT_VERSION + 1)

            val failure = assertThrows(ExecutionException::class.java) {
                connection.executeCommand(applyCommand(session.key, index = 0))
            }

            assertTrue(
                failure.cause?.message?.contains("已过期") == true,
                "文档变更后会话必须失效，实际 ${failure.cause?.message}",
            )
            assertTrue(connection.appliedEdits().isEmpty(), "失效会话不得发 applyEdit")
        }
    }

    /**
     * **第二道**防线：会话仍在但版本已变（例如失效回调没走到）时，按版本拒绝写入。
     *
     * 这里刻意不走 `didChange`：直接造一个版本号对不上的会话，验证命令侧的版本复验，
     * 而不是验证失效回调。
     */
    @Test
    fun versionMismatchIsRejectedEvenIfSessionSurvives() {
        withConnection { connection, uri, text ->
            val caret = text.indexOf(PARTIAL) + PARTIAL.length
            val session = connection.server.context.completionSessions.create(
                uri = uri,
                version = DOCUMENT_VERSION + 99,
                replaceStart = caret - PARTIAL.length,
                replaceEnd = caret,
                items = listOf(candidate("buildUser")),
            )

            val failure = assertThrows(ExecutionException::class.java) {
                connection.executeCommand(applyCommand(session.key, index = 0))
            }

            assertTrue(
                failure.cause?.message?.contains("文档已变") == true,
                "版本不一致必须拒绝写入，实际 ${failure.cause?.message}",
            )
            assertTrue(connection.appliedEdits().isEmpty(), "版本不一致不得发 applyEdit")
        }
    }

    /**
     * 能力声明：命令必须出现在 `executeCommandProvider` 里。
     *
     * 注意**不断言**服务端侧的 `applyEdit`：LSP 的服务端能力里根本没有这个字段
     * （核对 lsp4j 1.0.0 的 `WorkspaceServerCapabilities`：只有 workspaceFolders/
     * fileOperations/textDocumentContent），该能力只在**客户端**侧声明，服务端据此决定发不发请求。
     */
    @Test
    fun declaresApplyCommand() {
        val fixture = fixture()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            val result = connection.initialize(fixture.initializeParams())
            connection.initialized()

            assertTrue(
                result.capabilities.executeCommandProvider?.commands?.contains(LspCompletionCommands.APPLY_COMPLETION) == true,
                "必须声明 applyCompletion 命令，否则客户端接受候选后不会回调服务端，实际 ${result.capabilities.executeCommandProvider}",
            )
        }
    }

    /**
     * 客户端没声明 `workspace/applyEdit` → 明确失败，而不是发一个没人处理的请求。
     */
    @Test
    fun applyCompletionRequiresClientApplyEditSupport() {
        val fixture = fixture()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            // 最小客户端能力：不含 workspace.applyEdit。
            connection.initialize(fixture.initializeParams(capabilities = LspClientCapabilitiesBuilder.core()))
            connection.initialized()
            val uri = fixture.uri(USE_FILE)
            val text = fixture.text(USE_FILE)
            connection.openDocument(uri, text)
            val caret = text.indexOf(PARTIAL) + PARTIAL.length
            val session = connection.server.context.completionSessions.create(
                uri = uri,
                version = DOCUMENT_VERSION,
                replaceStart = caret - PARTIAL.length,
                replaceEnd = caret,
                items = listOf(candidate("buildUser")),
            )

            val failure = assertThrows(ExecutionException::class.java) {
                connection.executeCommand(applyCommand(session.key, index = 0))
            }

            assertTrue(
                failure.cause?.message?.contains("workspace/applyEdit") == true,
                "客户端不支持时必须明确报错，实际 ${failure.cause?.message}",
            )
            assertTrue(connection.appliedEdits().isEmpty(), "不支持时不得发 applyEdit")
        }
    }

    /**
     * 在默认夹具上打开文档并执行断言。
     */
    private fun withConnection(body: (LspIntegrationTestConnection, String, String) -> Unit) {
        val fixture = fixture()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val uri = fixture.uri(USE_FILE)
            val text = fixture.text(USE_FILE)
            connection.openDocument(uri, text)
            body(connection, uri, text)
        }
    }

    /**
     * 构造夹具。
     */
    private fun fixture() = LspWorkspaceFixtureBuilder()
        .source(USE_NAME, USE_SOURCE)
        .build()

    /**
     * `applyCompletion` 命令参数。
     */
    private fun applyCommand(sessionKey: String, index: Int): ExecuteCommandParams =
        ExecuteCommandParams(LspCompletionCommands.APPLY_COMPLETION, listOf(sessionKey, index))

    /**
     * 测试候选。
     */
    private fun candidate(name: String, callable: Boolean = false): CompletionSnapshot = completionSnapshot(
        lookupString = name,
        kind = CompletionItemKind.TOP_LEVEL,
        origin = CompletionOrigin.TOP_LEVEL,
        tailText = if (callable) "(): Int64" else "",
        insertParenthesis = callable,
    ).build()

    /**
     * 把编辑应用到原文。
     */
    private fun applyEdits(text: String, edits: List<TextEdit>): String {
        val out = StringBuilder()
        var cursor = 0
        edits.sortedBy { offsetOf(text, it.range.start) }.forEach { edit ->
            val start = offsetOf(text, edit.range.start)
            val end = offsetOf(text, edit.range.end)
            out.append(text, cursor, start).append(edit.newText)
            cursor = end
        }
        out.append(text, cursor, text.length)
        return out.toString()
    }

    /**
     * 位置 → 偏移（夹具源码只用 `\n`）。
     */
    private fun offsetOf(text: String, position: Position): Int {
        var line = 0
        var offset = 0
        while (line < position.line && offset < text.length) {
            val newline = text.indexOf('\n', offset)
            if (newline < 0) break
            offset = newline + 1
            line++
        }
        return offset + position.character
    }

    private companion object {
        /** 文档打开时的版本。 */
        const val DOCUMENT_VERSION = 1

        /** 半截标识符。 */
        const val PARTIAL = "buil"

        const val USE_NAME = "use.cj"
        const val USE_FILE = "src/use.cj"

        /** 触发补全的文件：`let u = buil` 是半截标识符。 */
        val USE_SOURCE = """
            package sample.command

            func consume(): Int64 {
                let u = buil
                return 1
            }
        """.trimIndent()
    }
}
