package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Body 瑙ｆ瀽 transformer 鈥?鍏蜂綋 dispatcher銆? *
 * 钖勫３瀹炵幇锛屾寔鏈?context銆乧omponents 鍜屼袱涓瓙 transformer銆? * 鎵€鏈?transformXxx 鏂规硶閫氳繃 [CfirAbstractBodyResolveTransformerDispatcher]
 * 濮旀墭鍒板搴旂殑瀛?transformer銆? *
 * 鍙傝€?K2 FirBodyResolveTransformer銆? */
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

