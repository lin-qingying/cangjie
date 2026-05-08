package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

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
            when (symbol) {
                is CaPropertyAccessorSymbol,
                is CaParameterSymbol,
                is CaFieldSymbol,
                is CaTypeParameterSymbol,
                is CaConstructorSymbol,
                is CaEnumConstructorSymbol,
                is CaTypeAliasSymbol,
                is CaAnonymousFunctionSymbol,
                is CaLocalVariableSymbol,
                -> return@CaRendererModalityModifierProvider null

                else -> {}
            }

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
