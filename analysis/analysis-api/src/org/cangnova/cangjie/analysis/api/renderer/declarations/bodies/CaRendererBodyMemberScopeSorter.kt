package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.name

fun interface CaRendererBodyMemberScopeSorter {
    public fun sortMembers(
        analysisSession: CaSession,
        members: List<CaDeclarationSymbol>,
        container: CaDeclarationContainerSymbol,
    ): List<CaDeclarationSymbol>

    companion object {


        val ENUM_CONSTRUCTORS_AT_BEGINNING: CaRendererBodyMemberScopeSorter = CaRendererBodyMemberScopeSorter {
                _: CaSession,
                members: List<CaDeclarationSymbol>,
                _: CaDeclarationContainerSymbol,
            -> members.sortedBy { it !is CaEnumConstructorSymbol }
        }
    }
}
