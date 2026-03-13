package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

abstract class AbstractSourceElementPositioningStrategy {
    abstract fun markDiagnostic(diagnostic: CjDiagnosticWithSource): List<TextRange>

    abstract fun isValid(element: AbstractCjSourceElement): Boolean
}


