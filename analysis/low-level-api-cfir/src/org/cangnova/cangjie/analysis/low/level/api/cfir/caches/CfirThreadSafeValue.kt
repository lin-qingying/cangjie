/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.cangnova.cangjie.cfir.caches.CfirLazyValue

internal class CfirThreadSafeValue<V>(createValue: () -> V) : CfirLazyValue<V>() {
    private val lazyValue by lazy(LazyThreadSafetyMode.SYNCHRONIZED, createValue)
    override fun getValue(): V = lazyValue
}