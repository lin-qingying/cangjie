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
    fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol>

    fun CjReferenceExpression.resolveToSymbol(): CaSymbol? = resolveToSymbols().singleOrNull()

    fun CjElement.resolveToCall(): CaCallInfo?
}
