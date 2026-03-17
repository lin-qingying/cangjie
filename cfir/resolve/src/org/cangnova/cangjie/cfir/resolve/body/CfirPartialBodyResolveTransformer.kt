package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode

/**
 * Body 瑙ｆ瀽閮ㄥ垎 transformer锛屽鎵?context/components 鍒?dispatcher銆? *
 * 鎵€鏈夊叿浣撶殑瀛?transformer锛堝 [CfirExpressionsResolveTransformer]锛? * 閮介€氳繃姝ょ被闂存帴鑾峰彇 dispatcher 鎸佹湁鐨?context 鍜?components銆? *
 * 鍙傝€?K2 FirPartialBodyResolveTransformer銆? */
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

