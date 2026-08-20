package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol

/**
 * use-site member scope 的 ScopeSession 缓存 key。
 *
 * @property useSiteSession 查询发生的 session。
 * @property classSymbol 被查询成员的 class symbol。
 */
data class CfirUseSiteMemberScopeKey(
    /**
     * 查询发生的 use-site session。
     */
    val useSiteSession: CfirSession,
    /**
     * 被查询成员的 class symbol。
     */
    val classSymbol: CfirClassSymbol,
)

/**
 * use-site member scope 的 ScopeSession key。
 */
val USE_SITE: ScopeSessionKey<CfirUseSiteMemberScopeKey, CfirTypeScope> = scopeSessionKey()

/**
 * 创建强类型 ScopeSession key。
 */
inline fun <reified ID : Any, reified FS : Any> scopeSessionKey(): ScopeSessionKey<ID, FS> {
    return object : ScopeSessionKey<ID, FS>() {}
}
