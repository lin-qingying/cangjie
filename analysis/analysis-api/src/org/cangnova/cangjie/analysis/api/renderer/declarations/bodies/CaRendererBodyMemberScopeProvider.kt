package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol

fun interface CaRendererBodyMemberScopeProvider {
    fun memberScope(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
    ): CaScope

    companion object {
        val ALL_DECLARED: CaRendererBodyMemberScopeProvider = CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
            with(analysisSession) { symbol.declaredMemberScope }
        }
    }
}
