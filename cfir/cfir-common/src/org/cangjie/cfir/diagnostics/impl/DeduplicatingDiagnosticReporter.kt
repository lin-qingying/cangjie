package org.cangjie.cfir.diagnostics.impl

import org.cangjie.AbstractCjSourceElement
import org.cangjie.cfir.diagnostics.*

class DeduplicatingDiagnosticReporter(private val delegate: DiagnosticReporter) : DiagnosticReporter() {
    override val hasErrors: Boolean get() = delegate.hasErrors
    override val hasWarningsForWError: Boolean get() = delegate.hasWarningsForWError

    private val reported = mutableSetOf<Triple<String?, AbstractCjSourceElement, CjDiagnosticFactoryN>>()

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        when (diagnostic) {
            null -> {}
            is CjDiagnosticWithoutSource -> delegate.report(diagnostic, context)
            is CjDiagnosticWithSource -> {
                if (reported.add(Triple(context.containingFilePath, diagnostic.element, diagnostic.factory))) {
                    delegate.report(diagnostic, context)
                }
            }
        }
    }
}

fun DiagnosticReporter.deduplicating(): DeduplicatingDiagnosticReporter {
    if (this is DeduplicatingDiagnosticReporter) return this
    return DeduplicatingDiagnosticReporter(this)
}


