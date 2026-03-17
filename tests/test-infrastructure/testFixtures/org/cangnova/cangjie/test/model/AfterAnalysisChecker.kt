package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.services.TestServices

/**
 * 分析后检查器
 *
 * 对应 Kotlin K2 的 AfterAnalysisChecker
 */
abstract class AfterAnalysisChecker(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    open fun check(failedAssertions: List<WrappedException>) {}

    open fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> = failedAssertions

    protected fun Throwable.wrap(): WrappedException = WrappedException.FromAfterAnalysisChecker(this)

    open val order: Order
        get() = Order.P3

    enum class Order {
        P1, P2, P3, P4, P5
    }
}
