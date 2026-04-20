package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion

/**
 * 公开类型替换器。
 *
 * 该接口对齐 Kotlin `KaSubstitutor` 的公开语义边界，只暴露“把类型中的类型参数替换成另一组类型”的能力，
 * 不把底层映射表、缓存键或后端实现细节泄露到 public API。
 */
interface CaSubstitutor : CaLifetimeOwner {
    /**
     * 对 [type] 执行类型替换。
     *
     * 如果当前替换器对该类型没有产生任何替换，则直接返回原类型对象。
     */
    fun substitute(type: CaType): CaType = withValidityAssertion {
        substituteOrNull(type) ?: type
    }

    /**
     * 对 [type] 执行类型替换。
     *
     * 如果当前替换器对该类型没有产生任何替换，则返回 `null`。
     */
    fun substituteOrNull(type: CaType): CaType?

    /**
     * 空替换器。
     *
     * 它不执行任何替换，并显式表达“当前没有可应用的类型参数映射”这一公开语义。
     */
    class Empty(
        override val token: CaLifetimeToken,
    ) : CaSubstitutor {
        override fun substitute(type: CaType): CaType = withValidityAssertion { type }

        override fun substituteOrNull(type: CaType): CaType? = withValidityAssertion { null }
    }
}
