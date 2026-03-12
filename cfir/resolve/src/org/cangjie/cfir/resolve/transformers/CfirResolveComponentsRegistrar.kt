package org.cangjie.cfir.resolve.transformers

import org.cangjie.cfir.analysis.CheckersComponent
import org.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationCheckers
import org.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionCheckers
import org.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangjie.cfir.resolve.providers.CfirResolveProviderPipeline
import org.cangjie.cfir.resolve.services.CfirLazyDeclarationResolver
import org.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.scopes.CfirScopeSession

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
        val checkersComponent = CheckersComponent().apply {
            register(CfirBasicDeclarationCheckers)
            register(CfirBasicExpressionCheckers)
        }
        session.register(CheckersComponent::class, checkersComponent)

        CfirResolveProviderPipeline
            .formalDefaults()
            .registerInto(session)
        session.register(CfirLazyDeclarationResolver::class, CfirLazyDeclarationResolver())
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        registerResolveProcessors(registry, diagnosticReporter, session, CfirScopeSession())
    }
}

object CfirLegacyResolveComponentsRegistrar {
    fun register(
        session: CfirSession,
        registry: CfirPhaseResolverRegistry,
        diagnosticReporter: CfirDiagnosticReporter,
    ) {
        val checkersComponent = CheckersComponent().apply {
            register(CfirBasicDeclarationCheckers)
            register(CfirBasicExpressionCheckers)
        }
        session.register(CheckersComponent::class, checkersComponent)

        CfirResolveProviderPipeline
            .legacyCompatibleDefaults()
            .registerInto(session)
        session.register(CfirLazyDeclarationResolver::class, CfirLazyDeclarationResolver())
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        registerResolveProcessors(registry, diagnosticReporter, session, CfirScopeSession())
    }
}
