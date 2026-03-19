package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Body resolve 的具体 dispatcher。
 * 它持有 `context`、`components` 以及两个子 transformer，
 * 所有 `transformXxx` 方法最终都会委托到对应子 transformer。
 * 参考 K2 `FirBodyResolveTransformer`。
 */
open class CfirBodyResolveTransformer(
    session: CfirSession,
    scopeSession: CfirScopeSession,
    returnTypeCalculator: CfirReturnTypeCalculator = CfirReturnTypeCalculator.Default,
    outerBodyResolveContext: CfirBodyResolveContext? = null,
    phase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE,
    override val implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformerDispatcher(phase, implicitTypeOnly) {

    override val context: CfirBodyResolveContext = outerBodyResolveContext ?: CfirBodyResolveContext(
        returnTypeCalculator = returnTypeCalculator,
        dataFlowAnalyzerContext = CfirDataFlowAnalyzerContext(),
    )

    override val components: BodyResolveTransformerComponents =
        BodyResolveTransformerComponents(session, scopeSession, this, context)

    final override val expressionsTransformer = CfirExpressionsResolveTransformer(this)
    final override val declarationsTransformer = CfirDeclarationsResolveTransformer(this)
}

