package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DocumentHighlightKind
import org.eclipse.lsp4j.DocumentHighlightParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 编辑器细节行为：嵌套调用的签名帮助、高亮的读/写种类。
 *
 * 这两条都是日常编辑高频路径，且容易在实现里被「取第一个签名 / 一律 Text」敷衍过去；
 * 断言必须落到具体签名与具体 kind，否则等于没测。
 */
class CangjieRealSemanticsEditorDetailTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：需要带源码的临时工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 嵌套调用中的签名帮助必须锁定**内层**调用与对应实参。
     *
     * 光标在 `inner(1, |` 时，客户端要高亮 `inner` 的第二个参数；若实现取最外层调用，
     * 参数提示会指到 `outer` 的第一个参数，用户看到的是错位的签名。
     */
    @Test
    fun `signature help resolves the innermost call at the caret`() {
        editorWorkspace("sig.cj", "realSemantics/signature.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/sig.cj")
                val text = fixture.text("src/sig.cj")
                session.openDocument(uri, text)

                val caret = positionAt(text, text.indexOf("inner(1, ") + "inner(1, ".length)
                val help = session.signatureHelp(SignatureHelpParams(TextDocumentIdentifier(uri), caret))

                assertFalse(help.signatures.isEmpty(), "嵌套调用内必须给出签名帮助")
                val active = help.activeSignature ?: 0
                val label = help.signatures[active.coerceIn(0, help.signatures.lastIndex)].label
                assertTrue(
                    label.contains("inner"),
                    "光标在 inner(...) 内时必须选中 inner 的签名，实际选中 `$label`（全部签名=" +
                        "${help.signatures.map { it.label }}）",
                )
                assertEquals(
                    1,
                    help.activeParameter ?: -1,
                    "光标位于 inner 的第二个实参，activeParameter 必须是 1，实际 ${help.activeParameter}",
                )
            }
        }
    }

    /**
     * 高亮必须区分读与写：写位置给 `Write`，读位置给 `Read`。
     *
     * LSP 定义这三个 kind 就是为了让编辑器用不同颜色区分读写；全部返回 `Text`
     * 会让「谁在改这个字段」失去视觉线索。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `document highlight distinguishes reads and writes`() {
        editorWorkspace("kind.cj", "realSemantics/highlight.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/kind.cj")
                val text = fixture.text("src/kind.cj")
                session.openDocument(uri, text)

                val declarationPosition = positionAt(text, text.indexOf("var value") + "var ".length)
                val writePosition = positionAt(text, text.indexOf("value = 1"))
                val readPosition = positionAt(text, text.indexOf("return value") + "return ".length)

                val declaration = session.documentHighlight(
                    DocumentHighlightParams(TextDocumentIdentifier(uri), declarationPosition),
                )
                val writes = session.documentHighlight(
                    DocumentHighlightParams(TextDocumentIdentifier(uri), writePosition),
                )
                val reads = session.documentHighlight(
                    DocumentHighlightParams(TextDocumentIdentifier(uri), readPosition),
                )
                val summary = "decl=${describe(declaration)} write=${describe(writes)} read=${describe(reads)}"

                assertTrue(
                    declaration.any { it.kind == DocumentHighlightKind.Write },
                    "声明位置应聚合到写引用（客户端据此区分读写），实际 $summary",
                )
                assertTrue(
                    reads.any { it.kind == DocumentHighlightKind.Read && covers(it.range, readPosition) },
                    "读取位置必须给出 Read 高亮，实际 $summary",
                )
                assertTrue(
                    writes.any { it.kind == DocumentHighlightKind.Write && covers(it.range, writePosition) },
                    "赋值位置必须给出 Write 高亮（当前为空说明写入目标解析不出来），实际 $summary",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    /**
     * 单文件工作区：源码来自 testData。
     */
    private fun editorWorkspace(fileName: String, testDataPath: String): LspWorkspaceFixture =
        LspWorkspaceFixtureBuilder()
            .sourceFromTestData(fileName, testDataPath)
            .build()

    // ---------------------------------------------------------- 断言辅助

    /**
     * 判断范围是否覆盖指定位置。
     */
    private fun covers(range: Range, position: Position): Boolean =
        !after(range.start, position) && !after(position, range.end)

    /**
     * 高亮结果的可读描述（kind + 行），用于失败诊断。
     */
    private fun describe(highlights: List<org.eclipse.lsp4j.DocumentHighlight>): List<String> =
        highlights.map { "${it.kind}@${it.range.start.line}:${it.range.start.character}" }

    /**
     * 位置先后比较。
     */
    private fun after(left: Position, right: Position): Boolean =
        left.line > right.line || (left.line == right.line && left.character > right.character)

    /**
     * 偏移转位置。
     */
    private fun positionAt(text: String, offset: Int): Position =
        LspTextDocument("file:///editor.cj", "cangjie", 1, text).positionAt(offset)

}
