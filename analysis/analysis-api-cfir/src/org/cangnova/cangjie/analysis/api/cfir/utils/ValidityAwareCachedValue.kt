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
    /**
     * 实际承载缓存值的 Kotlin lazy。
     */
    private val lazyValue: Lazy<T>,
) : ReadOnlyProperty<CaLifetimeOwner, T> {
    /**
     * 每次读取缓存值前先执行生命周期有效性检查。
     */
    override fun getValue(thisRef: CaLifetimeOwner, property: KProperty<*>): T {
        return thisRef.withValidityAssertion { lazyValue.value }
    }
}

/**
 * 为生命周期拥有者创建带有效性检查的 publication lazy 缓存。
 */
@Suppress("UnusedReceiverParameter") // we need to have the KtLifetimeOwner as receiver to make sure it's called only for KtLifetimeOwner
internal fun <T> CaLifetimeOwner.cached(init: () -> T): ValidityAwareCachedValue<T> {
    return ValidityAwareCachedValue(lazy(LazyThreadSafetyMode.PUBLICATION, init))
}
