package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 调用解析尝试的公开视图标记。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallResolutionAttempt`:
 * 该接口用作"解析过程产物"的统一锚点,具体子类型由实现层提供;
 * 在公开层只保证它是受 lifetime 管理的对象。
 */
sealed interface CaCallResolutionAttempt : CaLifetimeOwner

/**
 * 符号解析尝试的公开视图标记。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolResolutionAttempt`:
 * 与 [CaCallResolutionAttempt] 类似,只作为符号级解析过程的稳定锚点。
 */
sealed interface CaSymbolResolutionAttempt : CaLifetimeOwner
