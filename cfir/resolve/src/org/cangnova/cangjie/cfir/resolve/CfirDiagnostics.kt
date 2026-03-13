package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity

typealias CfirDiagnosticReporter = DiagnosticReporter

data class CfirResolvedDiagnostic(
    val factoryName: String,
    val message: String,
    val severity: Severity,
)

class CfirDiagnosticCollector : DiagnosticReporter() {
    private val storage = mutableListOf<CjDiagnostic>()

    val rawDiagnostics: List<CjDiagnostic>
        get() = storage

    val diagnostics: List<CfirResolvedDiagnostic>
        get() = storage.map {
            CfirResolvedDiagnostic(
                factoryName = it.factoryName,
                message = it.renderMessage(),
                severity = it.severity,
            )
        }

    override val hasErrors: Boolean
        get() = storage.any { it.severity.isError }

    override val hasWarningsForWError: Boolean
        get() = storage.any { it.severity.isErrorWhenWError }

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            storage += diagnostic
        }
    }
}
