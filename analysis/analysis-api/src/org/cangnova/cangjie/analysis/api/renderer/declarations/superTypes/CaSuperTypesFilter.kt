package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

fun interface CaSuperTypesFilter {
    fun shouldRenderSuperType(
        analysisSession: CaSession,
        owner: CaClassSymbol,
        superType: CaType,
    ): Boolean

    companion object {
        val ALL: CaSuperTypesFilter = CaSuperTypesFilter { _, _, _ -> true }
        val NONE: CaSuperTypesFilter = CaSuperTypesFilter { _, _, _ -> false }
    }
}
