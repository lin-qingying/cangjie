

package org.cangnova.cangjie.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 始终返回固定值的只读属性委托。
 */
class DummyDelegate<T>(
    /**
     * 委托属性读取时返回的固定值。
     */
    private val value: T,
) : ReadOnlyProperty<Any?, T> {
    /**
     * 返回构造时传入的固定值。
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}
