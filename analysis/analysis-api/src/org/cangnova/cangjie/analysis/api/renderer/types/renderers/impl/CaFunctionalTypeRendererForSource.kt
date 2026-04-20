package org.cangnova.cangjie.analysis.api.renderer.types.renderers.impl

import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaFunctionalTypeKindRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaFunctionalTypeParameterListRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaFunctionalTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaFunctionalTypeReturnTypeRenderer

/**
 * 仓颉函数类型 renderer preset。
 */
object CaFunctionalTypeRendererForSource {
    val WITH_KIND_KEYWORDS: CaFunctionalTypeRenderer = CaFunctionalTypeRenderer { analysisSession, type, renderer, printer ->
        CaFunctionalTypeKindRenderer.WITH_KIND_KEYWORDS.renderKind(renderer, type, printer)
        CaFunctionalTypeParameterListRenderer.AS_SOURCE.renderParameters(analysisSession, renderer, type, printer)
        CaFunctionalTypeReturnTypeRenderer.AS_SOURCE.renderReturnType(analysisSession, renderer, type, printer)
    }

    val WITHOUT_KIND_KEYWORDS: CaFunctionalTypeRenderer = CaFunctionalTypeRenderer { analysisSession, type, renderer, printer ->
        CaFunctionalTypeKindRenderer.NO_KEYWORDS.renderKind(renderer, type, printer)
        CaFunctionalTypeParameterListRenderer.AS_SOURCE.renderParameters(analysisSession, renderer, type, printer)
        CaFunctionalTypeReturnTypeRenderer.AS_SOURCE.renderReturnType(analysisSession, renderer, type, printer)
    }
}
