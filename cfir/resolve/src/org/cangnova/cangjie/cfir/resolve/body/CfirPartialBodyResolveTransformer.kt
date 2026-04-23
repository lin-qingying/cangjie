package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext

/**
 * Body resolve 的局部 transformer 基类，负责把 `context` / `components`
 * 委托给所属 dispatcher。
 * 具体子 transformer 都通过它间接访问 dispatcher 持有的共享上下文。
 * 参考 K2 `FirPartialBodyResolveTransformer`。
 */
abstract class CfirPartialBodyResolveTransformer(
    val transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirAbstractBodyResolveTransformer(transformer.transformerPhase) {
    final override var implicitTypeOnly: Boolean
        get() = transformer.implicitTypeOnly
        set(value) {
            transformer.implicitTypeOnly = value
        }

    final override val context: BodyResolveContext
        get() = transformer.context

    final override val resolutionContext: ResolutionContext
        get() = transformer.resolutionContext

    final override val components: BodyResolveTransformerComponents
        get() = transformer.components

    override fun <E : CfirElement> transformElement(element: E, data: ResolutionMode): E {
        element.transformChildren(transformer, data)
        @Suppress("UNCHECKED_CAST")
        return element as E
    }
}
