package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 补全在协议层的两条边界（设计 §7.11.7 前两项）。
 *
 * 1. **无候选返回合法空列表**，而不是异常：位置不允许补全（原始字符串内部、
 *    dummy 修正返回 null）是正常场景，客户端要能拿到一个空结果。
 * 2. **`isIncomplete` 与实际是否穷尽候选一致**：服务端一次算完全部候选，
 *    因此返回**完整列表**（`Either.forLeft`）——LSP 里没有 `isIncomplete` 字段的列表
 *    即代表「已穷尽」，客户端不必再请求。
 */
class LspCompletionProtocolBoundaryTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 原始字符串内部：位置不允许补全 → 空列表，不抛异常。
     */
    @Test
    fun positionThatAllowsNoCompletionReturnsEmptyList() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(USE_NAME, RAW_STRING_SOURCE)
            .build()
        val uri = fixture.uri("src/$USE_NAME")
        val text = fixture.text("src/$USE_NAME")
        // 光标落在原始字符串内部（`raw"a` 之后）：那里面不插值，补全产出的代码永远不会被求值。
        val position = positionAt(text, text.indexOf("raw\"a") + "raw\"a".length)

        withConnection(fixture, uri, text) { connection ->
            val result = connection.completion(CompletionParams(TextDocumentIdentifier(uri), position))

            assertInstanceOf(
                Either::class.java,
                result,
                "无候选位置必须返回合法结果而不是抛异常",
            )
            assertTrue(
                result.left.isEmpty(),
                "原始字符串内部不得给出候选，实际 ${result.left.map(CompletionItem::getLabel)}",
            )
        }
    }

    /**
     * 有候选时返回的是**完整列表**（`Either.forLeft`），即已穷尽，`isIncomplete` 为假。
     */
    @Test
    fun completeCandidateSetIsReturnedAsExhaustiveList() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(USE_NAME, PLAIN_SOURCE)
            .build()
        val uri = fixture.uri("src/$USE_NAME")
        val text = fixture.text("src/$USE_NAME")
        val position = positionAt(text, text.indexOf("let u =") + "let u =".length)

        withConnection(fixture, uri, text) { connection ->
            val result: Either<List<CompletionItem>, CompletionList> =
                connection.completion(CompletionParams(TextDocumentIdentifier(uri), position))

            assertTrue(result.isLeft, "服务端一次算完，必须返回完整列表而不是 isIncomplete=true 的分片")
            assertTrue(
                result.left.isNotEmpty(),
                "这个位置应当有候选（同文件的顶层函数），实际 ${result.left}",
            )
        }
    }

    /**
     * 打开文档并执行断言。
     */
    private fun withConnection(
        fixture: LspWorkspaceFixture,
        uri: String,
        text: String,
        body: (LspIntegrationTestConnection) -> Unit,
    ) {
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            connection.openDocument(uri, text)
            body(connection)
        }
    }

    /**
     * 偏移 → LSP 位置（夹具源码只用 `\n`）。
     */
    private fun positionAt(text: String, offset: Int): Position {
        val prefix = text.substring(0, offset)
        return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
    }

    private companion object {
        const val USE_NAME = "use.cj"

        /** 含原始字符串的文件（原始字符串不插值，其内部不允许补全）。 */
        val RAW_STRING_SOURCE = """
            package sample.boundary

            func consume(): Int64 {
                let text = raw"ab"
                return 1
            }
        """.trimIndent()

        /** 普通文件：顶层函数可供补全。 */
        val PLAIN_SOURCE = """
            package sample.boundary

            func buildUser(): Int64 {
                1
            }

            func consume(): Int64 {
                let u =
                return 1
            }
        """.trimIndent()
    }
}
