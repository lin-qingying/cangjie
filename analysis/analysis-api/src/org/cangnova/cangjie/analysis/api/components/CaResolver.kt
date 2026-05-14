package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * 解析协议。
 *
 * 只负责把源码元素映射到稳定的公开语义结果，
 * 不暴露底层候选、约束系统或后端特定解析细节。
 */
interface CaResolver : CaLifetimeOwner {
    /**
     * 把引用表达式解析为可能的多目标 symbol 集合,无法解析时返回空集合。
     */
    fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol>

    /**
     * 解析引用表达式的唯一目标 symbol;不存在唯一结果时返回 `null`。
     */
    fun CjReferenceExpression.resolveToSymbol(): CaSymbol? = resolveToSymbols().singleOrNull()

    /**
     * 将该元素解析为一次完整的调用信息,包含被调用者、实参绑定等;
     * 该元素不构成调用时返回 `null`。
     */
    fun CjElement.resolveToCall(): CaCallInfo?
}
