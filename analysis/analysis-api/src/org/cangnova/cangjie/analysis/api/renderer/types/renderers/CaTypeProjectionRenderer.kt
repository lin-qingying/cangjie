package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection

fun interface CaTypeProjectionRenderer {
    fun renderTypeProjection(
        analysisSession: CaSession,
        projection: CaTypeProjection,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val WITH_TYPE_ARGUMENTS: CaTypeProjectionRenderer = CaTypeProjectionRenderer { analysisSession, projection, typeRenderer, printer ->
            projection.type?.let { type ->
                typeRenderer.renderType(analysisSession, type, printer)
            }
        }

        val NONE: CaTypeProjectionRenderer = CaTypeProjectionRenderer { _, _, _, _ -> }
    }
}
