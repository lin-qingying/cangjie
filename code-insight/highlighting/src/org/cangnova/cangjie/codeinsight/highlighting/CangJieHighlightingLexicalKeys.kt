package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens.CDOC_HIGHLIGHT_TOKENS

/**
 * 仓颉词法 token 到 TextAttributesKey 的唯一映射入口。
 *
 * 这张表就是共享词法高亮事实源。IDE 的 SyntaxHighlighter、后续其他 IntelliJ-framework 消费方
 * 都应从这里取 token 映射，避免在不同宿主中各自维护一套 token -> key 规则。
 */
object CangJieHighlightingLexicalKeys {
    private val keys1: MutableMap<IElementType, TextAttributesKey> = HashMap()
    private val keys2: MutableMap<IElementType, TextAttributesKey> = HashMap()

    init {
        SyntaxHighlighterBase.fillMap(keys1, CjTokens.KEYWORDS, CangJieHighlightingColors.KEYWORD)
        SyntaxHighlighterBase.fillMap(keys1, CjTokens.BASICTYPES, CangJieHighlightingColors.KEYWORD)

        keys1[CjTokens.LET_KEYWORD] = CangJieHighlightingColors.LET_KEYWORD
        keys1[CjTokens.VAR_KEYWORD] = CangJieHighlightingColors.VAR_KEYWORD
        keys1[CjTokens.CONST_KEYWORD] = CangJieHighlightingColors.CONST_KEYWORD
        keys1[CjTokens.QUOTE_KEYWORD] = CangJieHighlightingColors.QUOTE_KEYWORD

        keys1[CjTokens.INTEGER_LITERAL] = CangJieHighlightingColors.NUMBER
        keys1[CjTokens.FLOAT_LITERAL] = CangJieHighlightingColors.NUMBER

        SyntaxHighlighterBase.fillMap(
            keys1,
            TokenSet.andNot(
                CjTokens.OPERATIONS,
                TokenSet.orSet(
                    TokenSet.create(CjTokens.IDENTIFIER, CjTokens.AT),
                    CjTokens.KEYWORDS,
                ),
            ),
            CangJieHighlightingColors.OPERATOR_SIGN,
        )

        keys1[CjTokens.LPAR] = CangJieHighlightingColors.PARENTHESIS
        keys1[CjTokens.RPAR] = CangJieHighlightingColors.PARENTHESIS
        keys1[CjTokens.LBRACE] = CangJieHighlightingColors.BRACES
        keys1[CjTokens.RBRACE] = CangJieHighlightingColors.BRACES
        keys1[CjTokens.LBRACKET] = CangJieHighlightingColors.BRACKETS
        keys1[CjTokens.RBRACKET] = CangJieHighlightingColors.BRACKETS
        keys1[CjTokens.COMMA] = CangJieHighlightingColors.COMMA
        keys1[CjTokens.SEMICOLON] = CangJieHighlightingColors.SEMICOLON
        keys1[CjTokens.COLON] = CangJieHighlightingColors.COLON
        keys1[CjTokens.QUEST] = CangJieHighlightingColors.QUEST
        keys1[CjTokens.DOT] = CangJieHighlightingColors.DOT
        keys1[CjTokens.ARROW] = CangJieHighlightingColors.ARROW

        keys1[CjTokens.OPEN_QUOTE] = CangJieHighlightingColors.STRING
        keys1[CjTokens.CLOSING_QUOTE] = CangJieHighlightingColors.STRING
        keys1[CjTokens.REGULAR_STRING_PART] = CangJieHighlightingColors.STRING
        keys1[CjTokens.LONG_TEMPLATE_ENTRY_END] = CangJieHighlightingColors.STRING_ESCAPE
        keys1[CjTokens.LONG_TEMPLATE_ENTRY_START] = CangJieHighlightingColors.STRING_ESCAPE
        keys1[CjTokens.SHORT_TEMPLATE_ENTRY_START] = CangJieHighlightingColors.STRING_ESCAPE
        keys1[CjTokens.ESCAPE_SEQUENCE] = CangJieHighlightingColors.STRING_ESCAPE
        keys1[CjTokens.RUNE_LITERAL] = CangJieHighlightingColors.STRING

        keys1[CjTokens.EOL_COMMENT] = CangJieHighlightingColors.LINE_COMMENT
        keys1[CjTokens.SHEBANG_COMMENT] = CangJieHighlightingColors.LINE_COMMENT
        keys1[CjTokens.BLOCK_COMMENT] = CangJieHighlightingColors.BLOCK_COMMENT
        keys1[CjTokens.DOC_COMMENT] = CangJieHighlightingColors.DOC_COMMENT

        SyntaxHighlighterBase.fillMap(keys1, CDOC_HIGHLIGHT_TOKENS, CangJieHighlightingColors.DOC_COMMENT)
        keys1[CDocTokens.TAG_NAME] = CangJieHighlightingColors.DOC_COMMENT
        keys2[CDocTokens.TAG_NAME] = CangJieHighlightingColors.CDOC_TAG

        keys1[TokenType.BAD_CHARACTER] = CangJieHighlightingColors.BAD_CHARACTER
    }

    /**
     * 返回 IntelliJ SyntaxHighlighter 需要的高亮 key 数组。
     */
    fun keysOf(tokenType: IElementType): Array<TextAttributesKey> =
        SyntaxHighlighterBase.pack(keys1[tokenType], keys2[tokenType])

    /**
     * 返回 token 的主高亮 key。只用于需要单 key 查询的共享代码。
     */
    fun primaryKeyOf(tokenType: IElementType): TextAttributesKey? = keys1[tokenType] ?: keys2[tokenType]
}
