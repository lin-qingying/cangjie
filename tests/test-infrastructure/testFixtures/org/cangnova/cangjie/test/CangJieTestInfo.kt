package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.TestInfo
import kotlin.jvm.optionals.getOrNull

/**
 * 表示 `CangJieTestInfo`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
data class CangJieTestInfo(
    /**
     * 保存 `className`，供测试基础设施在测试执行期间读取或传递。
     */
    val className: String,
    /**
     * 保存 `methodName`，供测试基础设施在测试执行期间读取或传递。
     */
    val methodName: String,
    /**
     * 保存 `tags`，供测试基础设施在测试执行期间读取或传递。
     */
    val tags: Set<String> = emptySet(),
) : TestService


/**
 * 执行 `toCangJieTestInfo` 对应的测试基础设施流程，维持测试框架的阶段契约。
 */
fun TestInfo.toCangJieTestInfo():CangJieTestInfo {
    return CangJieTestInfo(
        className = this.testClass.getOrNull()?.name ?: "_undefined_",
        methodName = this.testMethod.getOrNull()?.name ?: "_testUndefined_",
        tags = this.tags
    )
}

/**
 * 保存 `TestServices.testInfo`，供测试基础设施在测试执行期间读取或传递。
 */
val TestServices.testInfo: CangJieTestInfo by TestServices.testServiceAccessor()
