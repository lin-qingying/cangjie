package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CangjieFormattingIntegrationTest : AbstractLspIntegrationTest() {
    @Test
    fun `document formatting returns shared formatter edit and respects indent width`() {
        val uri = "file:///workspace/formatting.cj"
        session.openDocument(
            uri,
            """
            func main(){
            let value=1+2
            }
            """.trimIndent(),
        )

        val edits = session.formatting(
            DocumentFormattingParams(
                TextDocumentIdentifier(uri),
                FormattingOptions(2, true),
            ),
        )

        assertFalse(edits.isEmpty())
        assertEquals(1, edits.size)
        assertEquals(Position(0, 0), edits.single().range.start)
        assertEquals(
            """
            func main() {
              let value = 1 + 2
            }
            """.trimIndent(),
            edits.single().newText.trimEnd(),
        )
    }
}
