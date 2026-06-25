package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Body resolve 的具体 dispatcher。
 * 它持有 `context`、`components` 以及两个子 transformer，
 * 所有 `transformXxx` 方法最终都会委托到对应子 transformer。
 * 参考 K2 `FirBodyResolveTransformer`。
 */
open class CfirBodyResolveTransformer(
    session: CfirSession,
    scopeSession: ScopeSession,
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculator.Default,
    outerBodyResolveContext: BodyResolveContext? = null,
    phase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE,
    /** 当前 dispatcher 是否只解析隐式类型所需路径。 */
    override var implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformerDispatcher(phase, implicitTypeOnly) {

    /** 当前 body resolve 上下文。 */
    override val context: BodyResolveContext = outerBodyResolveContext ?: BodyResolveContext(
        returnTypeCalculator = returnTypeCalculator,
        dataFlowAnalyzerContext = CfirDataFlowAnalyzerContext(),
    )

    /** 当前 dispatcher 暴露给子 transformer 的组件集合。 */
    override val components: BodyResolveTransformerComponents =
        BodyResolveTransformerComponents(
            session = session,
            scopeSession = scopeSession,
            transformer = this,
            context = context,
            expandTypeAliases = true,
        )

    final override val expressionsTransformer = CfirExpressionsResolveTransformer(this)
    final override val declarationsTransformer = CfirDeclarationsResolveTransformer(this)
}
