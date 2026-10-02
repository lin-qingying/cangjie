@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.statistics.CaStatisticsService
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 性能/统计测试入口。
 *
 * 宿主形态对齐 [CaCfirStandaloneAnalysisApiTestConfigurator]（standalone CFIR），
 * 在其之上补齐统计链路所需的全部条件：
 *
 * 1. 从生产描述符登记 registry key 并打开 `cangjie.analysis.statistics`
 *    （[CaPerformanceTestServiceRegistrar]）；
 * 2. 注册 SDK 支撑的 OpenTelemetry provider（同上）；
 * 3. 用例通过 [counterDelta] 在一段操作前后取指标增量，再对
 *    [org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames]
 *    中的指标名断言；指标由 [CaPerformanceTestTelemetry] 的 SDK 收集。
 *
 * 子类只需给出 testData 目录并沿用 [AbstractAnalysisApiExecutionTest] 的用例签名：
 *
 * ```kotlin
 * class MyPerfTest : AbstractAnalysisApiPerformanceTest("analysis/analysis-performance-test/testData/performance") {
 *     @Test
 *     fun myCase(mainFile: CjFile) {
 *         val delta = counterDelta { analyzeForTest(mainFile) { } }
 *         assertTrue(counter(delta, LLStatisticsMetricNames.analyzeInvocations) >= 1)
 *     }
 * }
 * ```
 */
abstract class AbstractAnalysisApiPerformanceTest(
    testDirPathString: String,
) : AbstractAnalysisApiExecutionTest(testDirPathString) {
    /**
     * 使用 standalone CFIR 宿主。
     */
    override val configurator: AnalysisApiTestConfigurator
        get() = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 追加统计服务注册器。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + CaPerformanceTestServiceRegistrar()

    /**
     * 当前用例所属的 mock 工程；宿主装配完成后可用。
     */
    protected val project: Project
        get() = testServices.environmentManager.getProject()

    /**
     * 当前用例所属工程的统计服务；宿主装配完成后可用。
     */
    protected val statisticsService: CaStatisticsService
        get() = CaStatisticsService.getInstance(project)
            ?: error("Analysis API 统计未启用：请检查 ${CaPerformanceTestServiceRegistrar::class} 是否已登记并打开统计开关。")

    /**
     * 当前用例所属工程的 low-level 统计服务；统计域从这里获取。
     */
    protected val lowLevelStatisticsService: LLStatisticsService
        get() = LLStatisticsService.getInstance(project)
            ?: error("LLStatisticsService 不可用：OpenTelemetry provider 未注册或统计开关未启用。")

    /**
     * 在一段操作前后读取指标，返回"指标名 → 增量"。
     *
     * SDK 的指标按 JVM 累积，直接比较两次绝对值会把其他用例的数据算进来；
     * 用例内应一律基于增量断言。
     */
    protected fun counterDelta(block: () -> Unit): Map<String, Long> {
        val before = CaPerformanceTestTelemetry.collectLongCounters()
        block()
        val after = CaPerformanceTestTelemetry.collectLongCounters()
        val names = before.keys + after.keys
        return names.associateWith { name -> after[name].orZero() - before[name].orZero() }
    }

    /**
     * 读取某个指标的当前累积值。
     */
    protected fun counterValue(name: String): Long = CaPerformanceTestTelemetry.collectLongCounters()[name].orZero()

    /**
     * 从增量表 [delta] 中读取某个指标；没有该指标时返回 0。
     */
    protected fun counter(delta: Map<String, Long>, name: String): Long = delta[name] ?: 0L

    private fun Long?.orZero(): Long = this ?: 0L
}