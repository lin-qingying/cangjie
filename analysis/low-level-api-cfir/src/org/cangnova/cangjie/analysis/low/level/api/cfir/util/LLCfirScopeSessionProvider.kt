/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.utils.caches.SoftCachedMap
import org.cangnova.cangjie.cfir.ScopeSession
import java.util.concurrent.ConcurrentHashMap


abstract class LLCfirScopeSessionProvider {
    abstract fun getScopeSession(): ScopeSession

    companion object {
        fun create(project: Project, invalidationTrackers: List<Any>): LLCfirScopeSessionProvider = when {
            invalidationTrackers.isEmpty() -> LLCfirNonInvalidatableScopeSessionProvider()
            else -> LLCfirInvalidatableScopeSessionProvider(project, invalidationTrackers)
        }
    }
}

private class LLCfirInvalidatableScopeSessionProvider(project: Project, invalidationTrackers: List<Any>) : LLCfirScopeSessionProvider() {
    // ScopeSession is thread-local, so we use Thread id as a key
    // We cannot use thread locals here as it may lead to memory leaks
    private val cache = SoftCachedMap.create<Long, ScopeSession>(
        project,
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES,
        invalidationTrackers
    )

    override fun getScopeSession(): ScopeSession {
        return cache.getOrPut(Thread.currentThread().id) { ScopeSession() }
    }
}

private class LLCfirNonInvalidatableScopeSessionProvider : LLCfirScopeSessionProvider() {
    // ScopeSession is thread-local, so we use Thread id as a key
    // We cannot use thread locals here as it may lead to memory leaks
    private val cache = ConcurrentHashMap<Long, ScopeSession>()

    override fun getScopeSession(): ScopeSession {
        return cache.getOrPut(Thread.currentThread().id) { ScopeSession() }
    }
}
