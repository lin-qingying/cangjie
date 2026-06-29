package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer

/**
 * 元测试配置器
 *
 * 对应 Kotlin K2 的 MetaTestConfigurator
 */
abstract class MetaTestConfigurator(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    /**
     * 提供 `transformTestDataPath` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun transformTestDataPath(testDataFileName: String): String = testDataFileName

    /**
     * 提供 `shouldSkipTest` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun shouldSkipTest(): Boolean = false
}
