package org.cangnova.cangjie.lexer

import com.intellij.psi.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 对位官方 v1.1.3 LexerTerminatorTest：LF/CRLF 是换行，单独 CR 是未知 token 起始字符。 */
class CangJieLineTerminatorTest {
    @Test
    fun `line terminators are standalone tokens without changing their source length`() {
        for (lineTerminator in listOf("\n", "\r\n")) {
            val source = " \t$lineTerminator  b"
            val lexer = CangJieLexer()
            lexer.start(source)
            assertEquals(CjTokens.WHITE_SPACE, lexer.tokenType)
            assertEquals(0, lexer.tokenStart)
            assertEquals(2, lexer.tokenEnd)
            assertEquals(" \t", lexer.tokenText)
            lexer.advance()
            assertEquals(CjTokens.NL, lexer.tokenType)
            assertEquals(2, lexer.tokenStart)
            assertEquals(2 + lineTerminator.length, lexer.tokenEnd)
            assertEquals(lineTerminator, lexer.tokenText)
            lexer.advance()
            assertEquals(CjTokens.WHITE_SPACE, lexer.tokenType)
            assertEquals(2 + lineTerminator.length, lexer.tokenStart)
            assertEquals(4 + lineTerminator.length, lexer.tokenEnd)
            assertEquals("  ", lexer.tokenText)
            lexer.advance()
            assertEquals(CjTokens.IDENTIFIER, lexer.tokenType)
            assertEquals(4 + lineTerminator.length, lexer.tokenStart)
            assertEquals("b", lexer.tokenText)
            lexer.advance()
            assertNull(lexer.tokenType)
        }
    }

    @Test
    fun `a lone carriage return remains an unknown token start`() {
        val lexer = CangJieLexer()
        lexer.start("\r")
        assertEquals(CjTokens.UNKNOWN_TOKEN_START, lexer.tokenType)
        assertEquals(1, lexer.tokenEnd)
        lexer.advance()
        assertNull(lexer.tokenType)
    }

    @Test
    fun `unmatched backtick state consumes arbitrary following characters`() {
        for (character in listOf("§", "\r", "\\")) {
            val lexer = CangJieLexer()
            lexer.start("`$character")
            assertEquals(TokenType.BAD_CHARACTER, lexer.tokenType)
            assertEquals("`", lexer.tokenText)

            lexer.advance()
            assertEquals(TokenType.BAD_CHARACTER, lexer.tokenType)
            assertEquals(1, lexer.tokenStart)
            assertEquals(2, lexer.tokenEnd)
            assertEquals(character, lexer.tokenText)

            lexer.advance()
            assertNull(lexer.tokenType)
        }
    }
}
