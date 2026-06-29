package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.TestModuleStructure

/**
 * 保存 `TestServices.moduleStructure`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.moduleStructure: TestModuleStructure by TestServices.testServiceAccessor()
