package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType

fun interface CaIntersectionTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaIntersectionType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_INTERSECTION: CaIntersectionTypeRenderer = CaIntersectionTypeRenderer { analysisSession, type, typeRenderer, printer ->
            printer {
                printCollection(
                    type.conjuncts,
                    separator = " & ",
                ) { conjunct ->
                    typeRenderer.renderType(analysisSession, conjunct, printer)
                }
            }
        }
    }
}
