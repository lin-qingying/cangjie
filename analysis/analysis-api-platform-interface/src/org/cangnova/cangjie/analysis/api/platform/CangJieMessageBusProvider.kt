package org.cangnova.cangjie.analysis.api.platform

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * [CangJieMessageBusProvider] allows Analysis API implementations to provide a custom [MessageBus]. When subscribing to or publishing to
 * Analysis API topics, the message bus provided by [getMessageBus] should be used, not the [Project]'s message bus.
 */
@CaPlatformInterface
interface CangJieMessageBusProvider : CaPlatformComponent {
    /**
     * 返回 Analysis API 订阅和发布主题时应使用的消息总线。
     */
    fun getMessageBus(): MessageBus

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级 message bus provider 服务。
         */
        fun getInstance(project: Project): CangJieMessageBusProvider = project.service()
    }
}

/**
 * The [MessageBus] used to subscribe to and publish to Analysis API topics.
 */
@CaPlatformInterface
val Project.analysisMessageBus: MessageBus
    get() = CangJieMessageBusProvider.getInstance(this).getMessageBus()
