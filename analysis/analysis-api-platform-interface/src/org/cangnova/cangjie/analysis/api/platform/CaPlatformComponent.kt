package org.cangnova.cangjie.analysis.api.platform

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * Analysis API 平台服务组件标记接口。
 */
@CaPlatformInterface
interface CaPlatformComponent

/**
 * 可选平台注册组件标记接口。
 */
@CaPlatformInterface
interface CaOptionalPlatformComponent : CaPlatformComponent
