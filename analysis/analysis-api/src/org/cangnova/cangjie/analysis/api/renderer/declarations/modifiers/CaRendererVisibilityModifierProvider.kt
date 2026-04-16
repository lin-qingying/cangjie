package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility

fun interface CaRendererVisibilityModifierProvider {
    fun getVisibilityModifier(symbol: CaDeclarationSymbol): String?

    companion object {
        val NO_IMPLICIT_VISIBILITY: CaRendererVisibilityModifierProvider = CaRendererVisibilityModifierProvider { symbol ->
            if (!symbol.isVisibilityExplicit) return@CaRendererVisibilityModifierProvider null
            when (symbol.visibility) {
                CaSymbolVisibility.PRIVATE -> "private"
                CaSymbolVisibility.PRIVATE_TO_THIS -> "private"
                CaSymbolVisibility.PROTECTED -> "protected"
                CaSymbolVisibility.INTERNAL -> "internal"
                CaSymbolVisibility.LOCAL -> "local"
                CaSymbolVisibility.PUBLIC,
                CaSymbolVisibility.UNKNOWN,
                -> null
            }
        }

        val WITH_IMPLICIT_VISIBILITY: CaRendererVisibilityModifierProvider = CaRendererVisibilityModifierProvider { symbol ->
            when (symbol.visibility) {
                CaSymbolVisibility.PRIVATE -> "private"
                CaSymbolVisibility.PRIVATE_TO_THIS -> "private"
                CaSymbolVisibility.PROTECTED -> "protected"
                CaSymbolVisibility.INTERNAL -> "internal"
                CaSymbolVisibility.LOCAL -> "local"
                CaSymbolVisibility.PUBLIC,
                CaSymbolVisibility.UNKNOWN,
                -> "public"
            }
        }
    }
}
