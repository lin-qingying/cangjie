package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

/**
 * PSI 诊断定位策略基类。
 */
open class PositioningStrategy<in E : PsiElement> {
    /**
     * 根据有源码诊断对象计算文本范围。
     */
    open fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange> {
        @Suppress("UNCHECKED_CAST")
        return mark(diagnostic.psiElement as E)
    }

    /**
     * 根据 PSI 元素计算文本范围。
     */
    open fun mark(element: E): List<TextRange> {
        return markElement(element)
    }

    /**
     * 判断 PSI 元素是否可作为有效诊断位置。
     */
    open fun isValid(element: E): Boolean {
        return !hasSyntaxErrors(element)
    }
}

/**
 * 标记单个 PSI 元素。
 */
fun markElement(element: PsiElement): List<TextRange> {
    return listOf(TextRange(getStartOffset(element), getEndOffset(element)))
}

/**
 * 返回单个 PSI 元素的 TextRange。
 */
fun markSingleElement(element: PsiElement): TextRange {
    return TextRange(getStartOffset(element), getEndOffset(element))
}

/**
 * 将已有 TextRange 包装为诊断范围列表。
 */
fun markRange(range: TextRange): List<TextRange> {
    return listOf(range)
}

/**
 * 标记两个 PSI 元素之间的范围。
 */
fun markRange(from: PsiElement, to: PsiElement): List<TextRange> {
    return markRange(TextRange(getStartOffset(from), getEndOffset(to)))
}

/**
 * 递归计算 PSI 元素跳过前导空白和注释后的起始偏移。
 */
private fun getStartOffset(element: PsiElement): Int {
    var child = element.firstChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.nextSibling
        }
        if (child != null) {
            return getStartOffset(child)
        }
    }
    return element.textRange.startOffset
}

/**
 * 递归计算 PSI 元素跳过尾随空白和注释后的结束偏移。
 */
private fun getEndOffset(element: PsiElement): Int {
    var child = element.lastChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.prevSibling
        }
        if (child != null) {
            return getEndOffset(child)
        }
    }
    return element.textRange.endOffset
}

/**
 * 判断 PSI 元素或其最后子节点链上是否包含语法错误。
 */
fun hasSyntaxErrors(psiElement: PsiElement): Boolean {
    if (psiElement is PsiErrorElement) return true

    val children = psiElement.children
    return children.isNotEmpty() && hasSyntaxErrors(children.last())
}
