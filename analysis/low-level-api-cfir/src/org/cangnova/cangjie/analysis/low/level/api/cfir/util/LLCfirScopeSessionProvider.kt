/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirGlobalResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSessionStatistics
import org.cangnova.cangjie.analysis.utils.caches.SoftCachedMap
import org.cangnova.cangjie.cfir.ScopeSession
import java.util.concurrent.ConcurrentHashMap

/**
 * low-level CFIR 会话使用的 [ScopeSession] 提供器。
 *
 * 子类只负责"按什么键缓存"，"怎么新建一个 scope session"由基类的 [createScopeSession] 决定，
 * 统计埋点因此只存在于一处：新增一种缓存实现不会漏埋。
 */
abstract class LLCfirScopeSessionProvider(
    /**
     * scope session 创建统计域；统计未启用时为 `null`。
     */
    private val sessionStatistics: LLSessionStatistics?,
) {
    /**
     * 返回当前线程可用的 [ScopeSession]。
     */
    abstract fun getScopeSession(): ScopeSession

    /**
     * 新建一个 scope session 并记录创建次数。
     *
     * 只记次数不记耗时：`ScopeSession` 本身是空壳，创建耗时没有诊断价值；但创建次数直接
     * 反映 scope 缓存失效频率——PSI 修改后每个线程都要重建，是 scope 缓存失效风暴的信号。
     */
    protected fun createScopeSession(): ScopeSession {
        sessionStatistics?.onScopeSessionCreated()
        return ScopeSession()
    }

    /**
     * 创建带失效跟踪或无失效跟踪的 scope session 提供器。
     */
    companion object {
        /**
         * 根据 [invalidationTrackers] 是否为空选择具体实现。
         *
         * [sessionStatistics] 必须是工程级 session 创建统计域；统计未启用时传 `null`，
         * 此时 scope session 创建不产生任何计数。
         */
        fun create(
            project: Project,
            invalidationTrackers: List<Any>,
            sessionStatistics: LLSessionStatistics? = LLCfirGlobalResolveComponents.getInstance(project).sessionStatistics,
        ): LLCfirScopeSessionProvider = when {
            invalidationTrackers.isEmpty() -> LLCfirNonInvalidatableScopeSessionProvider(sessionStatistics)
            else -> LLCfirInvalidatableScopeSessionProvider(project, invalidationTrackers, sessionStatistics)
        }
    }
}

/**
 * 支持项目级失效跟踪的 [ScopeSession] 提供器。
 */
private class LLCfirInvalidatableScopeSessionProvider(
    project: Project,
    invalidationTrackers: List<Any>,
    sessionStatistics: LLSessionStatistics?,
) : LLCfirScopeSessionProvider(sessionStatistics) {
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
        return cache.getOrPut(Thread.currentThread().id) { createScopeSession() }
    }
}

/**
 * 不依赖外部失效跟踪器的 [ScopeSession] 提供器。
 */
private class LLCfirNonInvalidatableScopeSessionProvider(sessionStatistics: LLSessionStatistics?) :
    LLCfirScopeSessionProvider(sessionStatistics) {
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
        return cache.getOrPut(Thread.currentThread().id) { createScopeSession() }
    }
}
