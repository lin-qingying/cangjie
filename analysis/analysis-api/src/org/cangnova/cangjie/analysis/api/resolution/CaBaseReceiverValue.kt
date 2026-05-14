package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 公开 resolution API 的基础 receiver 实现。
 *
 * 现阶段公开层只稳定暴露 receiver 的类型,因此不引入 Kotlin 上游
 * explicit / implicit / smart-cast receiver 的细分接口;后续若需要更细的
 * receiver 分类,再通过新增子接口扩展,不破坏当前 API 兼容性。
 *
 * @property backingType 真实 receiver 类型,生命周期由其自身的 token 决定。
 */
@CaImplementationDetail
class CaBaseReceiverValue(
    private val backingType: CaType,
) : CaReceiverValue {
    /**
     * Lifetime token,直接复用 [backingType] 的 token,保持生命周期一致。
     */
    override val token: CaLifetimeToken
        get() = backingType.token

    /**
     * Receiver 的稳定语义类型,访问前会先做有效性断言。
     */
    override val type: CaType
        get() = withValidityAssertion { backingType }
}
