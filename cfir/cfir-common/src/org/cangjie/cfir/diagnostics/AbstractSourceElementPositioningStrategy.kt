package org.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import org.cangjie.cfir.source.AbstractCjSourceElement

abstract class AbstractSourceElementPositioningStrategy {
    abstract fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange>

    abstract fun isValid(element: AbstractCjSourceElement): Boolean
}


