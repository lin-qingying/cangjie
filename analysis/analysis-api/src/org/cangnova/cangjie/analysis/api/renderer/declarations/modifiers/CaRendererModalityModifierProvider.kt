package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality

fun interface CaRendererModalityModifierProvider {
    fun getModalityModifier(symbol: CaDeclarationSymbol): String?

    companion object {
        val NO_IMPLICIT_MODALITY: CaRendererModalityModifierProvider = CaRendererModalityModifierProvider { symbol ->
            if (!symbol.isModalityExplicit) return@CaRendererModalityModifierProvider null
            when (symbol.modality) {
                CaSymbolModality.SEALED -> "sealed"
                CaSymbolModality.OPEN -> "open"
                CaSymbolModality.ABSTRACT -> "abstract"
                CaSymbolModality.FINAL,
                null,
                -> null
            }
        }

        val WITH_IMPLICIT_MODALITY: CaRendererModalityModifierProvider = CaRendererModalityModifierProvider { symbol ->
            when (symbol.modality) {
                CaSymbolModality.FINAL -> "final"
                CaSymbolModality.SEALED -> "sealed"
                CaSymbolModality.OPEN -> "open"
                CaSymbolModality.ABSTRACT -> "abstract"
                null -> null
            }
        }
    }
}
