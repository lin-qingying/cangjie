package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

fun registerResolveProcessors(
    registry: CfirPhaseResolverRegistry,
    diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: ScopeSession = ScopeSession(),
) {
    registry.registerProcessor(
        CfirResolvePhase.IMPORTS,
        CfirImportResolveProcessor(diagnosticReporter, session, scopeSession),
    )
    registry.registerProcessor(
        CfirResolvePhase.SUPER_TYPES,
        CfirSupertypeResolverProcessor(session, scopeSession),
    )
    registry.registerProcessor(CfirResolvePhase.TYPES, CfirTypeResolveProcessor(session, scopeSession))
    registry.registerProcessor(
        CfirResolvePhase.STATUS,
        CfirStatusResolveProcessor(session, scopeSession),
    )
    registry.registerProcessor(
        CfirResolvePhase.EXTENSIONS,
        CfirExtensionsResolveProcessor(session, scopeSession),
    )
    registry.registerProcessor(
        CfirResolvePhase.IMPLICIT_TYPES,
        CfirImplicitTypesResolveProcessor(session, scopeSession),
    )
    registry.registerProcessor(CfirResolvePhase.BODY_RESOLVE, CfirBodyResolveProcessor(session, scopeSession))

}
