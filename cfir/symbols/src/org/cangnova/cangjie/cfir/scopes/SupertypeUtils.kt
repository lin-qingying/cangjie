package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol

val USE_SITE: ScopeSessionKey<Pair<CfirSession, CfirClassSymbol>, CfirTypeScope> = scopeSessionKey()

inline fun <reified ID : Any, reified FS : Any> scopeSessionKey(): ScopeSessionKey<ID, FS> {
    return object : ScopeSessionKey<ID, FS>() {}
}
