package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 新旧补全实现的候选集对比（设计 §7.11.6）。
 *
 * 验收两条：
 * 1. **新集合 ⊇ 旧集合**——换实现不得让用户少拿到本来能拿到的候选；
 * 2. 默认路径的候选**全部带管线标记**（命令 + 会话键），即旧实现的全工作区遍历
 *    不在默认路径里以任何形式生效（它只可能来自那条被开关关掉的路径）。
 *
 * **对比位置取空前缀**（`let u = ` 之后、还没敲任何字符），理由是两边回答的问题才相同：
 * 新路径**按已键入前缀过滤**（用户在补全前敲的半截标识符就该过滤掉不匹配的名字），
 * 旧实现完全不看前缀、直接把文件作用域与工作区名字倾倒出来。
 * 拿「敲了 `buil` 的位置」去比，等于问两个不同的问题——那会把「新实现正确地少了 `consume`」
 * 误判成「新实现少了候选」。
 */
class LspCompletionNewVsLegacyTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 在同一批固定位置上，新实现给出的集合必须覆盖旧实现给出的全部标签，
     * 且默认路径的项都来自管线。
     *
     * 位置是**固定清单**（不随机、不按候选动态挑），否则一次失败无法复现。
     */
    @Test
    fun newPipelineCoversEveryLabelLegacyFound() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(USE_NAME, USE_SOURCE)
            .build()
        val uri = fixture.uri("src/$USE_NAME")
        val text = fixture.text("src/$USE_NAME")

        positionsIn(text).forEach { (name, position) ->
            val newItems = itemsFrom(defaultServerOptions(), fixture, uri, position)
            val legacyItems = itemsFrom(legacyServerOptions(), fixture, uri, position)

            val newLabels = newItems.map(CompletionItem::getLabel).toSet()
            val legacyLabels = legacyItems.map(CompletionItem::getLabel).toSet()
            assertTrue(
                newLabels.containsAll(legacyLabels),
                "[$name] 新集合必须 ⊇ 旧集合；旧有而新缺的标签：${legacyLabels - newLabels}；" +
                    "新集合（${newLabels.size}）：${newLabels.sorted().take(40)}；" +
                    "旧集合（${legacyLabels.size}）：${legacyLabels.sorted().take(40)}",
            )

            assertTrue(
                newItems.all { it.command?.command == LspCompletionCommands.APPLY_COMPLETION && it.data != null },
                "[$name] 默认路径的候选必须全部来自管线（带命令与会话键），" +
                    "否则说明旧的全工作区遍历还在生效：" +
                    "${newItems.filter { it.command == null }.map(CompletionItem::getLabel)}",
            )
        }
    }

    /**
     * 固定的对比位置清单。
     *
     * 都取**空前缀**位置（理由见类注释），且都是**普通表达式/语句位置**——
     * 参数位置、成员访问位置等会走各自的专用区段（例如命名实参只给参数名），
     * 那些位置上新旧两条路径回答的本来就不是同一个问题。
     */
    private fun positionsIn(text: String): List<Pair<String, Position>> = listOf(
        // `func buildUser(): Int64 {\n` 之后的缩进行首：语句位置。
        "函数体起始" to positionAt(text, text.indexOf("{\n") + 2),
        "let 初始化中" to positionAt(text, text.indexOf("let u =") + "let u =".length),
        "二元表达式右侧" to positionAt(text, text.indexOf("1 +") + "1 +".length),
    )

    /**
     * 用给定服务端选项取一次补全结果。
     */
    private fun itemsFrom(
        options: CangjieLspServerOptions,
        fixture: LspWorkspaceFixture,
        uri: String,
        position: Position,
    ): List<CompletionItem> {
        LspIntegrationTestConnection.create(options).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            connection.openDocument(uri, fixture.text("src/$USE_NAME"))
            val result: Either<List<CompletionItem>, CompletionList> =
                connection.completion(CompletionParams(TextDocumentIdentifier(uri), position))
            return result.left
        }
    }

    /**
     * 打开旧实现的服务端选项（其余与默认一致）。
     */
    private fun legacyServerOptions(): CangjieLspServerOptions =
        defaultServerOptions().copy(
            descriptor = CangjieLanguageServerDescriptor(useLegacyCompletion = true),
        )

    /**
     * 偏移 → LSP 位置（夹具源码只用 `\n`）。
     */
    private fun positionAt(text: String, offset: Int): Position {
        val prefix = text.substring(0, offset)
        return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
    }

    private companion object {
        const val PARTIAL = "buil"
        const val USE_NAME = "use.cj"

        /** 触发文件：三个固定位置都是**空前缀的普通表达式/语句位置**（见 `positionsIn`）。 */
        val USE_SOURCE = """
            package sample.compare

            func buildUser(): Int64 {

                1
            }

            func consume(): Int64 {
                let u =
                let v = 1 +
                return v
            }
        """.trimIndent()
    }
}
