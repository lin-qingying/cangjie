/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventListener
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * [LLCfirSessionInvalidationService] listens to [modification events][org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEvent]
 * and invalidates [LLCfirSession]s which depend on the modified [CaModule].
 */
@CaImplementationDetail
class LLCfirSessionInvalidationService(private val project: Project) {
    internal class LLKotlinModificationEventListener(val project: Project) : KotlinModificationEventListener {
        override fun onModification(event: KotlinModificationEvent) {
            getInstance(project).invalidate(event)
        }
    }

    internal class LLPsiModificationTrackerListener(val project: Project) : PsiModificationTracker.Listener {
        override fun modificationCountChanged() {
            getInstance(project).invalidator.invalidateUnstableDanglingFileSessions()
        }
    }

    private val invalidator by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirSessionCacheStorageInvalidator(project, LLCfirSessionCache.getInstance(project).storage)
    }

    /**
     * @see LLCfirSessionCacheStorageInvalidator.invalidate
     */
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
        fun getInstance(project: Project): LLCfirSessionInvalidationService =
            project.getService(LLCfirSessionInvalidationService::class.java)
    }
}
