package org.cangnova.cangjie.cfir.builder.macro

import com.intellij.psi.TokenType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens

/** 相对于宏包装文本的 UTF-16 半开区间。 */
data class MacroExpressionTextRange(val startOffset: Int, val endOffset: Int)

/** 当前宏头部的原文区间与名称 token；不包含被包装的输入声明。 */
data class MacroExpressionHeadSyntax(
    val annotationStartOffset: Int,
    val rawName: String,
    val nameRange: MacroExpressionTextRange,
    val segmentRanges: List<MacroExpressionTextRange>,
    val attrRange: MacroExpressionTextRange?,
    val hasParenthesizedInput: Boolean,
)

/** 使用与 parser 相同的 lexer 获取宏头部，而不根据 JVM 字符类别猜测标识符。 */
object MacroExpressionHeadScanner {
    fun scan(text: CharSequence, startOffset: Int): MacroExpressionHeadSyntax? {
        require(startOffset in 0..text.length)
        val lexer = CangJieLexer()
        lexer.start(text, startOffset, text.length, 0)
        if (lexer.tokenType != CjTokens.AT && lexer.tokenType != CjTokens.ATEXCL) return null
        val annotationStart = lexer.tokenStart
        lexer.advance()

        fun skipTrivia() {
            while (lexer.tokenType != null && CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(lexer.tokenType)) {
                lexer.advance()
            }
        }

        skipTrivia()
        val segments = mutableListOf<MacroExpressionTextRange>()
        while (true) {
            if (lexer.tokenType != CjTokens.IDENTIFIER) return null
            segments += MacroExpressionTextRange(lexer.tokenStart, lexer.tokenEnd)
            lexer.advance()
            skipTrivia()
            if (lexer.tokenType != CjTokens.DOT) break
            lexer.advance()
            skipTrivia()
        }

        val nameRange = MacroExpressionTextRange(segments.first().startOffset, segments.last().endOffset)
        val attrStart = lexer.tokenStart
        var attrEnd: Int? = null
        val hasAttribute = lexer.tokenType == CjTokens.LBRACKET
        if (hasAttribute) {
            var depth = 0
            while (lexer.tokenType != null) {
                if (lexer.tokenType == TokenType.BAD_CHARACTER && lexer.tokenText == "\\") {
                    lexer.advance()
                    skipTrivia()
                    if (lexer.tokenType != CjTokens.LBRACKET && lexer.tokenType != CjTokens.RBRACKET && lexer.tokenType != CjTokens.AT) break
                    lexer.advance()
                    continue
                }
                when (lexer.tokenType) {
                    CjTokens.LBRACKET -> depth++
                    CjTokens.RBRACKET -> {
                        depth--
                        if (depth == 0) {
                            attrEnd = lexer.tokenEnd
                            break
                        }
                    }
                }
                lexer.advance()
            }
            if (attrEnd != null) {
                lexer.advance()
                skipTrivia()
            }
        }
        return MacroExpressionHeadSyntax(
            annotationStartOffset = annotationStart,
            rawName = text.subSequence(nameRange.startOffset, nameRange.endOffset).toString(),
            nameRange = nameRange,
            segmentRanges = segments,
            attrRange = attrEnd?.let { MacroExpressionTextRange(attrStart, it) },
            hasParenthesizedInput = (!hasAttribute || attrEnd != null) && lexer.tokenType == CjTokens.LPAR,
        )
    }
}
