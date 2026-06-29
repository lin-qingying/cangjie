@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventListener

/**
 * [LLCfirSessionInvalidationService] listens to [modification events][org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEvent]
 * and invalidates [LLCfirSession]s which depend on the modified module.
 */
@CaImplementationDetail
class LLCfirSessionInvalidationService(private val project: Project) {
    /**
     * 监听 analysis API 模块修改事件并转发给 session invalidation service。
     */
    @OptIn(CaPlatformInterface::class)
    internal class LLCangJieModificationEventListener(val project: Project) : KotlinModificationEventListener {
        /**
         * 在收到模块或源码修改事件后触发 session 失效。
         */
        override fun onModification(event: KotlinModificationEvent) {
            getInstance(project).invalidate(event)
        }
    }

    /**
     * 监听 PSI 全局修改计数变化，用于清理 unstable dangling file session。
     */
    internal class LLPsiModificationTrackerListener(val project: Project) : PsiModificationTracker.Listener {
        /**
         * PSI 修改计数变化后失效 unstable dangling file session。
         */
        override fun modificationCountChanged() {
            getInstance(project).invalidator.invalidateUnstableDanglingFileSessions()
        }
    }

    /**
     * 当前服务使用的 session cache storage invalidator。
     */
    private val invalidator by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirSessionCacheStorageInvalidator(project, LLCfirSessionCache.getInstance(project).storage)
    }

    /**
     * @see LLCfirSessionCacheStorageInvalidator.invalidate
     */
    @OptIn(CaPlatformInterface::class)
    fun invalidate(event: KotlinModificationEvent) {
        invalidator.invalidate(event)
    }

    /**
     * @see LLCfirSessionCacheStorageInvalidator.invalidateAll
     */
    fun invalidateAll(includeLibraryModules: Boolean, diagnosticInformation: String? = null) {
        invalidator.invalidateAll(includeLibraryModules, diagnosticInformation)
    }

    companion object {
        /**
         * 取得工程级 session invalidation service。
         */
        fun getInstance(project: Project): LLCfirSessionInvalidationService =
            project.getService(LLCfirSessionInvalidationService::class.java)
    }
}
