package org.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import org.cangjie.AbstractCjSourceElement

abstract class AbstractSourceElementPositioningStrategy {
    abstract fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange>

    abstract fun isValid(element: AbstractCjSourceElement): Boolean
}


