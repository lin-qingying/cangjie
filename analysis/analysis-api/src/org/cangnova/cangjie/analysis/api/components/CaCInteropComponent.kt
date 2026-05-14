package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement

/**
 * C 互操作信息协议。
 *
 * 设计要点/职责:
 * - 暴露 PSI 元素或 symbol 与 C 互操作语义之间的关联,不直接承载平台映射的细节实现。
 * - 协议返回值统一为可空,缺乏互操作语义时返回 `null`,避免在协议层抛出异常。
 *
 * 对齐 Kotlin Analysis API 的 `KaJavaInteroperabilityComponent`,
 * 用于桥接非 Cangjie 平台(此处特指 C/Native 互操作)的语义视图。
 */
interface CaCInteropComponent : CaLifetimeOwner {
    /**
     * 获取该 PSI 元素关联的 C 互操作信息;若不属于 C 互操作上下文则返回 `null`。
     */
    fun CjElement.getInteropInfo(): CaInteropInfo?

    /**
     * 获取该 symbol 关联的 C 互操作信息;若该 symbol 与互操作无关则返回 `null`。
     */
    fun CaSymbol.getInteropInfo(): CaInteropInfo?
}
