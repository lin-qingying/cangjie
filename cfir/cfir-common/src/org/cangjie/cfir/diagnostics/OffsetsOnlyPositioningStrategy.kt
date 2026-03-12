package org.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import org.cangjie.cfir.source.AbstractCjSourceElement

open class OffsetsOnlyPositioningStrategy : AbstractSourceElementPositioningStrategy() {
    open fun markCjDiagnostic(element: AbstractCjSourceElement, diagnostic: CjDiagnostic): List<TextRange> {
        return mark(element.startOffset, element.endOffset)
    }

    open fun mark(
        startOffset: Int,
        endOffset: Int,
    ): List<TextRange> {
        return markElement(startOffset, endOffset)
    }

    override fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange> {
        return markCjDiagnostic(diagnostic.element, diagnostic)
    }

    override fun isValid(element: AbstractCjSourceElement): Boolean = true
}

fun markElement(
    startOffset: Int,
    endOffset: Int,
): List<TextRange> = markRange(startOffset, endOffset)

fun markRange(
    startOffset: Int,
    endOffset: Int,
): List<TextRange> {
    return listOf(markSingleElement(startOffset, endOffset))
}

fun markSingleElement(
    startOffset: Int,
    endOffset: Int,
): TextRange {
    return TextRange(startOffset, endOffset)
}


