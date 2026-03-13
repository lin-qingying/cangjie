package org.cangnova.cangjie.cfir.scopes

import java.util.concurrent.ConcurrentHashMap

/**
 * Scope 计算记忆化缓存。
 *
 * 避免重复构建 scope，按 key 缓存已构建的 scope 实例。
 * 参考 K2 FirScopeSession。
 */
class CfirScopeSession {

    private val cache = ConcurrentHashMap<Any, CfirScope>()

    fun getOrBuild(key: Any, builder: () -> CfirScope): CfirScope =
        cache.getOrPut(key, builder)

    val size: Int get() = cache.size

    fun clear() = cache.clear()
}
