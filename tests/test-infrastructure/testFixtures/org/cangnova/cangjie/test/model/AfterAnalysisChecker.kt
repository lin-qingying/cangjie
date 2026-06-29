package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.services.TestServices

/**
 * 分析后检查器
 *
 * 对应 Kotlin K2 的 AfterAnalysisChecker
 */
abstract class AfterAnalysisChecker(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    /**
     * 提供 `check` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    open fun check(failedAssertions: List<WrappedException>) {}

    /**
     * 提供 `suppressIfNeeded` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    open fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> = failedAssertions

    /**
     * 提供 `wrap` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    protected fun Throwable.wrap(): WrappedException = WrappedException.FromAfterAnalysisChecker(this)

    /**
     * 保存 `order`，供测试模型在测试执行期间读取或传递。
     */
    open val order: Order
        get() = Order.P3

    /**
     * 表示 `Order`，承载测试模型中的配置数据、测试产物或处理步骤。
     */
    enum class Order {
        P1, P2, P3, P4, P5
    }
}
