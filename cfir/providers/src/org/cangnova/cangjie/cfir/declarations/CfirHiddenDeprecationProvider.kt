

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.getDeprecation
import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue

/**
 * 对齐 Kotlin `FirHiddenDeprecationProvider`。
 *
 * IDE / low-level 层统一通过 symbol 视图判断 `HIDDEN`，
 * 不再直接窥视 declaration 内部细节。
 */
open class CfirHiddenDeprecationProvider(val session: CfirSession) : CfirSessionComponent {
    open fun isDeprecationLevelHidden(symbol: CfirBasedSymbol<*>): Boolean = when (symbol) {
        is CfirCallableSymbol<*> ->
            symbol.getDeprecation(session.languageVersionSettings)?.all?.deprecationLevel == DeprecationLevelValue.HIDDEN

        is CfirClassLikeSymbol<*> ->
            symbol.getOwnDeprecation(session.languageVersionSettings)?.all?.deprecationLevel == DeprecationLevelValue.HIDDEN

        else -> false
    }
}

val CfirSession.hiddenDeprecationProvider: CfirHiddenDeprecationProvider by CfirSession.sessionComponentAccessor()
