package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer

/**
 * 元测试配置器
 *
 * 对应 Kotlin K2 的 MetaTestConfigurator
 */
abstract class MetaTestConfigurator(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    open fun transformTestDataPath(testDataFileName: String): String = testDataFileName

    open fun shouldSkipTest(): Boolean = false
}
