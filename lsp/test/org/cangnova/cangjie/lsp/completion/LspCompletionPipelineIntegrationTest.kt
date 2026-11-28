package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LSP 补全端到端：`textDocument/completion` 必须产出**共享管线**的候选（设计 §7.11.5/§7.11.6）。
 *
 * 判定「走的是哪条路径」不看候选文本，而看**只有新路径才有的字段**：
 * 候选项带 `applyCompletion` 命令与 `data` 会话键（旧实现产出的项什么都没有）。
 * 另外确认旧实现的特征——全 `Text` 类别与全工作区扫描——在新路径下不出现。
 */
class LspCompletionPipelineIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 半截标识符位置必须给出管线候选，并带命令与会话键。
     */
    @Test
    fun completionComesFromSharedPipelineWithApplyCommand() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(MODEL_NAME, MODEL_SOURCE)
            .source(USE_NAME, USE_SOURCE)
            .build()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val uri = fixture.uri(USE_FILE)
            val text = fixture.text(USE_FILE)
            connection.openDocument(uri, text)

            val items = connection.completion(
                CompletionParams(TextDocumentIdentifier(uri), positionAfter(text, PARTIAL)),
            ).left

            assertTrue(items.isNotEmpty(), "半截标识符位置必须给出候选，实际 $items")
            val buildUser = items.filter { it.label == "buildUser" }
            assertTrue(buildUser.isNotEmpty(), "依赖文件里的顶层函数必须成为候选，实际 ${items.map(CompletionItem::getLabel)}")

            val item = buildUser.first()
            assertEquals(
                LspCompletionCommands.APPLY_COMPLETION,
                item.command?.command,
                "新路径的候选必须带 applyCompletion 命令（旧实现没有）",
            )
            assertTrue(item.data != null, "新路径的候选必须带会话键 data，实际 ${item.data}")
            assertTrue(
                item.sortText.isNullOrEmpty().not(),
                "新路径的候选必须带 sortText（服务端预排序），实际 ${item.sortText}",
            )
        }
    }

    /**
     * 旧实现的特征不得出现在默认路径里：类别不再一律 `Text`。
     */
    @Test
    fun defaultPathDoesNotFlattenKindsToText() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(MODEL_NAME, MODEL_SOURCE)
            .source(USE_NAME, USE_SOURCE)
            .build()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val uri = fixture.uri(USE_FILE)
            val text = fixture.text(USE_FILE)
            connection.openDocument(uri, text)

            val items = connection.completion(
                CompletionParams(TextDocumentIdentifier(uri), positionAfter(text, PARTIAL)),
            ).left

            val kinds = items.map(CompletionItem::getKind).distinct()
            assertTrue(
                kinds.any { it != org.eclipse.lsp4j.CompletionItemKind.Text },
                "类别必须按候选形状映射，不得一律 Text（旧实现特征），实际 $kinds",
            )
            assertNotEquals(0, items.size)
        }
    }

    /**
     * 把文本偏移换算成 LSP 位置（夹具源码只用 `\n`）。
     */
    private fun positionAfter(text: String, token: String): Position {
        val offset = text.indexOf(token) + token.length
        val prefix = text.substring(0, offset)
        return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
    }

    private companion object {
        const val PARTIAL = "buil"
        const val MODEL_NAME = "model.cj"
        const val USE_NAME = "use.cj"
        const val USE_FILE = "src/use.cj"

        /** 依赖文件：提供 `buildUser`。 */
        val MODEL_SOURCE = """
            package sample.pipeline

            func buildUser(): Int64 {
                1
            }
        """.trimIndent()

        /** 触发文件：`let u = buil` 是半截标识符，必须靠 dummy 修正才能分析。 */
        val USE_SOURCE = """
            package sample.pipeline

            func consume(): Int64 {
                let u = buil
                return 1
            }
        """.trimIndent()
    }
}
