package org.cangnova.cangjie.cfir

/**
 * 所有 [org.cangnova.cangjie.cfir.session.CfirSession] 都应通过会话工厂创建。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.PrivateSessionConstructor`
 *
 * 用于标记会话子类构造器，避免绕过工厂直接实例化。
 */
@RequiresOptIn
annotation class PrivateSessionConstructor

/**
 * 用于约束会话初始化后的组件注册行为。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.SessionConfiguration`
 *
 * 通过 `@OptIn(SessionConfiguration::class)` 明确允许在特定配置路径下注册组件，
 * 降低误用风险。
 */
@RequiresOptIn
annotation class SessionConfiguration

