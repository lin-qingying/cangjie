/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import org.cangnova.cangjie.cfir.caches.CfirLazyValue

/**
 * 使用 Kotlin synchronized lazy 实现的线程安全 CFIR lazy value。
 */
internal class CfirThreadSafeValue<V>(createValue: () -> V) : CfirLazyValue<V>() {
    /**
     * 实际保存计算结果的同步 lazy 委托。
     */
    private val lazyValue by lazy(LazyThreadSafetyMode.SYNCHRONIZED, createValue)

    /**
     * 返回 lazy value，首次访问时完成同步初始化。
     */
    override fun getValue(): V = lazyValue
}
