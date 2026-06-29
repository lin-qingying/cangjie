package org.cangnova.cangjie.utils

import java.lang.ref.WeakReference

/**
 * 以弱引用保存两个对象的 Pair。
 *
 * 该类型用于关联缓存对象但不阻止任一端被 GC 回收。
 */
class WeakPair<K, V>(first: K, second: V) {
    /**
     * 第一个对象的弱引用。
     */
    private val firstReference: WeakReference<K> = WeakReference(first)
    /**
     * 第二个对象的弱引用。
     */
    private val secondReference: WeakReference<V> = WeakReference(second)

    /**
     * 当前仍存活的第一个对象；若已被回收则为 null。
     */
    val first: K?
        get() = firstReference.get()

    /**
     * 当前仍存活的第二个对象；若已被回收则为 null。
     */
    val second: V?
        get() = secondReference.get()
}

/**
 * 支持对可空 [WeakPair] 做解构声明的第一个分量。
 */
operator fun <K, V> WeakPair<K, V>?.component1(): K? {
    return this?.first
}

/**
 * 支持对可空 [WeakPair] 做解构声明的第二个分量。
 */
operator fun <K, V> WeakPair<K, V>?.component2(): V? {
    return this?.second
}
