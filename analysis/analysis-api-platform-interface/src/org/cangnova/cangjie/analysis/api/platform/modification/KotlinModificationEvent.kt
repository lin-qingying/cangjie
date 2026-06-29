package org.cangnova.cangjie.analysis.api.platform.modification

import com.intellij.util.messages.Topic
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus

/**
 * In the Analysis API, [KotlinModificationEvent]s signal changes in source code, module and project settings, or project structure. These
 * events should be subscribed to and published via the [TOPIC] on the [Analysis API message bus][analysisMessageBus]. They must be
 * published in a **write action**.
 */
@CaPlatformInterface
sealed interface KotlinModificationEvent {
    @CaPlatformInterface
    companion object {
        /**
         * @see KotlinModificationEvent
         * @see KotlinModificationEventListener
         */
        val TOPIC: Topic<KotlinModificationEventListener> = Topic(
            KotlinModificationEventListener::class.java,
            Topic.BroadcastDirection.TO_CHILDREN,
            true,
        )
    }
}

/**
 * A listener for [KotlinModificationEvent]s. It should be registered on the [analysisMessageBus] with [KotlinModificationEvent.TOPIC].
 */
@CaPlatformInterface
fun interface KotlinModificationEventListener {
    /**
     * [onModification] is invoked before or after the modification and usually in a write action.
     */
    fun onModification(event: KotlinModificationEvent)
}

/**
 * [KotlinModificationEventKind] represents the kinds of [KotlinModificationEvent]s.
 */
@CaPlatformInterface
enum class KotlinModificationEventKind {
    MODULE_STATE_MODIFICATION,
    MODULE_OUT_OF_BLOCK_MODIFICATION,
    GLOBAL_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
    CODE_FRAGMENT_CONTEXT_MODIFICATION,
}

/**
 * 当前修改事件种类是否只影响单个模块。
 */
@CaPlatformInterface
val KotlinModificationEventKind.isModuleLevel: Boolean
    get() = this == KotlinModificationEventKind.MODULE_STATE_MODIFICATION ||
            this == KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION ||
            this == KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION

/**
 * 当前修改事件种类是否影响全局模块或项目状态。
 */
@CaPlatformInterface
val KotlinModificationEventKind.isGlobalLevel: Boolean
    get() = !isModuleLevel
