package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession

internal fun createStubBodyResolveComponents(cfirSession: CfirSession): CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents {
    val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    val stubBodyResolveTransformer = CfirBodyResolveTransformer(
        session = cfirSession,
        phase = CfirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    return StubBodyResolveTransformerComponents(
        cfirSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    )
}

internal open class StubBodyResolveTransformerComponents(
    session: CfirSession,
    scopeSession: ScopeSession,
    transformer: CfirBodyResolveTransformer,
    context: BodyResolveContext,
) : CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
    session,
    scopeSession,
    transformer,
    context,
    expandTypeAliases = true,
)
