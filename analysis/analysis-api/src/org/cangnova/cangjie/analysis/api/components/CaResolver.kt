package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionAttempt
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * 解析协议。
 *
 * 只负责把源码元素映射到稳定的公开语义结果，
 * 不暴露底层候选、约束系统或后端特定解析细节。
 *
 * 协议分两条路径，与 Kotlin `KaResolver` 一致：
 *
 * 1. 引用路径：[resolveToSymbols] 面向引用表达式，经由引用实现解析（Kotlin 的 `KtReference.resolveToSymbols`）；
 * 2. 元素路径：[tryResolveSymbols] / [resolveSymbols] / [resolveSymbol] 面向 PSI 元素，
 *    经由后端的元素级解析钩子产出（Kotlin 的 `KtResolvable.tryResolveSymbols` 一族）。
 *
 * 与 Kotlin 的偏差：Kotlin 用 `KtResolvable` 标记 PSI 元素级可解析性，
 * 仓颉 PSI 没有对应标记接口，因此元素路径的接收者取 `CjElement`，
 * 与同协议的 [resolveToCall] 保持一致。
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

    /**
     * 元素级解析尝试:保留成功、失败诊断与候选符号,供需要区分"解析失败"与"无结果"的调用方使用。
     */
    fun CjElement.tryResolveSymbols(): CaSymbolResolutionAttempt?

    /**
     * 元素级解析的符号集合;无法解析时返回空集合。
     */
    fun CjElement.resolveSymbols(): Collection<CaSymbol>

    /**
     * 元素级解析的唯一目标符号;不存在唯一结果时返回 `null`。
     */
    fun CjElement.resolveSymbol(): CaSymbol?
}