package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode

/**
 * Body 解析部分 transformer，委托 context/components 到 dispatcher。
 *
 * 所有具体的子 transformer（如 [CfirExpressionsResolveTransformer]）
 * 都通过此类间接获取 dispatcher 持有的 context 和 components。
 *
 * 参考 K2 FirPartialBodyResolveTransformer。
 */
abstract class CfirPartialBodyResolveTransformer(
    val transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirAbstractBodyResolveTransformer(transformer.transformerPhase) {

    final override val context: CfirBodyResolveContext
        get() = transformer.context

    final override val components: BodyResolveTransformerComponents
        get() = transformer.components

    override fun <E : CfirElement> transformElement(element: E, data: CfirResolutionMode): E {
        element.transformChildren(transformer, data)
        @Suppress("UNCHECKED_CAST")
        return element as E
    }
}
