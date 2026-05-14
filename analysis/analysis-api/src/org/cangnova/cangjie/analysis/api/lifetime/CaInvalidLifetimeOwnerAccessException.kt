package org.cangnova.cangjie.analysis.api.lifetime

/**
 * 访问"已失效"的生命周期对象时抛出。
 *
 * 对象所属 session、模块或文件已被修改或销毁,任何后续访问都不再合法。
 * 失效不可逆转,调用方需要重新发起 `analyze {}` 取新对象。
 *
 * 对齐 Kotlin Analysis API 的 `KaInvalidLifetimeOwnerAccessException`。
 *
 * @property message 触发此异常的具体原因,由 [CaLifetimeToken.getInvalidationReason] 提供。
 */
class CaInvalidLifetimeOwnerAccessException(
    override val message: String,
) : CaIllegalLifetimeOwnerAccessException()
