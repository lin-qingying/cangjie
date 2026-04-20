

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder

internal interface CfirCacheWithInvalidation<K : Any, V, CONTEXT> {
    /**
     * Drops the incorrect value from the cache and add a new value instead.
     */
    fun fixInconsistentValue(
        key: K,
        context: CONTEXT & Any,
        mapping: (oldValue: V, newValue: V & Any) -> V & Any,
        inconsistencyMessage: String,
        buildAdditionalAttachments: (ExceptionAttachmentBuilder.(K, CONTEXT) -> Unit)? = null,
    ): V & Any
}

/**
 * Return cached value or created a new one from [context].
 * This method assumes that we can't return null for not-null context.
 * Logs inconsistency error if it is present.
 *
 * @return not-null [VALUE] in case of [CfirCacheWithInvalidation] cache.
 */
internal fun <KEY : Any, VALUE, CONTEXT> CfirCache<KEY, VALUE, CONTEXT>.getNotNullValueForNotNullContext(
    key: KEY,
    context: CONTEXT,
    buildAdditionalAttachments: (ExceptionAttachmentBuilder.(KEY, CONTEXT) -> Unit)? = null,
): VALUE {
    val value = getValue(key, context)
    @Suppress("CANNOT_CHECK_FOR_ERASED")
    return if (value != null ||
        context == null ||
        this !is CfirCacheWithInvalidation<KEY, VALUE, CONTEXT>
    ) {
        value
    } else {
        fixInconsistentValue(
            key = key,
            context = context,
            mapping = { old, new -> old ?: new },
            inconsistencyMessage = "Inconsistency in the cache. Someone without context put a null value in the cache",
            buildAdditionalAttachments = buildAdditionalAttachments,
        )
    }
}
