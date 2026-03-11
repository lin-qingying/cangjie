package org.cangjie.cfir.resolve

import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.providers.CfirResolveProviderPipeline
import org.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
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
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        registerResolveProcessors(registry, diagnosticReporter)
    }
}

object CfirLegacyResolveComponentsRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
    ) {
        CfirResolveProviderPipeline
            .legacyCompatibleDefaults()
            .registerInto(session)
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        registerResolveProcessors(registry, diagnosticReporter)
    }
}
