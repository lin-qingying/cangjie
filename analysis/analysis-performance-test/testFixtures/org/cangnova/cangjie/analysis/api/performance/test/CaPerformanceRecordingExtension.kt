package org.cangnova.cangjie.analysis.api.performance.test

import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * 性能运行的采集 extension：在用例执行的**两侧**各取一次指标快照。
 *
 * ## 为什么单独一个 extension
 *
 * 测试基类本身拿不到这两个时间点：`AbstractAnalysisApiExecutionTest` 上挂的
 * `AnalysisApiExecutionTestExtension` 才实现了 `BeforeTestExecutionCallback`，而它是在
 * `performTest` 里完成 testData 解析与宿主装配的。要把"装配开销"排除在用例测量之外，
 * 采集就必须发生在那之后。
 *
 * ## 为什么顺序是对的
 *
 * JUnit 5 对生命周期回调的执行顺序是：before 类回调按注册顺序、after 类回调按注册的
 * **逆序**。本 extension 注册在 `AnalysisApiExecutionTestExtension` 之后，于是：
 *
 * - before：框架装配 → 本 extension 取起始快照；
 * - after：本 extension 收尾取终值 → 框架拆卸。
 *
 * 正好把测量窗口夹在"宿主已就绪"与"宿主尚未拆卸"之间。
 */
class CaPerformanceRecordingExtension : BeforeTestExecutionCallback, AfterTestExecutionCallback {
    override fun beforeTestExecution(context: ExtensionContext) {
        CaPerformanceRecorder.beforeCase()
    }

    override fun afterTestExecution(context: ExtensionContext) {
        CaPerformanceRecorder.afterCase(
            testClass = context.requiredTestInstance.javaClass.name,
            testMethod = context.requiredTestMethod.name,
        )
    }
}