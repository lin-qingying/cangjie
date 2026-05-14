package org.cangnova.cangjie.analysis.api.lifetime

/**
 * 所有"生命周期违规"异常的基类。
 *
 * - 当调用方在 `analyze {}` 块外、或者使用已失效的 session 产物时,
 *   Analysis API 应通过该异常体系报错而非返回脏数据;
 * - 直接子类区分两种违规:
 *   - [CaInvalidLifetimeOwnerAccessException]:对象已失效;
 *   - [CaInaccessibleLifetimeOwnerAccessException]:对象有效但当前不可访问(例如未持有读锁)。
 *
 * 对齐 Kotlin Analysis API 的 `KaIllegalLifetimeOwnerAccessException`。
 */
abstract class CaIllegalLifetimeOwnerAccessException : IllegalStateException()
