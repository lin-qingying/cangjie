package org.cangnova.cangjie.lsp.server

import com.intellij.mock.MockProject
import com.intellij.openapi.util.registry.Registry
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

/**
 * LSP 请求级耗时统计的端到端接线测试。
 *
 * 这一层此前完全没有验证，而它正是分析侧之外最直接的用户感知路径：用户在编辑器里按一次
 * 补全，等待的时长就在 `cangjie.lsp.request.textDocument/completion.duration` 里。
 *
 * 此前只有 `CangjieLspRequestCoverageTest`，它测的是枚举本身（每个协议方法有对应枚举项、
 * 方法名不重复、指标名不含斜杠），完全不碰 `CangjieRequestExecutor.compute`。于是
 * "注册表没登记上导致 `forProject` 恒返回 null" 这类断线不会让任何用例变红——请求照常执行，
 * 只是永远不出指标。
 *
 * 本用例走完整链路：真实 `CangjieLspEnvironment`（其 create 内部已跑 registrar，含声明
 * 统计开关的 platform-interface 描述符）→ 真实 `CangjieServerContext` → 真实
 * `CangjieRequestExecutor.compute` → 内存 SDK 断言。
 *
 * 断言的是**回调路径**而不是请求返回值：动作体做什么与统计无关，用一个最简动作即可，
 * 这样用例不会因语义能力实现变化而脆。
 */
@OptIn(CaPlatformInterface::class)
class CangjieRequestStatisticsSeamTest {
    /**
     * 统计域从真实工程解析得到，一次成功请求留下耗时与次数，一个抛异常的请求额外留下失败数。
     */
    @Test
    fun realRequestExecutorReportsDurationRunsAndFailures() {
        val reader = InMemoryMetricReader.create()
        val sdk = OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build()
        // 全局实例必须在环境创建之前设好：`CangjieLspEnvironment.create` 内部会跑 registrar，
        // 它注册的 `CangJieGlobalOpenTelemetryProvider` 读的就是 `GlobalOpenTelemetry.get()`。
        // 先设好，registrar 注册的那个 provider 才指向这份 SDK。事后自己再注册一个会撞
        // "Key ... duplicated"——Mock 容器不允许同键覆盖。
        GlobalOpenTelemetry.set(sdk)
        val environment = try {
            CangjieLspEnvironment.create(CangJieCoreEnvironmentMode.UnitTest)
        } catch (throwable: Throwable) {
            GlobalOpenTelemetry.resetForTest()
            throw throwable
        }
        try {
            val project = environment.project as MockProject
            Registry.get(STATISTICS_REGISTRY_KEY).setValue("true")
            try {
                // `CangjieServerContext` 在构造期就调用该工厂（第 65 行），所以不能给一个抛异常的工厂：它当场就抛，测不到请求执行器。
                // 用 lsp testkit 里现成的契约 facade，它实现了接口全部抽象成员。
                val context = CangjieServerContext(
                    descriptor = org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor(),
                    environment = environment,
                    analysisFacadeFactory = { context -> ProtocolContractAnalysisFacade(context) }
                )
                context.use {
                    assertNotNull(
                        CangjieRequestStatistics.forProject(project),
                        "统计开关已开且后端已注册，统计域必须能解析出来；返回 null 意味着 LSP 侧指标会静默不上报",
                    )

                    val beforeCounters = counters(reader)
                    val beforeDurations = histogramCounts(reader)
                    val completed = it.requestExecutor
                        .compute(CangjieLspRequest.COMPLETION) { "completion-result" }
                        .get()
                    assertEquals("completion-result", completed, "请求必须正常返回，不得被统计包装改变")

                    val afterCounters = counters(reader)
                    val afterDurations = histogramCounts(reader)
                    val runsDelta = delta(afterCounters, beforeCounters, CangjieRequestStatistics.metricName(CangjieLspRequest.COMPLETION, "runs"))
                    assertEquals(1L, runsDelta, "一次真实请求必须记一次执行次数")
                    assertEquals(
                        1L,
                        delta(afterDurations, beforeDurations, CangjieRequestStatistics.metricName(CangjieLspRequest.COMPLETION, "duration")),
                        "一次真实请求必须留下一个耗时采样",
                    )

                    // 失败请求单独计数：请求慢与请求报错对使用者的含义完全不同。
                    val beforeFailure = counters(reader)
                    val failure = assertThrows(ExecutionException::class.java) {
                        it.requestExecutor
                            .compute(CangjieLspRequest.COMPLETION) { throw IllegalStateException("boom") }
                            .get()
                    }
                    assertEquals("boom", failure.cause?.message, "动作抛出的异常必须原样上抛，不得被统计吞掉")
                    val afterFailure = counters(reader)
                    assertEquals(
                        1L,
                        delta(afterFailure, beforeFailure, CangjieRequestStatistics.metricName(CangjieLspRequest.COMPLETION, "failures")),
                        "抛异常的请求必须记一次失败",
                    )
                    assertEquals(
                        1L,
                        delta(afterFailure, beforeFailure, CangjieRequestStatistics.metricName(CangjieLspRequest.COMPLETION, "runs")),
                        "失败请求同样计入执行次数",
                    )
                }
            } finally {
                Registry.get(STATISTICS_REGISTRY_KEY).setValue("false")
            }
        } finally {
            GlobalOpenTelemetry.resetForTest()
            environment.close()
        }
    }

