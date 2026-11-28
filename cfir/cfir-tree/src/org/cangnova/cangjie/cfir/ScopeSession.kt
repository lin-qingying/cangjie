package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.util.PrivateForInline

/**
 * scope 级缓存会话。
 *
 * 非 owned 缓存沿用 id/key 双层存储；拥有完成代次的缓存必须显式登记所属 owner，
 * 由产品发布方精确退役该域，不能清空其它 session/provider 的缓存。
 */
class ScopeSession {
    private val scopes: HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = hashMapOf()
    private val ownedEntries = hashMapOf<ScopeSessionCacheOwner, MutableSet<CacheAddress>>()

    private data class CacheAddress(val id: Any, val key: ScopeSessionKey<*, *>)

    /** 为既有 inline 调用点保留底层表入口；语义缓存使用 owned API 登记和退役。 */
    @PrivateForInline
    fun scopes(): HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = scopes

    /** 创建绑定此物理存储的稳定缓存域；生产方应按实际来源分区复用此 owner。 */
    fun createCacheOwner(): ScopeSessionCacheOwner = ScopeSessionCacheOwner(this)

    /**
     * 仅退役此 owner 登记的条目并推进代次，返回实际移除数量。
     *
     * 进行中的 builder 可以完成计算，但其旧代产品不会再被写回；语义 owner 必须在
     * 返回该产品前复核当前代次。未登记的条目、其它 owner 和空域均不受影响。
     */
    fun retire(owner: ScopeSessionCacheOwner): Int {
        require(owner.scopeSession === this) { "Cache owner belongs to a different ScopeSession" }
        var removed = 0
        for (address in ownedEntries.remove(owner).orEmpty()) {
            val bucket = scopes[address.id] ?: continue
            if (bucket.remove(address.key) != null) removed++
            if (bucket.isEmpty()) scopes.remove(address.id)
        }
        owner.advanceGeneration()
        return removed
    }

    /** 获取或构建 id/key 产品；owned id 的代次必须在进入此存储时仍然有效。 */
    @OptIn(PrivateForInline::class)
    inline fun <reified ID : Any, reified FS : Any> getOrBuild(
        id: ID,
        key: ScopeSessionKey<ID, FS>,
        build: () -> FS,
    ): FS {
        val ownedId = id as? ScopeSessionOwnedId<*>
        if (ownedId == null) {
            return scopes().getOrPut(id) { hashMapOf() }.getOrPut(key, build) as FS
        }
        validateOwnedId(ownedId)
        val owner = ownedId.owner
        scopes()[id]?.get(key)?.let { return it as FS }

        // 先完成 factory，避免失败留下空 bucket 或将重入期间退役的旧表重新挂回缓存。
        val built = build()
        if (ownedId.generation != owner.generation) return built
        val bucket = scopes().getOrPut(id) { hashMapOf() }
        val value = bucket.getOrPut(key) { built }
        recordOwnedEntry(ownedId, key)
        return value as FS
    }

    @PublishedApi
    internal fun validateOwnedId(id: ScopeSessionOwnedId<*>) {
        require(id.owner.scopeSession === this) { "Cache owner belongs to a different ScopeSession" }
        check(id.generation == id.owner.generation) { "Cannot query a retired scope-cache generation" }
    }

    @PublishedApi
    internal fun recordOwnedEntry(id: ScopeSessionOwnedId<*>, key: ScopeSessionKey<*, *>) {
        ownedEntries.getOrPut(id.owner) { linkedSetOf() }.add(CacheAddress(id, key))
    }

    /**
     * 泛型产品的显式 owned 入口，不要求 factory 支持非局部 return。
     *
     * 原 inline/reified getOrBuild 保持既有调用合同；这里使用相同登记和代次检查，
     * 供产品类型由上层通用缓存工厂传递的语义缓存使用。
     */
    @Suppress("UNCHECKED_CAST")
    fun <ID : Any, PRODUCT : Any> getOrBuildOwned(
        id: ScopeSessionOwnedId<ID>,
        key: ScopeSessionKey<ScopeSessionOwnedId<ID>, PRODUCT>,
        build: () -> PRODUCT,
    ): PRODUCT {
        validateOwnedId(id)
        scopes[id]?.get(key)?.let { return it as PRODUCT }
        val built = build()
        if (id.owner.generation != id.generation) return built
        val value = scopes.getOrPut(id) { hashMapOf() }.getOrPut(key) { built }
        recordOwnedEntry(id, key)
        return value as PRODUCT
    }
}

/** 一个 ScopeSession 中可精确退役的 opaque 缓存所有权；不解释语义版本或类型。 */
class ScopeSessionCacheOwner internal constructor(internal val scopeSession: ScopeSession) {
    var generation: Long = 0L
        private set

    internal fun advanceGeneration() {
        generation++
    }

    /** 使用当前所有权代次包装实际语义 id；值的 equality 仍由原语义模型决定。 */
    fun <ID : Any> id(value: ID): ScopeSessionOwnedId<ID> = ScopeSessionOwnedId(this, generation, value)
}

/** 缓存 id 的物理 owner、捕获代次和完整语义输入，供共同存储登记精确条目。 */
data class ScopeSessionOwnedId<out ID : Any> internal constructor(
    val owner: ScopeSessionCacheOwner,
    val generation: Long,
    val value: ID,
)

/** ScopeSession 缓存项的强类型键。 */
abstract class ScopeSessionKey<ID : Any, FS : Any>
