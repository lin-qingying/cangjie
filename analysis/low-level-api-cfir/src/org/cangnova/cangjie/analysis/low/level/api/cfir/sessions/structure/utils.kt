

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

internal fun <T> KProperty1<T, *>.isLazyInitialized(receiver: T): Boolean {
    isAccessible = true

    // If we don't error out here, we won't notice if a property actually isn't `by lazy`. Since the code is only run when
    // explicitly enabled by an internal flag, an error is fine.
    val lazy = getDelegate(receiver) as? Lazy<*> ?: error("Expected a lazy property.")

    return lazy.isInitialized()
}
