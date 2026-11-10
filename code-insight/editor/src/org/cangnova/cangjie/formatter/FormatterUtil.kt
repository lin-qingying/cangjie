/*
 * Copyright 2025 LinQingYing. and contributors.
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


package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.*
import com.intellij.formatting.ASTBlock
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings

import com.intellij.psi.util.PsiUtilCore


/**
 * 返回 ASTBlock 绑定的 ASTNode，缺失时视为 formatter 不变量破坏。
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")


/**
 * 计算 PSI 元素覆盖的文本行数。
 */
fun PsiElement.getLineCount(): Int {
    val spaceRange = textRange ?: TextRange.EMPTY_RANGE
    return getLineCountByDocument(spaceRange.startOffset, spaceRange.endOffset)
        ?: (StringUtil.getLineBreakCount(text ?: error("Cannot count number of lines")) + 1)
}

/**
 * 优先通过文档行号计算指定偏移范围的行数。
 */
fun PsiElement.getLineCountByDocument(startOffset: Int, endOffset: Int): Int? {
    val doc = containingFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) } ?: return null
    if (endOffset > doc.textLength || startOffset >= endOffset) return null

    val startLine = doc.getLineNumber(startOffset)
    val endLine = doc.getLineNumber(endOffset)

    return endLine - startLine + 1
}

/**
 * 判断 PSI 元素文本是否跨多行。
 */
fun PsiElement.isMultiline() = getLineCount() > 1

/**
 * 判断 PSI 元素是否为空白换行。
 */
fun PsiElement?.isLineBreak() = this is PsiWhiteSpace && StringUtil.containsLineBreak(text)

/**
 * 查找前后方向上第一个非空白叶子。
 */
fun PsiElement.leafIgnoringWhitespace(forward: Boolean = true, skipEmptyElements: Boolean = true) =
    leaf(forward) { (!skipEmptyElements || it.textLength != 0) && it !is PsiWhiteSpace }

/**
 * 查找前后方向上第一个非空白且非注释叶子。
 */
fun PsiElement.leafIgnoringWhitespaceAndComments(forward: Boolean = true, skipEmptyElements: Boolean = true) =
    leaf(forward) { (!skipEmptyElements || it.textLength != 0) && it !is PsiWhiteSpace && it !is PsiComment }

/**
 * 按方向查找满足过滤条件的叶子节点。
 */
fun PsiElement.leaf(forward: Boolean = true, filter: (PsiElement) -> Boolean): PsiElement? =
    if (forward) nextLeaf(filter)
    else prevLeaf(filter)

/**
 * 判断 PSI 元素是否为逗号 token。
 */
val PsiElement.isComma: Boolean get() =PsiUtilCore. getElementType(this) == CjTokens.COMMA

/**
 * 判断指定全局偏移范围内是否包含换行。
 */
fun PsiElement.containsLineBreakInChild(globalStartOffset: Int, globalEndOffset: Int): Boolean =
    getLineCountByDocument(globalStartOffset, globalEndOffset)?.let { it > 1 }
        ?: firstChild.siblings(forward = true, withItself = true)
            .dropWhile { it.startOffset < globalStartOffset }
            .takeWhile { it.endOffset <= globalEndOffset }
            .any { it.textContains('\n') || it.textContains('\r') }

/**
 * 将预定义仓颉代码风格应用到 custom settings。
 */
fun applyCangJieCodeStyle(
    codeStyleId: String?,
    codeStyleSettings: CangJieCodeStyleSettings,
    modifyCodeStyle: Boolean = true
) = when (codeStyleId) {
    CangJieStyleGuideCodeStyle.CODE_STYLE_ID -> CangJieStyleGuideCodeStyle.applyToCangJieCustomSettings(codeStyleSettings, modifyCodeStyle)
    CangJieObsoleteCodeStyle.CODE_STYLE_ID -> CangJieObsoleteCodeStyle.applyToCangJieCustomSettings(codeStyleSettings, modifyCodeStyle)
    else -> Unit
}

/**
 * 将预定义仓颉代码风格应用到 common settings。
 */
fun applyCangJieCodeStyle(
    codeStyleId: String?,
    codeStyleSettings: CommonCodeStyleSettings,
    modifyCodeStyle: Boolean = true
) = when (codeStyleId) {
    CangJieStyleGuideCodeStyle.CODE_STYLE_ID -> CangJieStyleGuideCodeStyle.applyToCommonSettings(codeStyleSettings, modifyCodeStyle)
    CangJieObsoleteCodeStyle.CODE_STYLE_ID -> CangJieObsoleteCodeStyle.applyToCommonSettings(codeStyleSettings, modifyCodeStyle)
    else -> Unit
}

/**
 * 将预定义仓颉代码风格应用到完整 CodeStyleSettings。
 */
fun applyCangJieCodeStyle(codeStyleId: String?, codeStyleSettings: CodeStyleSettings): Boolean {
    when (codeStyleId) {
        CangJieStyleGuideCodeStyle.CODE_STYLE_ID -> CangJieStyleGuideCodeStyle.apply(codeStyleSettings)
        CangJieObsoleteCodeStyle.CODE_STYLE_ID -> CangJieObsoleteCodeStyle.apply(codeStyleSettings)
        else -> return false
    }

    return true
}
