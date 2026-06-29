package org.cangnova.cangjie.test.builders

import org.cangnova.cangjie.test.NonGroupingTestRunner


/**
 * 提供 `nonGroupingPhaseTestRunner` 对应的测试配置构建流程，维持测试框架的阶段契约。
 */
inline fun nonGroupingPhaseTestRunner(testDataPath: String, crossinline init: TestConfigurationBuilder.() -> Unit): NonGroupingTestRunner {
    return NonGroupingTestRunner(testConfiguration(testDataPath, init))
}
