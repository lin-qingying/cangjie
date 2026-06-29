/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.utils.caches.softCachedValue
import org.cangnova.cangjie.cfir.caches.CfirLazyValue

/**
 * 以 IntelliJ soft cached value 承载的 CFIR lazy value。
 */
internal class LLCfirSoftLazyValue<V>(project: Project, createValue: () -> V) : CfirLazyValue<V>() {
    /**
     * 可被内存压力回收的底层 cached value。
     */
    private val cachedValue = softCachedValue(project) { createValue() }

    /**
     * 返回当前 soft cached value，必要时重新计算。
     */
    override fun getValue(): V = cachedValue.getValue()
}
