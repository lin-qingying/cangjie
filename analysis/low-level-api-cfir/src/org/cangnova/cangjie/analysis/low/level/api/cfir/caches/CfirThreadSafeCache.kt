

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPutWithNullableValue
import org.cangnova.cangjie.analysis.api.platform.caches.nullValueToNull
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirSymbolEntry
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.logErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import java.util.concurrent.ConcurrentHashMap

internal class CfirThreadSafeCache<K : Any, V, CONTEXT>(
    private val map: ConcurrentHashMap<K, Any> = ConcurrentHashMap(),
    private val createValue: (K, CONTEXT) -> V,
) : CfirCache<K, V, CONTEXT>(), CfirCacheWithInvalidation<K, V, CONTEXT> {
    override fun getValue(key: K, context: CONTEXT): V = map.getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    override fun getValueIfComputed(key: K): V? = map[key]?.nullValueToNull()

    @CfirCacheInternals
    override val cachedValues: Collection<V>
        get() = map.values.mapNotNull { it.nullValueToNull() }

    override fun fixInconsistentValue(
        key: K,
        context: CONTEXT & Any,
        mapping: (oldValue: V, newValue: V & Any) -> V & Any,
        inconsistencyMessage: String,
        buildAdditionalAttachments: (ExceptionAttachmentBuilder.(K, CONTEXT) -> Unit)?,
    ): V & Any {
        val newValue = createValue(key, context)
        checkWithAttachment(
            newValue != null,
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

private val LOG = Logger.getInstance(CfirThreadSafeCache::class.java)
