/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals

/**
 * [ValueReferenceCleaner] performs a cleaning operation after its associated value has been removed from a [CleanableValueReferenceCache]
 * or was garbage-collected. The cleaner will be strongly referenced from the value reference held by the cache.
 *
 * You **must not** store a reference to the associated value [V] in its [ValueReferenceCleaner]. Otherwise, the cached values will never
 * become non-strongly reachable.
 *
 * The cleaner may be invoked multiple times by the cache, in any thread. Implementations of [ValueReferenceCleaner] must ensure that the
 * operation is repeatable and thread-safe.
 */
@LLCfirInternals
fun interface ValueReferenceCleaner<V> {
    /**
     * Cleans up after [value] has been removed from the [CleanableValueReferenceCache] or was garbage-collected.
     *
     * [value] is non-null if it was removed from the cache and is still referable, or `null` if it has already been garbage-collected.
     */
    fun cleanUp(value: V?)

    /**
     * A version of [cleanUp] which allows adding additional diagnostic information.
     */
    fun cleanUp(value: V?, diagnosticInformation: String?) = cleanUp(value)
}

@LLCfirInternals
@Suppress("unused")// used in IDE
class NoOpValueReferenceCleaner<V> : ValueReferenceCleaner<V> {
    override fun cleanUp(value: V?) {}
}