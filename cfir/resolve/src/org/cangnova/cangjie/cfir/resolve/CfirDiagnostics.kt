package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

typealias CfirDiagnosticReporter = DiagnosticReporter

/**
 * 诊断上报器的会话组件包装。
 * 对齐 Kotlin 的做法：诊断能力由 session 持有，而不是在 resolve 组件注册时单独传参。
 */
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

