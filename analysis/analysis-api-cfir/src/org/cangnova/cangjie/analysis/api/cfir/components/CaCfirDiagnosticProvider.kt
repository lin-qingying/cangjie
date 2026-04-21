package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.plus
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 诊断组件。
 */
internal class CaCfirDiagnosticProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDiagnosticProvider, CaCfirSessionComponent {
    override fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            analysisSession.diagnosticQueries.queryDiagnostics(this@diagnostics, filter.asCfilter())
                .map { diagnostic -> diagnostic.asPublicDiagnostic() }
        }

    override fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            analysisSession.diagnosticQueries.queryFileDiagnostics(this@collectDiagnostics, filter.asCfilter())
                .map { diagnostic -> diagnostic.asPublicDiagnostic() }
        }

    private fun CaDiagnosticCheckerFilter.asCfilter(): DiagnosticCheckerFilter = when (this) {
        CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS
        CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS ->
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
    }
}
