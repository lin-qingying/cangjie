package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
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

fun interface CaRendererOtherModifiersProvider {
    fun getOtherModifiers(symbol: CaDeclarationSymbol): List<String>

    companion object {
        val ALL: CaRendererOtherModifiersProvider = CaRendererOtherModifiersProvider { symbol ->
            buildList {
                addAll(symbol.renderedOtherModifiers(includeConst = true, includeMutating = true))
            }
        }

        val DECLARATION_PREFIX_ONLY: CaRendererOtherModifiersProvider = CaRendererOtherModifiersProvider { symbol ->
            buildList {
                addAll(symbol.renderedOtherModifiers(includeConst = false, includeMutating = false))
            }
        }
    }
}

private fun CaDeclarationSymbol.renderedOtherModifiers(
    includeConst: Boolean,
    includeMutating: Boolean,
): List<String> = when (this) {
    is CaFunctionSymbol -> buildList {
        if (isStatic) add("static")
        if (includeConst && isConst) add("const")
        if (includeMutating && isMutating) add("mut")
        if (isOverride) add("override")
        if (isOperator) add("operator")
        if (isUnsafe) add("unsafe")
        if (isForeign) add("foreign")
    }

    is CaPropertySymbol -> buildList {
        if (isStatic) add("static")
        if (includeConst && isConst) add("const")
        if (includeMutating && isMutating) add("mut")
        if (isOverride) add("override")
        if (isUnsafe) add("unsafe")
        if (isForeign) add("foreign")
    }

    is CaFieldSymbol -> buildList {
        if (isStatic) add("static")
        if (includeConst && isConst) add("const")
    }

    else -> emptyList()
}
