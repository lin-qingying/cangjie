

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.util.messages.Topic
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * [Topic]s for events published by [LLCfirSessionInvalidationService] *after* session invalidation. These topics should be subscribed to via
 * the Analysis API message bus: [analysisMessageBus][org.cangnova.cangjie.analysis.api.platform.analysisMessageBus].
 *
 * Session invalidation events are guaranteed to be published after the associated sessions have been invalidated.
 * Sessions can be invalidated either in a write action, or in the case if the caller can guarantee no other threads can perform
 * invalidation or code analysis until the cleanup is complete. Session invalidation events are published on the same thread – it means
 * only the reporter thread has access to sessions.
 *
 * When a session is garbage-collected due to being softly reachable, no session invalidation event will be published for it. See the
 * documentation of [LLCfirSession] for background information.
 *
 * Session invalidation events are not published for unstable
 * [CjDanglingFileModules][org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule].
 */

/**
 * low-level CFIR session 失效后发布的消息主题集合。
 */
object LLCfirSessionInvalidationTopics {
    /**
     * session 失效事件主题。
     */
    val SESSION_INVALIDATION: Topic<LLCfirSessionInvalidationListener> =
        Topic(LLCfirSessionInvalidationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)
}

/**
 * low-level CFIR session 失效事件监听器。
 */
interface LLCfirSessionInvalidationListener {
    /**
     * [afterInvalidation] is published when sessions for the given [modules] have been invalidated. Because the sessions are already
     * invalid, the event carries their [CaModule][CaModule]s.
     *
     * @see LLCfirSessionInvalidationTopics
     */
    fun afterInvalidation(modules: Set<CaModule>)

    /**
     * [afterGlobalInvalidation] is published when all sessions may have been invalidated. The event doesn't guarantee that all sessions
     * have been invalidated, but e.g. caches should be cleared as if this was the case. This event is published when the invalidated
     * sessions cannot be easily enumerated.
     *
     * @see LLCfirSessionInvalidationTopics
     */
    fun afterGlobalInvalidation()
}
