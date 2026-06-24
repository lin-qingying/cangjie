package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.util.PrivateForInline

/**
 * scope 级缓存会话。
 *
 * 同一个 [ScopeSession] 内会复用以声明或类型为 id 的 scope 计算结果，避免重复构建成员 scope。
 */
class ScopeSession {
    /**
     * 按 id 和 key 双层索引的 scope 缓存。
     */
    private val scopes: HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = hashMapOf()

    /**
     * 为 inline 调用点暴露底层缓存表。
     */
    @PrivateForInline
    fun scopes(): HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = scopes

    /**
     * 获取或构建 [id] + [key] 对应的 scope 结果。
     */
    @OptIn(PrivateForInline::class)
    inline fun <reified ID : Any, reified FS : Any> getOrBuild(id: ID, key: ScopeSessionKey<ID, FS>, build: () -> FS): FS {
        return scopes().getOrPut(id) {
            hashMapOf()
        }.getOrPut(key) {
            build()
        } as FS
    }
}

/**
 * [ScopeSession] 缓存项的强类型键。
 */
abstract class ScopeSessionKey<ID : Any, FS : Any>
