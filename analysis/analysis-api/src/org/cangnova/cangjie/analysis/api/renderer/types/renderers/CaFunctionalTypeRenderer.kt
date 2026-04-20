package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.impl.CaFunctionalTypeRendererForSource
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

fun interface CaFunctionalTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaFunctionType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFunctionalTypeRenderer = CaFunctionalTypeRendererForSource.WITH_KIND_KEYWORDS
    }
}
