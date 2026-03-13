package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

internal class CfirImplicitTypesResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
) {
    override val transformer: CfirImplicitTypesResolveTransformer =
        CfirImplicitTypesResolveTransformer(session)
}

internal class CfirBodyResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.BODY_RESOLVE,
) {
    override val transformer: CfirBodyResolveTransformer =
        CfirBodyResolveTransformer(session)
}

internal class CfirImplicitTypesResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.IMPLICIT_TYPES) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS && declaration.resolvePhase < CfirResolvePhase.IMPLICIT_TYPES) {
            declaration.resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
        }
        return declaration
    }
}

internal class CfirBodyResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.BODY_RESOLVE) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES && declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
            declaration.resolvePhase = CfirResolvePhase.BODY_RESOLVE
        }
        return declaration
    }
}
