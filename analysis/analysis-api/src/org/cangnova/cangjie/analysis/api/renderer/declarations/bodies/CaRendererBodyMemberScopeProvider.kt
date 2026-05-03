package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol

fun interface CaRendererBodyMemberScopeProvider {

    public fun getMemberScope(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol
    ): List<CaDeclarationSymbol>

    companion object {
        val ALL: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
                with(analysisSession) {
                    symbol.combinedDeclaredMemberScope.declarations.toList()
                }
            }

        val ALL_DECLARED: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
                with(analysisSession) {
                    symbol.combinedDeclaredMemberScope.declarations
                        .filter { member ->
                            val origin = member.origin
                            origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE_DECLARATION_SITE &&
                                    origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE_CALL_SITE &&
                                    origin != CaSymbolOrigin.INTERSECTION_OVERRIDE
                        }
                        .toList()
                }
            }

        val NONE: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { _, _ -> emptyList() }
    }
}
