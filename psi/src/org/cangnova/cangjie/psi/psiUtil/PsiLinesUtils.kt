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

package org.cangnova.cangjie.psi.psiUtil

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.abs
/**
 * 保存 `PsiElement.range`，供PSI 工具流程读取节点结构或语义信息。
 */
val PsiElement.range: TextRange get() = textRange ?: error(if (isPhysical) "No text range for $this" else "No text range is expected for non-physical element $this")
/**
 * 保存 `TextRange.start`，供PSI 工具流程读取节点结构或语义信息。
 */
val TextRange.start: Int get() = startOffset
/**
 * 保存 `TextRange.end`，供PSI 工具流程读取节点结构或语义信息。
 */
val TextRange.end: Int get() = endOffset

/**
 * 提供 `startLine` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.startLine(doc: Document): Int = doc.getLineNumber(range.start)
/**
 * 提供 `endLine` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.endLine(doc: Document): Int = doc.getLineNumber(range.end)
/**
 * 提供 `isWithCaret` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement?.isWithCaret(caret: Int) = this?.textRange?.contains(caret) == true

/**
 * 提供 `getLineStartOffset` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiFile.getLineStartOffset(line: Int): Int? {
    return getLineStartOffset(line, skipWhitespace = true)
}

/**
 * 提供 `getLineStartOffset` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiFile.getLineStartOffset(line: Int, skipWhitespace: Boolean): Int? {
    val doc = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this)
    if (doc != null && line >= 0 && line < doc.lineCount) {
        val startOffset = doc.getLineStartOffset(line)
        val element = findElementAt(startOffset) ?: return startOffset

        if (skipWhitespace && (element is PsiWhiteSpace || element is PsiComment)) {
            return PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java, PsiComment::class.java)?.startOffset ?: startOffset
        }
        return startOffset
    }

    return null
}

/**
 * 提供 `getLineEndOffset` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiFile.getLineEndOffset(line: Int): Int? {
    val document = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this)
    return document?.getLineEndOffset(line)
}

/**
 * 提供 `getLineNumber` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.getLineNumber(start: Boolean = true): Int {
    val document = containingFile.viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(containingFile)
    val index = if (start) this.startOffset else this.endOffset
    if (index > (document?.textLength ?: 0)) return 0
    return document?.getLineNumber(index) ?: 0
}

// Copied to formatter
/**
 * 提供 `getLineCount` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { file -> PsiDocumentManager.getInstance(project).getDocument(file) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength && spaceRange.startOffset < spaceRange.endOffset) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: error("Cannot count number of lines")) + 1
}

/**
 * 提供 `isMultiLine` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.isMultiLine() = getLineCount() > 1

/**
 * 提供 `isOneLiner` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.isOneLiner() = getLineCount() == 1

/**
 * 提供 `getLineCountInRange` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun Document.getLineCountInRange(textRange: TextRange): Int = abs(getLineNumber(textRange.startOffset) - getLineNumber(textRange.endOffset))

/**
 * 提供 `containsLineBreakInRange` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun Document.containsLineBreakInRange(textRange: TextRange): Boolean = getLineCountInRange(textRange) != 0
