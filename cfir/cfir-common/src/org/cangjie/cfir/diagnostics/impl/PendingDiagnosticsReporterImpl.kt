package org.cangjie.cfir.diagnostics.impl

import org.cangjie.AbstractCjSourceElement
import org.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangjie.cfir.diagnostics.PendingDiagnosticReporter

class PendingDiagnosticsReporterImpl(private val delegate: DiagnosticReporter) : PendingDiagnosticReporter() {
    private val pendingDiagnosticsByFilePath: MutableMap<String, MutableList<CjDiagnostic>> = mutableMapOf()

    override val hasErrors: Boolean
        get() = delegate.hasErrors

    override val hasWarningsForWError: Boolean
        get() = delegate.hasWarningsForWError

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        when (val filePath = context.containingFilePath) {
            null -> delegate.report(diagnostic, context)
            else -> {
                if (context.isDiagnosticSuppressed(diagnostic)) return
                val pendingDiagnostics = pendingDiagnosticsByFilePath.getOrPut(filePath) { mutableListOf() }
                pendingDiagnostics.add(diagnostic)
            }
        }
    }

    override fun checkAndCommitReportsOn(
        element: AbstractCjSourceElement,
        context: DiagnosticContext,
        commitEverything: Boolean,
    ) {
        if (pendingDiagnosticsByFilePath.isEmpty()) return
        val pathFromContext = context.containingFilePath
        val pendingIterator = pendingDiagnosticsByFilePath.iterator()
        while (pendingIterator.hasNext()) {
            val (path, pendingList) = pendingIterator.next()
            assert(pathFromContext == null || path == pathFromContext) {
                "Pending diagnostics for file $path are commited on file $pathFromContext"
            }

            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                val diagnosticElement = (diagnostic as? CjDiagnosticWithSource)?.element
                when {
                    context.isDiagnosticSuppressed(diagnostic) -> {
                        if (diagnosticElement != null &&
                            (diagnosticElement == element ||
                                    diagnosticElement.startOffset >= element.startOffset && diagnosticElement.endOffset <= element.endOffset)
                        ) {
                            iterator.remove()
                        }
                    }
                    diagnosticElement == element || commitEverything -> {
                        iterator.remove()
                        delegate.report(diagnostic, context)
                    }
                }
            }
            if (pendingList.isEmpty()) {
                pendingIterator.remove()
            }
        }
    }
}


