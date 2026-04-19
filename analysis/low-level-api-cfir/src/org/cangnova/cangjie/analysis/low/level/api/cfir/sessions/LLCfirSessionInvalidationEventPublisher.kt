/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.isStable
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus

/**
 * [LLCfirSessionInvalidationEventPublisher] publishes [session invalidation events][LLCfirSessionInvalidationTopics] after session
 * invalidation to allow caches that depend on [LLCfirSession]s to be invalidated actively. These events are not published after garbage
 * collection of softly reachable sessions. See [LLCfirSession] for more information.
 */
internal class LLCfirSessionInvalidationEventPublisher(private val project: Project) {
    /**
     * [invalidatedModules] can only exist during write actions while executing [collectSessionsAndPublishInvalidationEvent], so we don't
     * have to use a thread-safe collection.
     */
    private var invalidatedModules: MutableSet<CaModule>? = null

    /**
     * Invokes [action] and collects all sessions which were invalidated during its execution. At the end, publishes a session invalidation
     * event if at least one session was invalidated.
     *
     * Invalidated sessions are tracked via [collectSession].
     *
     * Must be called in a write action.
     */
    @OptIn(CaPlatformInterface::class)
    inline fun collectSessionsAndPublishInvalidationEvent(action: () -> Unit) {
        require(invalidatedModules == null) {
            "The set of invalidated modules should be `null` when `collectSessionsAndPublishInvalidationEvent` has just been called."
        }
        invalidatedModules = mutableSetOf()

        try {
            action()

            if (invalidatedModules?.isNotEmpty() == true) {
                project.analysisMessageBus
                    .syncPublisher(LLCfirSessionInvalidationTopics.SESSION_INVALIDATION)
                    .afterInvalidation(invalidatedModules!!)
            }
        } finally {
            invalidatedModules = null
        }
    }

    fun collectSession(session: LLCfirSession) {
        // We don't want to collect any modules outside `collectSessionsAndPublishInvalidationEvent`. For example, this might happen during
        // global invalidation, or when unstable dangling file sessions are replaced during `LLCfirSessionCache.getSession`.
        val invalidatedModules = this.invalidatedModules ?: return

        // Session invalidation events don't need to be published for unstable dangling file modules. However, since the file(s) might have
        // been modified, we need to check the module's validity before checking `isStable`, as otherwise an exception might occur in
        // `isStable`. Even if the module is invalid, we still want to publish a session invalidation event so that the downstream analysis
        // session can be invalidated.
        val ktModule = session.ktModule
        if (ktModule is CaDanglingFileModule && ktModule.isValid && !ktModule.isStable) {
            return
        }

        invalidatedModules.add(ktModule)
    }

    companion object {
        fun getInstance(project: Project): LLCfirSessionInvalidationEventPublisher =
            project.getService(LLCfirSessionInvalidationEventPublisher::class.java)
    }
}
