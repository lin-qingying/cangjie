package org.cangjie.cfir.resolve

import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.providers.CfirResolveProviderPipeline
import org.cangjie.cfir.session.CfirSession

/**
 * Formal CFIR resolve component registration entry.
 *
 * The provider chain is centralized in [CfirResolveProviderPipeline] so we can
 * evolve from legacy-compatible providers to full providers without changing call sites.
 */
object CfirResolveComponentsRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
    ) {
        CfirResolveProviderPipeline
            .formalDefaults()
            .registerInto(session)
        registerMinimalResolveProcessors(registry, diagnosticReporter)
    }
}

@Deprecated(
    message = "Use CfirResolveComponentsRegistrar for the formal CFIR_RESOLVE pipeline.",
    replaceWith = ReplaceWith("CfirResolveComponentsRegistrar"),
)
object CfirMinimalResolveComponentsRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
    ) {
        CfirResolveProviderPipeline
            .legacyCompatibleDefaults()
            .registerInto(session)
        registerMinimalResolveProcessors(registry, diagnosticReporter)
    }
}
