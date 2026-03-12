/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DummyDelegate<T>(private val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

