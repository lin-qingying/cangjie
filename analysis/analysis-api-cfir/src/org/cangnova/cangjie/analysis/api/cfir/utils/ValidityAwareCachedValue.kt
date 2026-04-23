package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lazy value that guaranties safe publication and checks validity on every access
 */
@JvmInline
internal value class ValidityAwareCachedValue<T>(
    private val lazyValue: Lazy<T>,
) : ReadOnlyProperty<CaLifetimeOwner, T> {
    override fun getValue(thisRef: CaLifetimeOwner, property: KProperty<*>): T {
        return thisRef.withValidityAssertion { lazyValue.value }
    }
}

@Suppress("UnusedReceiverParameter") // we need to have the KtLifetimeOwner as receiver to make sure it's called only for KtLifetimeOwner
internal fun <T> CaLifetimeOwner.cached(init: () -> T): ValidityAwareCachedValue<T> {
    return ValidityAwareCachedValue(lazy(LazyThreadSafetyMode.PUBLICATION, init))
}
