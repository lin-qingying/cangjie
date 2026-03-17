

package org.cangnova.cangjie.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DummyDelegate<T>(private val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

