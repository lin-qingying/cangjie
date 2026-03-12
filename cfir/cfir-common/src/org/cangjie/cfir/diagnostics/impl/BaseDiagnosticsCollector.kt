package org.cangjie.cfir.diagnostics.impl

import org.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangjie.cfir.diagnostics.CjDiagnostic

/**
 * [BaseDiagnosticsCollector] is a [DiagnosticReporter] which stores all reported diagnostics inside itself.
 */
abstract class BaseDiagnosticsCollector : DiagnosticReporter() {
    abstract val diagnostics: List<CjDiagnostic>
    abstract val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>

    object DoNothing : BaseDiagnosticsCollector() {
        override val diagnostics: List<CjDiagnostic>
            get() = emptyList()
        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
            get() = emptyMap()

        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {}

        override val hasErrors: Boolean
            get() = false
        override val hasWarningsForWError: Boolean
            get() = false
    }
}


