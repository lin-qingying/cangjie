package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

object CaSuperTypeRendererForSource {
    val WITH_OUT_APPROXIMATION: CaSuperTypeRenderer = CaSuperTypeRenderer { analysisSession, superType, declarationRenderer, printer ->
        printer {
            declarationRenderer.typeRenderer.renderType(
                analysisSession,
                declarationRenderer.declarationTypeApproximator.approximateType(
                    analysisSession,
                    superType,
                ),
                this,
            )
        }
    }
}
