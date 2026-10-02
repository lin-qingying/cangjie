package org.cangnova.cangjie.lsp.server

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import io.opentelemetry.api.metrics.Meter
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieOpenTelemetryProvider

/**
 * LSP 请求耗时统计域。
 *
 * 命名空间是 `cangjie.lsp.*` 而不是 `cangjie.analysis.*`：指标归属于 LSP 服务本身，
 * 而不是 Analysis API。两者共用同一个 OpenTelemetry 后端实例，因此一次导入就能在同一个
 * 看板里同时看到"请求多慢"和"分析多慢"。
 *
 * 三个指标：耗时直方图（毫秒）、执行次数、失败次数。失败单独计数而不是只看耗时，是因为
 * "请求慢"与"请求报错"对 IDE 使用者的含义完全不同，混在耗时分布里看不出区别。
 *
 * 开关沿用 registry key `cangjie.analysis.statistics`：它是平台声明的唯一工程级遥测开关，
 * 为 LSP 另开一个开关只会让"统计到底开没开"出现两个答案。
 */
class CangjieRequestStatistics(meter: Meter) {
    /**
     * 耗时的桶边界（毫秒）。LSP 请求跨度从毫秒级的补全到秒级的全文诊断。
     */
    private val durationBuckets = listOf(1L, 5L, 10L, 25L, 50L, 100L, 250L, 500L, 1_000L, 2_500L, 5_000L, 10_000L)

    /**
     * 按请求种类分桶的耗时直方图（毫秒）。
     */
    private val durations: Map<CangjieLspRequest, LongHistogram> = CangjieLspRequest.entries.associateWith { request ->
        meter.histogramBuilder(metricName(request, "duration"))
            .setDescription("LSP ${request.lspMethod} request duration")
            .setUnit("ms")
            .ofLongs()
            .setExplicitBucketBoundariesAdvice(durationBuckets)
            .build()
    }

    /**
     * 按请求种类分桶的执行次数。
     */
    private val runs: Map<CangjieLspRequest, LongCounter> = CangjieLspRequest.entries.associateWith { request ->
        meter.counterBuilder(metricName(request, "runs"))
            .setDescription("Number of LSP ${request.lspMethod} requests executed")
            .build()
    }

    /**
     * 按请求种类分桶的失败次数。
     */
    private val failures: Map<CangjieLspRequest, LongCounter> = CangjieLspRequest.entries.associateWith { request ->
        meter.counterBuilder(metricName(request, "failures"))
            .setDescription("Number of failed LSP ${request.lspMethod} requests")
            .build()
    }

    /**
     * 记录一次请求的耗时与成败。
     */
    fun onRequestFinished(request: CangjieLspRequest, elapsedNanos: Long, failed: Boolean) {
        durations[request]?.record(elapsedNanos / 1_000_000L)
        runs[request]?.add(1)
        if (failed) failures[request]?.add(1)
    }

    /**
     * 为 [project] 创建请求统计域。
     *
     * 返回 `null` 表示统计未启用：registry key `cangjie.analysis.statistics` 关闭，
     * 或平台没有提供 OpenTelemetry 后端。此时调用方必须走零开销路径。
     */
    @OptIn(CaPlatformInterface::class)
    companion object {
        /**
         * 统计总开关，与 analysis 层共用同一个 key。
         */
        private const val STATISTICS_REGISTRY_KEY = "cangjie.analysis.statistics"

        /**
         * 创建请求统计域；统计未启用或后端缺失时返回 `null`。
         */
        fun forProject(project: Project): CangjieRequestStatistics? {
            if (!Registry.`is`(STATISTICS_REGISTRY_KEY, false)) return null
            val openTelemetry = CangJieOpenTelemetryProvider.getInstance(project)?.openTelemetry ?: return null
            return CangjieRequestStatistics(openTelemetry.getMeter("cangjie.lsp"))
        }

        /**
         * 指标名：`cangjie.lsp.request.<lspMethod>.<suffix>`。
         *
         * LSP 方法名本身含 `/`，在指标名里替换成 `.` 以免被误读成层级分隔符。
         */
        fun metricName(request: CangjieLspRequest, suffix: String): String =
            "cangjie.lsp.request.${request.lspMethod.replace('/', '.')}.$suffix"
    }
}
