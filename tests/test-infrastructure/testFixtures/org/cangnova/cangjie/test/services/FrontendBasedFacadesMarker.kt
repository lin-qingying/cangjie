package org.cangnova.cangjie.test.services

/**
 * 提供 `FrontendBasedFacadesMarker` 单例，集中承载测试服务的共享状态、常量或默认行为。
 */
object FrontendBasedFacadesMarker : TestService

/**
 * 保存 `frontendBasedFacadesMarkerRegistrationData`，供测试服务在测试执行期间读取或传递。
 */
val frontendBasedFacadesMarkerRegistrationData: ServiceRegistrationData =
    service { _: TestServices -> FrontendBasedFacadesMarker }
