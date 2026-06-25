package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.FqName

/**
 * use-site member scope 的 ScopeSession 缓存 key。
 *
 * @property useSiteSession 查询发生的 session。
 * @property classSymbol 被查询成员的 class symbol。
 * @property useSitePackage 当前 use-site 文件包名，用于 private/protected extend 可见性。
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
    /**
     * 当前 use-site 文件包名，用于 private/protected extend 可见性。
     */
    val useSitePackage: FqName?,
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
