package org.cangnova.cangjie.lexer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 官方 cjc 1.1.3 接受 Unicode 15 标识符，并保留源码范围。 */
class CangJieUnicodeIdentifierTest {
    @Test
    fun `decomposed character remains part of identifier`() {
        assertSingleIdentifier("cafe\u0301")
    }

    @Test
    fun `Unicode 15 supplementary character can start identifier`() {
        assertSingleIdentifier("\uD807\uDF02Type")
    }

    @Test
    fun `Unicode 15 supplementary combining character can continue identifier`() {
        assertSingleIdentifier("a\uD807\uDF00")
    }

    @Test
    fun `supplementary identifier continuation does not change exclamation token boundary`() {
        for (prefix in listOf("in", "is")) {
            val identifier = prefix + "\uD807\uDF02"
            val lexer = CangJieLexer()
            lexer.start("!$identifier")
            assertEquals(CjTokens.EXCL, lexer.tokenType)
            assertEquals("!", lexer.tokenText)
            lexer.advance()
            assertEquals(CjTokens.IDENTIFIER, lexer.tokenType)
            assertEquals(identifier, lexer.tokenText)
            lexer.advance()
            assertNull(lexer.tokenType)
        }
    }

    private fun assertSingleIdentifier(source: String) {
        val lexer = CangJieLexer()
        lexer.start(source)
        assertEquals(CjTokens.IDENTIFIER, lexer.tokenType)
        assertEquals(0, lexer.tokenStart)
        assertEquals(source.length, lexer.tokenEnd)
        assertEquals(source, lexer.tokenText)
        lexer.advance()
        assertNull(lexer.tokenType)
    }
}
