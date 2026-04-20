package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer

object CaCallableReturnTypeRendererForSource {
    val WITH_OUT_APPROXIMATION: CaCallableReturnTypeRenderer = CaCallableReturnTypeRenderer { analysisSession, symbol, declarationRenderer, printer ->
        if (!declarationRenderer.returnTypeFilter.shouldRenderReturnType(analysisSession, symbol)) return@CaCallableReturnTypeRenderer
        val renderedType = prettyPrint {
            declarationRenderer.typeRenderer.renderType(
                analysisSession,
                declarationRenderer.declarationTypeApproximator.approximateType(
                    analysisSession,
                    symbol.returnType,
                ),
                this,
            )
        }
        printer {
            append(if (declarationRenderer.codeStyle.spaceAfterColon) ": " else ":")
            append(renderedType)
        }
    }
}
