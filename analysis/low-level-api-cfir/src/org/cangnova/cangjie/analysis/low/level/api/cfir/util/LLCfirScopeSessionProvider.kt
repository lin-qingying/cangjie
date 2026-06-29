/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.utils.caches.SoftCachedMap
import org.cangnova.cangjie.cfir.ScopeSession
import java.util.concurrent.ConcurrentHashMap

/**
 * low-level CFIR 会话使用的 [ScopeSession] 提供器。
 */
abstract class LLCfirScopeSessionProvider {
    /**
     * 返回当前线程可用的 [ScopeSession]。
     */
    abstract fun getScopeSession(): ScopeSession

    /**
     * 创建带失效跟踪或无失效跟踪的 scope session 提供器。
     */
    companion object {
        /**
         * 根据 [invalidationTrackers] 是否为空选择具体实现。
         */
        fun create(project: Project, invalidationTrackers: List<Any>): LLCfirScopeSessionProvider = when {
            invalidationTrackers.isEmpty() -> LLCfirNonInvalidatableScopeSessionProvider()
            else -> LLCfirInvalidatableScopeSessionProvider(project, invalidationTrackers)
        }
    }
}

/**
 * 支持项目级失效跟踪的 [ScopeSession] 提供器。
 */
private class LLCfirInvalidatableScopeSessionProvider(project: Project, invalidationTrackers: List<Any>) : LLCfirScopeSessionProvider() {
    // ScopeSession is thread-local, so we use Thread id as a key
    // We cannot use thread locals here as it may lead to memory leaks
    /**
     * 以线程 ID 为键、受失效跟踪器控制的软缓存。
     */
    private val cache = SoftCachedMap.create<Long, ScopeSession>(
        project,
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES,
        invalidationTrackers
    )

    /**
     * 返回当前线程对应的 [ScopeSession]，不存在时创建。
     */
    override fun getScopeSession(): ScopeSession {
        return cache.getOrPut(Thread.currentThread().id) { ScopeSession() }
    }
}

/**
 * 不依赖外部失效跟踪器的 [ScopeSession] 提供器。
 */
private class LLCfirNonInvalidatableScopeSessionProvider : LLCfirScopeSessionProvider() {
    // ScopeSession is thread-local, so we use Thread id as a key
    // We cannot use thread locals here as it may lead to memory leaks
    /**
     * 以线程 ID 为键的常驻 scope session 缓存。
     */
    private val cache = ConcurrentHashMap<Long, ScopeSession>()

    /**
     * 返回当前线程对应的 [ScopeSession]，不存在时创建。
     */
    override fun getScopeSession(): ScopeSession {
        return cache.getOrPut(Thread.currentThread().id) { ScopeSession() }
    }
}
