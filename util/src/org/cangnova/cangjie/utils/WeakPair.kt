package org.cangnova.cangjie.utils

import java.lang.ref.WeakReference

class WeakPair<K, V>(first: K, second: V) {
    private val firstReference: WeakReference<K> = WeakReference(first)
    private val secondReference: WeakReference<V> = WeakReference(second)

    val first: K?
        get() = firstReference.get()

    val second: V?
        get() = secondReference.get()
}

operator fun <K, V> WeakPair<K, V>?.component1(): K? {
    return this?.first
}

operator fun <K, V> WeakPair<K, V>?.component2(): V? {
    return this?.second
}
