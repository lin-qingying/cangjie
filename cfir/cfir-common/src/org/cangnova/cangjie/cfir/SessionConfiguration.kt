package org.cangnova.cangjie.cfir

/**
 * 标记只能在 session 配置阶段调用的 API。
 *
 * 带有该注解的函数通常会组合或注册 [org.cangnova.cangjie.cfir.session.CfirSessionComponent]，
 * 调用点必须处在 session 构造流程中，避免解析阶段动态改变全局组件拓扑。
 */
@RequiresOptIn
annotation class SessionConfiguration
