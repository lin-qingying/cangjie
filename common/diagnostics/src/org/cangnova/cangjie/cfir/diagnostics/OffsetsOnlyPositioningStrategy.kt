package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 仅根据源码 offset 计算诊断范围的定位策略。
 */
open class OffsetsOnlyPositioningStrategy : AbstractSourceElementPositioningStrategy() {
    /**
     * 根据抽象源码元素的起止偏移计算诊断范围。
     */
    open fun markCjDiagnostic(element: AbstractCjSourceElement, diagnostic: CjDiagnostic): List<TextRange> {
        return mark(element.startOffset, element.endOffset)
    }

    /**
     * 根据起止偏移计算诊断范围。
     */
    open fun mark(
        startOffset: Int,
        endOffset: Int,
    ): List<TextRange> {
        return markElement(startOffset, endOffset)
    }

    /**
     * 根据有源码诊断对象计算 offset-only 范围。
     */
    override fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange> {
        return markCjDiagnostic(diagnostic.element, diagnostic)
    }

    /**
     * offset-only 源元素默认视为有效。
     */
    override fun isValid(element: AbstractCjSourceElement): Boolean = true
}

/**
 * 标记一个 offset-only 元素范围。
 */
fun markElement(
    startOffset: Int,
    endOffset: Int,
): List<TextRange> = markRange(startOffset, endOffset)

/**
 * 将起止偏移包装为诊断范围列表。
 */
fun markRange(
    startOffset: Int,
    endOffset: Int,
): List<TextRange> {
    return listOf(markSingleElement(startOffset, endOffset))
}

/**
 * 将起止偏移转换为单个 TextRange。
 */
fun markSingleElement(
    startOffset: Int,
    endOffset: Int,
): TextRange {
    return TextRange(startOffset, endOffset)
}

