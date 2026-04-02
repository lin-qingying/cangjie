package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LspDocumentStoreTest {
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
}
