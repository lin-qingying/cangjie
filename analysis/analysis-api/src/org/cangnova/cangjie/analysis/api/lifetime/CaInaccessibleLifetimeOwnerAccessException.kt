package org.cangnova.cangjie.analysis.api.lifetime

/**
 * 访问"暂时不可访问"的生命周期对象时抛出。
 *
 * 对象本身仍然有效(例如 session 未失效),但当前线程不满足访问前提:
 * - IntelliJ 平台下,必须在读锁内访问 Analysis API;
 * - 某些平台可能存在线程/上下文绑定。
 *
 * 对齐 Kotlin Analysis API 的 `KaInaccessibleLifetimeOwnerAccessException`。
 *
 * @property message 触发此异常的具体原因,由 [CaLifetimeToken.getInaccessibilityReason] 提供。
 */
class CaInaccessibleLifetimeOwnerAccessException(
    override val message: String,
) : CaIllegalLifetimeOwnerAccessException()
