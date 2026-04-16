package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaTupleType

fun interface CaTupleTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaTupleType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaTupleTypeRenderer = CaTupleTypeRenderer { analysisSession, type, typeRenderer, printer ->
            printer {
                printCollection(
                    type.elementTypes,
                    prefix = "(",
                    postfix = ")",
                ) { elementType ->
                    typeRenderer.renderType(analysisSession, elementType, this)
                }
            }
        }
    }
}
