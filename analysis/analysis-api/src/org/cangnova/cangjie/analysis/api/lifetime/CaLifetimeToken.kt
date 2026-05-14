package org.cangnova.cangjie.analysis.api.lifetime

/**
 * 用于追踪 [CaLifetimeOwner] 生命周期的 token。
 *
 * - 由 session 在创建时分配,所有 session 产物共享同一个 token;
 * - 当 session 失效(例如模块结构变化、内容修改),token 也同步失效;
 * - 当前调用线程缺乏访问前提(IntelliJ 下未持有读锁等)时,token 报告"不可访问"。
 *
 * 一旦失效便不会再次变为有效;但"可访问性"是动态的,会随上下文切换。
 *
 * 对齐 Kotlin Analysis API 的 `KaLifetimeToken`。
 */
abstract class CaLifetimeToken {
    /**
     * 当前 token 是否仍然有效。
     *
     * 失效后不会重新变为有效。
     */
    abstract fun isValid(): Boolean

    /**
     * 返回 [isValid] 为 `false` 时的失效原因。
     *
     * 如果当前 token 仍然有效,实现可自行决定是否抛错。
     */
    abstract fun getInvalidationReason(): String

    /**
     * 当前 token 在调用线程上是否可访问。
     *
     * 例如 IntelliJ 实现要求调用方持有读锁,否则视为不可访问。
     */
    abstract fun isAccessible(): Boolean

    /**
     * 返回 [isAccessible] 为 `false` 时的不可访问原因。
     */
    abstract fun getInaccessibilityReason(): String
}

/**
 * 断言 token 有效;否则抛出 [CaInvalidLifetimeOwnerAccessException]。
 */
fun CaLifetimeToken.assertIsValid() {
    if (!isValid()) {
        throw CaInvalidLifetimeOwnerAccessException(getInvalidationReason())
    }
}

/**
 * 断言 token 当前可访问;否则抛出 [CaInaccessibleLifetimeOwnerAccessException]。
 */
fun CaLifetimeToken.assertIsAccessible() {
    if (!isAccessible()) {
        throw CaInaccessibleLifetimeOwnerAccessException(getInaccessibilityReason())
    }
}

/**
 * 同时断言有效性与可访问性。
 *
 * 优先校验有效性,确保失效信息不会被可访问性问题掩盖。
 */
fun CaLifetimeToken.assertIsValidAndAccessible() {
    assertIsValid()
    assertIsAccessible()
}
