package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

typealias CfirDiagnosticReporter = DiagnosticReporter

/**
 * 璇婃柇涓婃姤鍣ㄧ殑浼氳瘽缁勪欢鍖呰銆? *
 * 瀵归綈 Kotlin: 璇婃柇鑳藉姏鐢?session 鎸佹湁锛岃€岄潪鍦?resolve 缁勪欢娉ㄥ唽鏃跺崟鐙紶鍙傘€? */
class CfirDiagnosticReporterComponent(
    val reporter: CfirDiagnosticReporter,
) : CfirSessionComponent

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

