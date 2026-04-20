/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.caches

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.utils.caches.softCachedValue
import org.cangnova.cangjie.cfir.caches.CfirLazyValue

internal class LLCfirSoftLazyValue<V>(project: Project, createValue: () -> V) : CfirLazyValue<V>() {
    private val cachedValue = softCachedValue(project) { createValue() }

    override fun getValue(): V = cachedValue.getValue()
}
