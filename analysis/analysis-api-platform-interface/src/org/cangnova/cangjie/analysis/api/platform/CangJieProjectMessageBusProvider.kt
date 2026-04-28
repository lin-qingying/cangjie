package org.cangnova.cangjie.analysis.api.platform

import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * 提供项目级 [MessageBus] 作为 Analysis API message bus。
 *
 * 这是 IDE 与 standalone Analysis API 的默认实现。保留独立 provider 抽象，
 * 是为了后续即使底层 message bus 不再直接等于 project.messageBus，平台 API 形状也无需变化。
 */
@CaPlatformInterface
class CangJieProjectMessageBusProvider(private val project: Project) : CangJieMessageBusProvider {
    override fun getMessageBus(): MessageBus = project.messageBus
}
