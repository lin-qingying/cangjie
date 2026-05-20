package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.TokenType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens

class CangJieHighlightingLexicalKeysTest {
    @Test
    fun testKeywordNumberStringAndCommentTokens() {
        assertKeys(CjTokens.LET_KEYWORD, CangJieHighlightingColors.LET_KEYWORD)
        assertKeys(CjTokens.VAR_KEYWORD, CangJieHighlightingColors.VAR_KEYWORD)
        assertKeys(CjTokens.CONST_KEYWORD, CangJieHighlightingColors.CONST_KEYWORD)
        assertKeys(CjTokens.INTEGER_LITERAL, CangJieHighlightingColors.NUMBER)
        assertKeys(CjTokens.FLOAT_LITERAL, CangJieHighlightingColors.NUMBER)
        assertKeys(CjTokens.REGULAR_STRING_PART, CangJieHighlightingColors.STRING)
        assertKeys(CjTokens.ESCAPE_SEQUENCE, CangJieHighlightingColors.STRING_ESCAPE)
        assertKeys(CjTokens.EOL_COMMENT, CangJieHighlightingColors.LINE_COMMENT)
        assertKeys(CjTokens.SHEBANG_COMMENT, CangJieHighlightingColors.LINE_COMMENT)
        assertKeys(CjTokens.BLOCK_COMMENT, CangJieHighlightingColors.BLOCK_COMMENT)
        assertKeys(CjTokens.DOC_COMMENT, CangJieHighlightingColors.DOC_COMMENT)
    }

    @Test
    fun testCdocTagUsesBaseDocCommentAndSpecificTagKey() {
        assertKeys(
            CDocTokens.TAG_NAME,
            CangJieHighlightingColors.DOC_COMMENT,
            CangJieHighlightingColors.CDOC_TAG,
        )
    }

    @Test
    fun testOperatorBracketsPunctuationAndBadCharacterTokens() {
        assertKeys(CjTokens.PLUS, CangJieHighlightingColors.OPERATOR_SIGN)
        assertKeys(CjTokens.LPAR, CangJieHighlightingColors.PARENTHESIS)
        assertKeys(CjTokens.RPAR, CangJieHighlightingColors.PARENTHESIS)
        assertKeys(CjTokens.LBRACE, CangJieHighlightingColors.BRACES)
        assertKeys(CjTokens.RBRACE, CangJieHighlightingColors.BRACES)
        assertKeys(CjTokens.LBRACKET, CangJieHighlightingColors.BRACKETS)
        assertKeys(CjTokens.RBRACKET, CangJieHighlightingColors.BRACKETS)
        assertKeys(CjTokens.COMMA, CangJieHighlightingColors.COMMA)
        assertKeys(CjTokens.SEMICOLON, CangJieHighlightingColors.SEMICOLON)
        assertKeys(CjTokens.COLON, CangJieHighlightingColors.COLON)
        assertKeys(CjTokens.DOT, CangJieHighlightingColors.DOT)
        assertKeys(CjTokens.QUEST, CangJieHighlightingColors.QUEST)
        assertKeys(CjTokens.ARROW, CangJieHighlightingColors.ARROW)
        assertKeys(TokenType.BAD_CHARACTER, CangJieHighlightingColors.BAD_CHARACTER)
    }

    @Test
    fun testSyntaxHighlighterUsesSharedLexicalTable() {
        val highlighter = CangJieHighlighter()

        listOf(
            CjTokens.LET_KEYWORD,
            CjTokens.INTEGER_LITERAL,
            CjTokens.REGULAR_STRING_PART,
            CjTokens.PLUS,
            CjTokens.LPAR,
            CDocTokens.TAG_NAME,
            TokenType.BAD_CHARACTER,
        ).forEach { tokenType ->
            assertContentEquals(
                CangJieHighlightingLexicalKeys.keysOf(tokenType),
                highlighter.getTokenHighlights(tokenType),
            )
        }
    }

    @Test
    fun testUnmappedIdentifierHasNoLexicalHighlightingKey() {
        assertTrue(CangJieHighlightingLexicalKeys.keysOf(CjTokens.IDENTIFIER).isEmpty())
        assertEquals(null, CangJieHighlightingLexicalKeys.primaryKeyOf(CjTokens.IDENTIFIER))
    }

    private fun assertKeys(
        tokenType: com.intellij.psi.tree.IElementType,
        vararg expected: TextAttributesKey,
    ) {
        assertContentEquals(expected.asList(), CangJieHighlightingLexicalKeys.keysOf(tokenType).asList())
        assertEquals(expected.firstOrNull(), CangJieHighlightingLexicalKeys.primaryKeyOf(tokenType))
    }
}
