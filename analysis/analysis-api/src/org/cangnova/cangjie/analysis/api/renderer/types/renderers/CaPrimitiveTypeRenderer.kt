package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType

fun interface CaPrimitiveTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaPrimitiveType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaPrimitiveTypeRenderer =
            CaPrimitiveTypeRenderer { analysisSession, type, typeRenderer, printer ->
                printer {
                    " ".separated(
                        { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this) },
                        {
                            append(type.kind.typeName)
                        },
                    )
                }
            }
    }
}
