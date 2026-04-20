package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaUnionType

fun interface CaUnionTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaUnionType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_UNION: CaUnionTypeRenderer = CaUnionTypeRenderer { analysisSession, type, typeRenderer, printer ->
            printer {
                printCollection(
                    type.alternatives,
                    separator = " | ",
                ) { alternative ->
                    typeRenderer.renderType(analysisSession, alternative, this)
                }
            }
        }
    }
}
