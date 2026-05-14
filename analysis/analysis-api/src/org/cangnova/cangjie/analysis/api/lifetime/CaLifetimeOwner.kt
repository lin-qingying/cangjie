package org.cangnova.cangjie.analysis.api.lifetime

/**
 * 具有有限生命周期的 Analysis API 对象。
 *
 * - 最常见的实现者是 [CaSession][org.cangnova.cangjie.analysis.api.CaSession] 自身,以及
 *   它派生出的所有 symbol、type、call、signature 等;
 * - 生命周期由 [token] 描述,所有 use-site 在访问任意公开成员前都应做有效性断言;
 * - 不允许把 lifetime owner 泄漏到 `analyze {}` 块之外;
 *   跨块共享请改用对应的 pointer 机制(`CaSymbolPointer` 等)。
 *
 * 对齐 Kotlin Analysis API 的 `KaLifetimeOwner`。
 */
interface CaLifetimeOwner {
    /**
     * 标记该对象生命周期的 token。
     */
    val token: CaLifetimeToken
}

/**
 * 当前 lifetime owner 是否仍然有效。
 *
 * 仅检查有效性,不检查可访问性(读锁、线程绑定等)。
 */
fun CaLifetimeOwner.isValid(): Boolean = token.isValid()

/**
 * 断言对象既有效又可访问;若不满足,抛出对应的 [CaIllegalLifetimeOwnerAccessException] 子类。
 */
inline fun CaLifetimeOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

/**
 * 在确认 lifetime owner 处于合法访问状态后执行 [action]。
 *
 * Analysis API 所有公共入口都应在内部使用此函数:
 * 一旦 lifetime 失效或不可访问,立刻以异常告警,而不会返回过期值。
 */
inline fun <R> CaLifetimeOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}
