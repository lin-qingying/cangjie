package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType

interface CaTypeParameterTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaTypeParameterType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    object AS_SOURCE : CaTypeParameterTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaTypeParameterType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                    {
                        typeRenderer.typeNameRenderer.renderName(analysisSession, type.name, type, typeRenderer, printer)
                        with(analysisSession) {
                        }
                    },
                )
            }
        }
    }
}
