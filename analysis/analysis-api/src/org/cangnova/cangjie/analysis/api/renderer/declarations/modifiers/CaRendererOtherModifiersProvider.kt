package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol

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
