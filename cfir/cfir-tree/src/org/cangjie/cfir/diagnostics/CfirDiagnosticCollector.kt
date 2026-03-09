package org.cangjie.cfir.diagnostics

/**
 * 诊断列表收集实现。
 */
class CfirDiagnosticCollector : CfirDiagnosticReporter {

    private val _diagnostics = mutableListOf<CfirDiagnostic>()

    val diagnostics: List<CfirDiagnostic> get() = _diagnostics

    override fun report(diagnostic: CfirDiagnostic) {
        _diagnostics.add(diagnostic)
    }

    override val hasErrors: Boolean
        get() = _diagnostics.any { it.severity == CfirDiagnosticSeverity.ERROR }

    val size: Int get() = _diagnostics.size

    fun clear() = _diagnostics.clear()
}
