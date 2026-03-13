package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic

/**
 * Standard implementation of [BaseDiagnosticsCollector]
 */
class DiagnosticsCollectorImpl : BaseDiagnosticsCollector() {
    private val diagnosticsByFilePathStorage: MutableMap<String?, MutableList<CjDiagnostic>> = mutableMapOf()

    override val diagnostics: List<CjDiagnostic>
        get() = diagnosticsByFilePath.flatMap { it.value }

    override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
        get() = diagnosticsByFilePathStorage

    override var hasErrors = false
        private set

    override var hasWarningsForWError = false
        private set

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            diagnosticsByFilePathStorage.getOrPut(context.containingFilePath) { mutableListOf() }.run {
                add(diagnostic)
                hasErrors = hasErrors || diagnostic.severity.isError
                hasWarningsForWError = hasWarningsForWError || diagnostic.severity.isErrorWhenWError
            }
        }
    }
}


