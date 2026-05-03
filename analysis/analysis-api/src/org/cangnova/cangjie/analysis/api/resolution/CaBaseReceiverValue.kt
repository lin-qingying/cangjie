package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 当前仓颉 public resolution API 的基础 receiver 叶子。
 *
 * 现阶段接口层只稳定公开 receiver 的类型，
 * 因此这里不引入 Kotlin 上游 explicit/implicit/smart-cast receiver 的公开细分接口。
 */
@CaImplementationDetail
class CaBaseReceiverValue(
    private val backingType: CaType,
) : CaReceiverValue {
    override val token: CaLifetimeToken
        get() = backingType.token

    override val type: CaType
        get() = withValidityAssertion { backingType }
}
