package org.cangnova.cangjie.analysis.api.lifetime

/**
 * 具有有限生命周期的对象（对齐 Kotlin 的 KaLifetimeOwner）。
 *
 * CaSession 及其产出的符号、类型等都实现此接口。
 * 不能在 analyze() 块外泄漏，跨块传递需使用 Pointer 模式。
 */
interface CaLifetimeOwner {
    val token: CaLifetimeToken
}

fun CaLifetimeOwner.isValid(): Boolean = token.isValid()

inline fun CaLifetimeOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

inline fun <R> CaLifetimeOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}
