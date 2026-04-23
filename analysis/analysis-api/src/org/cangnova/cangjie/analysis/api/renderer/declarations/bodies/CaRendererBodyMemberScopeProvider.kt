package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol

fun interface CaRendererBodyMemberScopeProvider {

    public fun getMemberScope(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol
    ): List<CaDeclarationSymbol>

    companion object {
        val ALL_DECLARED: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
                with(analysisSession) {
                    symbol.combinedDeclaredMemberScope.declarations
                        .filter { member ->
                            val origin = member.origin
//                            origin != CaSymbolOrigin.DELEGATED &&
//                                    origin != CaSymbolOrigin.SOURCE_MEMBER_GENERATED &&
//                                    origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE &&
                                    origin != CaSymbolOrigin.INTERSECTION_OVERRIDE
                        }.filter { member ->
                            member !is CaConstructorSymbol || symbol !is CaClassSymbol
                        }.filterNot { member ->
                            member is CaConstructorSymbol
                        }
                        .toList()
                }
            }
    }
}
