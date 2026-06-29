

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPutWithNullableValue
import org.cangnova.cangjie.analysis.api.platform.caches.nullValueToNull
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.logErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 `ConcurrentHashMap` 的线程安全 CFIR cache，支持缓存 null 值并可修正不一致条目。
 */
internal class CfirThreadSafeCache<K : Any, V, CONTEXT>(
    /**
     * 保存缓存条目的并发 map，内部会用哨兵对象表示 null 值。
     */
    private val map: ConcurrentHashMap<K, Any> = ConcurrentHashMap(),
    /**
     * 在 key 未命中时根据 key 与上下文创建缓存值的函数。
     */
    private val createValue: (K, CONTEXT) -> V,
) : CfirCache<K, V, CONTEXT>(), CfirCacheWithInvalidation<K, V, CONTEXT> {
    /**
     * 取得缓存值；未命中时以线程安全方式调用 [createValue]。
     */
    override fun getValue(key: K, context: CONTEXT): V = map.getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    /**
     * 只在条目已经计算完成时返回值，不触发新计算。
     */
    override fun getValueIfComputed(key: K): V? = map[key]?.nullValueToNull()

    /**
     * 返回当前所有已缓存的非空值，供 CFIR cache 内部调试和失效逻辑使用。
     */
    @CfirCacheInternals
    override val cachedValues: Collection<V>
        get() = map.values.mapNotNull { it.nullValueToNull() }

    /**
     * 重新计算 key 的非空值并用调用方提供的合并策略修复 map 中的不一致条目。
     */
    override fun fixInconsistentValue(
        key: K,
        context: CONTEXT & Any,
        mapping: (oldValue: V, newValue: V & Any) -> V & Any,
        inconsistencyMessage: String,
        buildAdditionalAttachments: (ExceptionAttachmentBuilder.(K, CONTEXT) -> Unit)?,
    ): V & Any {
        val newValue = createValue(key, context)
        checkWithAttachment(
            condition = newValue != null,
            message = { "A value for requested key & context must not be null due to the contract" },
        ) {
            buildAttachments(key, context, newValue)
        }

        LOG.logErrorWithAttachment(inconsistencyMessage) {
            buildAttachments(key, context, newValue)
            if (buildAdditionalAttachments != null) {
                buildAdditionalAttachments(key, context)
            }
        }

        val result = map.merge(key, newValue) { old, _ ->
            mapping(old.nullValueToNull(), newValue)
        }

        @Suppress("UNCHECKED_CAST")
        return result as (V & Any)
    }

    /**
     * 为 cache 不一致日志构造 key、context 与 value 的异常附件。
     */
    private fun ExceptionAttachmentBuilder.buildAttachments(key: K, context: CONTEXT, value: V) {
        withEntry("key", key.toString())

        if (context is PsiElement) {
            withPsiEntry("context", context)
        } else {
            withEntry("context", context.toString())
        }

        val unwrapped = (value as? Collection<*>)?.singleOrNull() ?: value
        when (unwrapped) {
            is CfirElement -> withCfirEntry("value", unwrapped)
            is CfirBasedSymbol<*> -> withCfirSymbolEntry("value", unwrapped)
            else -> withEntry("value", unwrapped.toString())
        }
    }
}

/**
 * 线程安全 CFIR cache 的日志对象。
 */
private val LOG = Logger.getInstance(CfirThreadSafeCache::class.java)
