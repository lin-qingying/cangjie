package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.asCaDiagnostic
import org.cangnova.cangjie.analysis.api.cfir.resolve.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.cfir.resolve.plus
import org.cangnova.cangjie.analysis.api.components.*
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 实现的各 session component（对齐 Kotlin 的 KaFir* 系列）。
 *
 * 每个 component 通过 [analysisSessionProvider] 回引 [CaCfirSession]，
 * 以访问底层 CfirSession 和各种构建器。
 * 对齐 Kotlin 中 KaFirResolver(analysisSessionProvider) 的模式。
 */

internal class CaCfirResolver(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaResolver {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirSymbolRelationProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaSymbolRelationProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirDiagnosticProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDiagnosticProvider, CaCfirSessionComponent {

    override fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            resolutionFacade.getDiagnostics(this@diagnostics, filter.asLLFilter())
                .map { it.asCaDiagnostic(token) }
        }

    override fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            resolutionFacade.collectDiagnosticsForFile(this@collectDiagnostics, filter.asLLFilter())
                .map { it.asCaDiagnostic(token) }
        }

    private fun CaDiagnosticCheckerFilter.asLLFilter() = when (this) {
        CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS
        CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
    }
}

internal class CaCfirScopeProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaScopeProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirCompletionCandidateChecker(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaCompletionCandidateChecker {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirExpressionTypeProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaExpressionTypeProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirTypeProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaTypeProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirTypeInformationProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaTypeInformationProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirSymbolProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaSymbolProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirCInteropComponent(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaCInteropComponent {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirSymbolInformationProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaSymbolInformationProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirTypeRelationChecker(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaTypeRelationChecker {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirExpressionInformationProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaExpressionInformationProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirEvaluator(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaEvaluator {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirReferenceShortener(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaReferenceShortener {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirImportOptimizer(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaImportOptimizer {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirRenderer(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaRenderer {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirVisibilityChecker(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaVisibilityChecker {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirOriginalPsiProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaOriginalPsiProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirTypeCreator(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaTypeCreator {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirAnalysisScopeProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaAnalysisScopeProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirSignatureSubstitutor(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaSignatureSubstitutor {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirSubstitutorProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaSubstitutorProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirDataFlowProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaDataFlowProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirSourceProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaSourceProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}

internal class CaCfirDocProvider(
    private val analysisSessionProvider: () -> CaCfirSession,
) : CaDocProvider {
    override val token: CaLifetimeToken get() = analysisSessionProvider().token
}
