package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 基于源码元素计算诊断标记范围的抽象策略。
 */
abstract class AbstractSourceElementPositioningStrategy {
    /**
     * 为给定诊断计算应高亮的文本范围列表。
     */
    abstract fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange>

    /**
     * 判断当前策略是否适用于给定源码元素。
     */
    abstract fun isValid(element: AbstractCjSourceElement): Boolean
}

