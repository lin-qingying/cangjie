package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer

object CaCallableReceiverRendererForSource {
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
