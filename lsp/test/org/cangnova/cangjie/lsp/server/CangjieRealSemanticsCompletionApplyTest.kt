package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.completion.LspCompletionCommands
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspEditApplicator
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 「接受候选」整链：`textDocument/completion` → `workspace/executeCommand(applyCompletion)` → `workspace/applyEdit`。
 *
 * 这是编辑器里最常见的一次交互，也是分层最多的一条链：
 * 候选携带会话键、命令回到服务端、服务端按**当前**文档重新算编辑、最后经 `applyEdit` 交给客户端落盘。
 * 任何一段断掉，用户看到的现象都是「点了候选没反应」——而单元测试各自都是绿的。
 */
class CangjieRealSemanticsCompletionApplyTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例自带工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 接受候选必须真的插入文本。
     */
    @Test
    fun `accepting a candidate applies an edit that inserts it`() {
        completionWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FILE)
                val text = fixture.text(FILE)
                session.openDocument(uri, text)

                val item = requireCandidate(session, uri, text, "twice")
                val command = item.command
                assertNotNull(
                    command,
                    "候选必须带 applyCompletion 命令，否则客户端接受候选后不会回调服务端：$item",
                )
                assertEquals(LspCompletionCommands.APPLY_COMPLETION, command!!.command)

                val appliedBefore = session.appliedEdits().size
                session.executeCommand(ExecuteCommandParams(command.command, command.arguments))

                val applied = session.appliedEdits().drop(appliedBefore)
                assertEquals(1, applied.size, "接受候选必须发出恰好一次 applyEdit，实际 $applied")

                val result = LspEditApplicator.apply(applied.single().edit, mapOf(uri to text))
                val updated = result[uri].orEmpty()
                assertTrue(
                    updated.contains("return twice"),
                    "应用编辑后必须真的出现 `return twice`，实际文本：\n$updated",
                )
                assertFalse(
                    updated.contains("return twi\n") || updated.endsWith("return twi"),
                    "未完成的 `twi` 必须被替换掉，实际文本：\n$updated",
                )
            }
        }
    }

    /**
     * 会话键失效时必须返回错误，不能静默「成功但什么都没做」。
     *
     * 客户端只会把错误提示给用户；静默成功会让用户以为已经插入，继续敲代码，
     * 最后得到一段看起来「莫名其妙没补全」的代码。
     */
    @Test
    fun `applying with an unknown session key reports a protocol error`() {
        completionWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FILE)
                val text = fixture.text(FILE)
                session.openDocument(uri, text)

                val item = requireCandidate(session, uri, text, "twice")
                val arguments = item.command!!.arguments.orEmpty().toMutableList()
                arguments[0] = "stale-session-key"

                val before = session.appliedEdits().size
                val failure = runCatching {
                    session.executeCommand(
                        ExecuteCommandParams(LspCompletionCommands.APPLY_COMPLETION, arguments),
                    )
                }.exceptionOrNull()

                assertTrue(
                    protocolErrorOrNull(failure) != null,
                    "失效会话必须返回 JSON-RPC 错误，实际 $failure",
                )
                assertEquals(
                    before,
                    session.appliedEdits().size,
                    "失效会话不得发出 applyEdit：编辑区间可能已经不对",
                )
            }
        }
    }

    /**
     * 文档版本变了之后，旧候选的编辑不能再生效。
     *
     * 用户按「补全还没返回」时继续打字是常态；按旧版本算出的区间若照旧应用，
     * 会覆盖掉用户刚敲进去的内容。
     */
    @Test
    fun `applying after the document changed reports a protocol error`() {
        completionWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FILE)
                val text = fixture.text(FILE)
                session.openDocument(uri, text, version = 1)

                val item = requireCandidate(session, uri, text, "twice")
                val command = item.command!!

                session.changeDocument(uri, text.replace("return twi", "return twix"), version = 2)

                val failure = runCatching {
                    session.executeCommand(ExecuteCommandParams(command.command, command.arguments))
                }.exceptionOrNull()

                assertTrue(
                    protocolErrorOrNull(failure) != null,
                    "文档已变更时不得应用旧候选的编辑，应返回 JSON-RPC 错误，实际 $failure",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val FILE: String = "src/doc.cj"
    }

    /** 带一处未完成标识符的单文件工作区（`return twi`）。 */
    private fun completionWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("doc.cj", "realSemantics/cdoc.cj")
        .build()

    // ---------------------------------------------------------------- 断言辅助

    /** 在 `twi` 之后取候选，返回指定标签的候选。 */
    private fun requireCandidate(
        session: org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession,
        uri: String,
        text: String,
        label: String,
    ): CompletionItem {
        val completion = session.completion(
            CompletionParams(TextDocumentIdentifier(uri), caretAfterPendingIdentifier(text)),
        )
        val items = if (completion.isLeft) completion.left.orEmpty() else completion.right?.items.orEmpty()
        return items.firstOrNull { it.label == label }
            ?: error("候选里没有 `$label`，实际前 20 项：${items.take(20).map { it.label }}")
    }

    /**
     * 从异常链里取出协议错误。
     *
     * 测试连接用 `CompletableFuture.get()` 等结果，服务端返回的 `ResponseError` 会被包进
     * `ExecutionException`；只判最外层类型会漏掉真正的协议错误。
     */
    private fun protocolErrorOrNull(failure: Throwable?): ResponseErrorException? =
        generateSequence(failure) { it.cause }.filterIsInstance<ResponseErrorException>().firstOrNull()

    /** `return twi` 之后（也就是 `twi` 末尾）的位置。 */
    private fun caretAfterPendingIdentifier(text: String): Position {
        val token = "return twi"
        val offset = text.indexOf(token)
        check(offset >= 0) { "测试数据里找不到 `$token`" }
        return LspTextDocument("file:///completion.cj", "cangjie", 1, text)
            .positionAt(offset + token.length)
    }
}
