/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens

/** 宏 token payload 在宿主文件中的原文与起始偏移。 */
data class CjMacroTokenPayload(
    /** 外层 delimiter 之间的准确源码文本，保留空白和注释。 */
    val text: String,
    /** [text] 在宿主文件中的起始偏移。 */
    val startOffset: Int,
) {
    companion object {
        /**
         * 从宿主源码提取一组 delimiter 之间的 payload，并在首个直接解析错误处截断。
         * 直接错误节点属于该 token 容器的语法层；不向嵌套表达式递归，避免吞掉合法子语法。
         */
        fun fromDelimitedElement(
            element: PsiElement,
            leftDelimiterType: IElementType,
            rightDelimiterType: IElementType,
            tokenContainer: PsiElement? = null,
        ): CjMacroTokenPayload? {
            val leftDelimiter = element.node.findChildByType(leftDelimiterType) ?: return null
            val rightDelimiter = element.node.findChildByType(rightDelimiterType) ?: return null
            val startOffset = leftDelimiter.startOffset + leftDelimiter.textLength
            val delimiterEndOffset = rightDelimiter.startOffset
            val firstParserErrorOffset = firstDirectParserErrorOffset(tokenContainer)
                ?.takeIf { it in startOffset..delimiterEndOffset }
            val endOffset = firstParserErrorOffset ?: delimiterEndOffset
            if (endOffset < startOffset) return null
            return CjMacroTokenPayload(
                text = element.containingFile.text.substring(startOffset, endOffset),
                startOffset = startOffset,
            )
        }

        /**
         * 返回错误恢复节点中首个非空白、非注释 token 的宿主源码偏移。
         *
         * PSI 与 LightTree 的错误节点可能把错误 token 前的 trivia 一并纳入节点范围；
         * 截断必须保留这段仍属于合法 token 流的 trivia，并从真正错误 token 开始。
         */
        fun firstNonTriviaOffset(text: CharSequence, baseOffset: Int): Int {
            val lexer = CangJieLexer()
            lexer.start(text)
            while (true) {
                val tokenType = lexer.tokenType ?: break
                if (!CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(tokenType)) break
                lexer.advance()
            }
            val relativeOffset = if (lexer.tokenType == null) text.length else lexer.tokenStart
            return baseOffset + relativeOffset
        }

        /** 返回 token 容器第一处错误的源码边界，并保留零宽错误节点后的连续 trivia。 */
        fun firstDirectParserErrorOffset(tokenContainer: PsiElement?): Int? {
            val children = tokenContainer?.node?.getChildren(null) ?: return null
            return children.indices.asSequence()
                .filter { index -> children[index].elementType == TokenType.ERROR_ELEMENT }
                .map { index ->
                    val error = children[index]
                    val errorText = StringBuilder(error.text)
                    var nextIndex = index + 1
                    while (nextIndex < children.size &&
                        CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(children[nextIndex].elementType)
                    ) {
                        errorText.append(children[nextIndex].text)
                        nextIndex++
                    }
                    firstNonTriviaOffset(errorText, error.startOffset)
                }
                .minOrNull()
        }

        /** 只检查当前 token 容器的解析错误，保持与宏 token grammar 的边界一致。 */
        fun hasDirectParserError(tokenContainer: PsiElement?): Boolean =
            tokenContainer?.node?.getChildren(null)?.any { it.elementType == TokenType.ERROR_ELEMENT } == true
    }
}

/**
 * 表示 `CjMacroInput`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjMacroInput(node: ASTNode) : CjExpressionImpl(node) {

    /**
     * 宏括号输入中的 token 容器；不包含外层 `(` 与 `)`，且不包含未保留到 PSI 树的 trivia。
     *
     * 宏协议的 `Tokens` 参数只对应 `QUOTE_TOKENS`；构造完整 payload 时使用
     * [parenthesizedPayload]，从宿主源码切片以保留空白和注释。
     */
    val quoteTokens: CjQuoteTokens?
        get() = findChildByType<CjQuoteTokens>(CjNodeTypes.QUOTE_TOKENS)

    /** 当前输入是否实际包含任一括号 delimiter；不完整语法不能退化为声明输入。 */
    val hasParenthesisDelimiters: Boolean
        get() = findChildByType<PsiElement>(CjTokens.LPAR) != null ||
            findChildByType<PsiElement>(CjTokens.RPAR) != null

    /** 宏解析器是否成功接受了完整括号输入。 */
    val hasParenthesizedInput: Boolean
        get() = parenthesizedPayload != null && !CjMacroTokenPayload.hasDirectParserError(quoteTokens)

    /**
     * 返回完整括号输入的 payload 原文；缺少任一 delimiter 时返回 null。
     * 使用宿主源码切片可保留 PSI token tree 中未建节点的空白和注释。
     */
    val parenthesizedPayload: CjMacroTokenPayload?
        get() = delimitedPayload(CjTokens.LPAR, CjTokens.RPAR)

    /** 返回无括号声明输入的原文；括号形式必须使用 [parenthesizedPayload]。 */
    val declarationPayload: CjMacroTokenPayload?
        get() = if (hasParenthesisDelimiters) {
            null
        } else {
            CjMacroTokenPayload(text, textRange.startOffset)
        }

    private fun delimitedPayload(
        left: IElementType,
        right: IElementType,
    ): CjMacroTokenPayload? =
        CjMacroTokenPayload.fromDelimitedElement(this, left, right, quoteTokens)

    /**
     * 保存 `declarations`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val declarations: CjDeclaration?
        get() {
            return PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java).firstOrNull()
        }
    /**
     * 保存 `tokens`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tokens: List<PsiElement>
        get() = findChildByType<CjQuoteTokens>(CjNodeTypes.QUOTE_TOKENS)?.tokens ?: emptyList()
}

/**
 * 表示 `CjQuoteParameters`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjQuoteParameters(node: ASTNode) : CjExpressionImpl(node)
/**
 * 表示 `CjMacroAttr`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjMacroAttr(node: ASTNode) : CjExpressionImpl(node){
    /**
     * 宏方括号属性中的原始 token 容器；不包含外层 `[` 与 `]`。
     */
    val quoteTokens: CjQuoteTokens?
        get() = findChildByType<CjQuoteTokens>(CjNodeTypes.QUOTE_TOKENS)

    /** 返回完整方括号属性的 payload 原文；缺少任一 delimiter 时返回 null。 */
    val payload: CjMacroTokenPayload?
        get() = CjMacroTokenPayload.fromDelimitedElement(
            this,
            CjTokens.LBRACKET,
            CjTokens.RBRACKET,
            quoteTokens,
        )

    /**
     * 保存 `tokens`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tokens: List<PsiElement>
        get() = findChildByType<CjQuoteTokens>(CjNodeTypes.QUOTE_TOKENS)?.tokens ?: emptyList()

}
/**
 * 表示 `CjQuoteTokens`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CjQuoteTokens(node: ASTNode) : CjExpressionImpl(node) {

    /**
     * 保存 `tokens`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val tokens: List<PsiElement> get() = findChildrenByType(CangJieExpressionParsing.QUOTE_TOKENS)
}
