package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.openapi.util.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.cangnova.cangjie.lexer.CjTokens

class CangJieHighlightingLexerTest {
    @Test
    fun macroEscapeTokensKeepTheirFullLexemesAndRanges() {
        val source = "\\(\\)\\[\\]\\@!"
        val expected = listOf(
            Triple(CjTokens.ESCAPE_LPAR, "\\(", TextRange(0, 2)),
            Triple(CjTokens.ESCAPE_RPAR, "\\)", TextRange(2, 4)),
            Triple(CjTokens.ESCAPE_LBRACKET, "\\[", TextRange(4, 6)),
            Triple(CjTokens.ESCAPE_RBRACKET, "\\]", TextRange(6, 8)),
            Triple(CjTokens.ESCAPE_AT, "\\@", TextRange(8, 10)),
            Triple(CjTokens.EXCL, "!", TextRange(10, 11)),
        )
        val lexer = CangJieHighlightingLexer()
        lexer.start(source)
        for ((type, lexeme, range) in expected) {
            assertEquals(type, lexer.tokenType)
            assertEquals(lexeme, lexer.tokenText)
            assertEquals(range, TextRange(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        assertNull(lexer.tokenType)
    }
}
