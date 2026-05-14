package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer

/**
 * 面向源码风格的接收者 renderer 预设。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableReceiverRendererForSource`。
 */
object CaCallableReceiverRendererForSource {
    /**
     * 预设: 把接收者写成 `Type.` 形式;
     * 类型先经过 [CaDeclarationRenderer.declarationTypeApproximator] 近似化, 便于显示。
     */
    val AS_TYPE_WITH_IN_APPROXIMATION: CaCallableReceiverRenderer = CaCallableReceiverRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val receiverType = symbol.receiverType ?: return@CaCallableReceiverRenderer
        printer {
            declarationRenderer.typeRenderer.renderType(
                analysisSession,
                declarationRenderer.declarationTypeApproximator.approximateType(
                    analysisSession,
                    receiverType,
                ),
                this,
            )
            append(".")
        }
    }
}
