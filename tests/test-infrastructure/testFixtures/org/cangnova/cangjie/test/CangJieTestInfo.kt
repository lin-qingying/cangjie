package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.services.TestService
import org.junit.jupiter.api.TestInfo
import kotlin.jvm.optionals.getOrNull

data class CangJieTestInfo(
    val className: String,
    val methodName: String,
    val tags: Set<String> = emptySet(),
) : TestService


fun TestInfo.toCangJieTestInfo():CangJieTestInfo {
    return CangJieTestInfo(
        className = this.testClass.getOrNull()?.name ?: "_undefined_",
        methodName = this.testMethod.getOrNull()?.name ?: "_testUndefined_",
        tags = this.tags
    )
}
