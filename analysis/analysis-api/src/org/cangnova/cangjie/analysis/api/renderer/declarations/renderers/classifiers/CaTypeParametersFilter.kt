package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

fun interface CaTypeParametersFilter {
    fun shouldRenderTypeParameter(
        analysisSession: CaSession,
        owner: CaTypeParameterOwnerSymbol,
        typeParameter: CaTypeParameterSymbol,
    ): Boolean

    companion object {
        val ALL: CaTypeParametersFilter = CaTypeParametersFilter { _, _, _ -> true }
        val NONE: CaTypeParametersFilter = CaTypeParametersFilter { _, _, _ -> false }
    }
}
