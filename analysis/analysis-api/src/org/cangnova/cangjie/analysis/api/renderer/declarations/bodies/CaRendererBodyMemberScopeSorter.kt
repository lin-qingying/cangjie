package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.name

fun interface CaRendererBodyMemberScopeSorter {
    fun sort(symbols: List<CaDeclarationSymbol>): List<CaDeclarationSymbol>

    companion object {
        val BY_NAME: CaRendererBodyMemberScopeSorter = CaRendererBodyMemberScopeSorter { symbols ->
            symbols.sortedBy { symbol -> symbol.name?.asString().orEmpty() }
        }

        val ENUM_CONSTRUCTORS_AT_BEGINNING: CaRendererBodyMemberScopeSorter = CaRendererBodyMemberScopeSorter { symbols ->
            symbols.sortedWith(
                compareBy<CaDeclarationSymbol>(
                    { if (it is CaEnumConstructorSymbol) 0 else 1 },
                    { it.name?.asString().orEmpty() },
                ),
            )
        }
    }
}
