package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.Assertions

/**
 * 表示 `AssertionsService`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class AssertionsService : Assertions(), TestService

/**
 * 保存 `TestServices.assertions`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.assertions: AssertionsService by TestServices.testServiceAccessor()
