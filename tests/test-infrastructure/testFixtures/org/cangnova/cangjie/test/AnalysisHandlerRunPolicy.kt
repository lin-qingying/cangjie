package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.AnalysisHandler

/**
 * 执行 `AnalysisHandler` 对应的测试基础设施流程，维持测试框架的阶段契约。
 */
fun AnalysisHandler<*>.shouldRun(thereWasAnException: Boolean): Boolean {
    return !(doNotRunIfThereWerePreviousFailures && thereWasAnException)
}
