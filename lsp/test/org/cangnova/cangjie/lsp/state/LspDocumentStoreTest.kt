package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 校验 LSP 文档存储的增量编辑和坐标转换行为。
 */
class LspDocumentStoreTest {
    /**
     * 校验 LF 文本上的 UTF-16 行列范围增量替换。
     */
    @Test
    fun `applies incremental change using utf16 positions`() {
        val store = LspDocumentStore()
        store.open(TextDocumentItem("file:///sample.cj", "cangjie", 1, "alpha\nbeta\ngamma"))

        val updated = store.applyChanges(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier("file:///sample.cj", 2),
                listOf(
                    TextDocumentContentChangeEvent(
                        Range(Position(1, 0), Position(1, 4)),
                        "BETA",
                    ),
                ),
            ),
        )

        assertEquals("alpha\nBETA\ngamma", updated.text)
        assertEquals(2, updated.version)
    }

    /**
     * 校验缺少 range 的变更会替换整篇文档。
     */
    @Test
    fun `replaces whole document when range is absent`() {
        val store = LspDocumentStore()
        store.open(TextDocumentItem("file:///sample.cj", "cangjie", 1, "old"))

        val updated = store.applyChanges(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier("file:///sample.cj", 2),
                listOf(TextDocumentContentChangeEvent("new")),
            ),
        )

        assertEquals("new", updated.text)
        assertEquals(2, updated.version)
    }

    /**
     * 校验 LF 文本 offset 可以转换回 LSP range。
     */
    @Test
    fun `converts offsets back to lsp ranges`() {
        val document = LspTextDocument(
            uri = "file:///sample.cj",
            languageId = "cangjie",
            version = 1,
            text = "alpha\nbeta\ngamma",
        )

        assertEquals(Position(1, 0), document.positionAt(6))
        assertEquals(
            Range(Position(1, 1), Position(1, 4)),
            document.rangeOf(7, 10),
        )
    }

    /**
     * 校验 CRLF 文本上的增量范围按客户端原始文本坐标计算。
     */
    @Test
    fun `applies incremental change using crlf positions`() {
        val store = LspDocumentStore()
        store.open(TextDocumentItem("file:///sample.cj", "cangjie", 1, "alpha\r\nbeta\r\ngamma"))

        val updated = store.applyChanges(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier("file:///sample.cj", 2),
                listOf(
                    TextDocumentContentChangeEvent(
                        Range(Position(1, 1), Position(1, 3)),
                        "ET",
                    ),
                ),
            ),
        )

        assertEquals("alpha\r\nbETa\r\ngamma", updated.text)
    }

    /**
     * 校验多个增量变更按客户端声明顺序依次应用。
     */
    @Test
    fun `applies multiple incremental changes in declared order`() {
        val store = LspDocumentStore()
        store.open(TextDocumentItem("file:///sample.cj", "cangjie", 1, "alpha\nbeta\ngamma"))

        val updated = store.applyChanges(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier("file:///sample.cj", 2),
                listOf(
                    TextDocumentContentChangeEvent(
                        Range(Position(0, 0), Position(0, 5)),
                        "ALPHA",
                    ),
                    TextDocumentContentChangeEvent(
                        Range(Position(2, 0), Position(2, 5)),
                        "GAMMA",
                    ),
                ),
            ),
        )

        assertEquals("ALPHA\nbeta\nGAMMA", updated.text)
    }

    /**
     * 校验 CRLF 文本的 offset、position 和 range 互转。
     */
    @Test
    fun `converts offsets back to lsp ranges with crlf text`() {
        val document = LspTextDocument(
            uri = "file:///sample.cj",
            languageId = "cangjie",
            version = 1,
            text = "alpha\r\nbeta\r\ngamma",
        )

        assertEquals(Position(1, 0), document.positionAt(7))
        assertEquals(
            Range(Position(1, 1), Position(1, 3)),
            document.rangeOf(8, 10),
        )
        assertEquals(8, document.offsetAt(Position(1, 1)))
    }

    /**
     * 校验代理对字符按 UTF-16 语义处理。
     */
    @Test
    fun `treats surrogate pairs with utf16 semantics`() {
        val store = LspDocumentStore()
        store.open(TextDocumentItem("file:///sample.cj", "cangjie", 1, "a😀b"))

        val updated = store.applyChanges(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier("file:///sample.cj", 2),
                listOf(
                    TextDocumentContentChangeEvent(
                        Range(Position(0, 1), Position(0, 3)),
                        "X",
                    ),
                ),
            ),
        )

        assertEquals("aXb", updated.text)
        assertEquals(3, LspTextDocument("file:///sample.cj", "cangjie", 1, "a😀b").offsetAt(Position(0, 3)))
    }

    /**
     * 校验 range edit 可以在文件末尾插入文本。
     */
    @Test
    fun `supports eof insertion through range edits`() {
        val store = LspDocumentStore()
        store.open(TextDocumentItem("file:///sample.cj", "cangjie", 1, "alpha"))

        val updated = store.applyChanges(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier("file:///sample.cj", 2),
                listOf(
                    TextDocumentContentChangeEvent(
                        Range(Position(0, 5), Position(0, 5)),
                        "\nomega",
                    ),
                ),
            ),
        )

        assertEquals("alpha\nomega", updated.text)
    }
}