    /**
     * 统计域为 `null` 时请求执行器走零开销路径，请求照常返回。
     *
     * 这条锁定的是"关掉统计不会顺带关掉 LSP 功能本身"。这里直接构造默认
     * `statistics = null` 的执行器，而不是去卸载工程上的 provider 服务：`forProject` 在
     * provider 存在但后端是 noop 时返回的是非空对象（那属于"统计开着但没 SDK"，指标照常
     * 写进 noop），`null` 只在后端缺失时出现，而那不是本用例要验的分支。
     */
    @Test
    fun requestsStillRunWhenStatisticsAreUnavailable() {
        CangjieRequestExecutor().use { executor ->
            val result = executor.compute(CangjieLspRequest.COMPLETION) { "still-works" }.get()
            assertEquals("still-works", result, "统计不可用时请求必须照常返回")
        }

        val failure = assertThrows(ExecutionException::class.java) {
            CangjieRequestExecutor().use { executor ->
                executor.compute(CangjieLspRequest.COMPLETION) { throw IllegalStateException("boom") }.get()
            }
        }
        assertEquals("boom", failure.cause?.message, "统计不可用时异常也必须原样上抛")
    }

    /**
     * 读取当前所有 long 计数器："指标名 → 累计值"。
     *
     * 只读 `longSumData`：计数器的点在 SDK 里叫 `longSumData`，直接简单地叫 `metric.longPoints`
     * 是错的。
     */
    private fun counters(reader: InMemoryMetricReader): Map<String, Long> {
        val result = LinkedHashMap<String, Long>()
        reader.collectAllMetrics().forEach { metric ->
            val sum = metric.longSumData ?: return@forEach
            result.merge(metric.name, sum.points.sumOf { it.value }, Long::plus)
        }
        return result
    }

    /**
     * 读取当前所有直方图的采样点数："指标名 → 采样次数"。
     */
    private fun histogramCounts(reader: InMemoryMetricReader): Map<String, Long> {
        val counts = LinkedHashMap<String, Long>()
        reader.collectAllMetrics().forEach { metric ->
            val histogram = metric.histogramData ?: return@forEach
            counts.merge(metric.name, histogram.points.sumOf { it.count }, Long::plus)
        }
        return counts
    }

    /**
     * 取两次快照之间的增量。
     */
    private fun delta(after: Map<String, Long>, before: Map<String, Long>, name: String): Long =
        (after[name] ?: 0L) - (before[name] ?: 0L)

    private companion object {
        /**
         * 统计开关，与 analysis 侧共用同一个 key。
         */
        private const val STATISTICS_REGISTRY_KEY = "cangjie.analysis.statistics"
    }
}