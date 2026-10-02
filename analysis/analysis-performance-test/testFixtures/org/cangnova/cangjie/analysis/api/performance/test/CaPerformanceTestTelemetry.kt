package org.cangnova.cangjie.analysis.api.performance.test

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricReader
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import java.util.concurrent.TimeUnit

/**
 * 性能测试宿主使用的 OpenTelemetry SDK 实例。
 *
 * Analysis API 的统计指标只写进 [CangJieOpenTelemetryProvider.openTelemetry] 返回的 `MeterProvider`
 * （对齐 Kotlin 的 `KotlinGlobalOpenTelemetryProvider`）。没有 SDK 时 `GlobalOpenTelemetry.get()`
 * 返回 noop 实现，计数被直接丢弃，测试无法断言任何指标。本类为测试宿主提供真实 SDK：
 *
 * 1. [InMemoryMetricReader] 常驻，用于在用例内读取指标值；
 * 2. 可选 OTLP 导出器：设置系统属性 [OTLP_ENDPOINT_PROPERTY]（如 `http://localhost:4317`）后，
 *    指标会同时以固定周期推送到本地 collector / Jaeger；
 * 3. 指标按 JVM 累积（cumulative temporality），用例应通过 [AbstractCaPerformanceTest.counterDelta]
 *    取增量后再断言。
 */
object CaPerformanceTestTelemetry {
    /**
     * 开启 OTLP 导出的系统属性名，值为 gRPC endpoint。
     */
    const val OTLP_ENDPOINT_PROPERTY: String = "cangjie.performance.otlp.endpoint"

    /**
     * OTLP 导出周期（秒）。
     */
    private const val EXPORT_INTERVAL_SECONDS: Long = 5L

    private val inMemoryReader: InMemoryMetricReader = InMemoryMetricReader.create()

    private val optionalReaders: List<MetricReader> = buildList {
        System.getProperty(OTLP_ENDPOINT_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?.let { endpoint ->
                val exporter = OtlpGrpcMetricExporter.builder().setEndpoint(endpoint).build()
                add(
                    PeriodicMetricReader.builder(exporter)
                        .setInterval(EXPORT_INTERVAL_SECONDS, TimeUnit.SECONDS)
                        .build()
                )
            }
    }

    /**
     * 测试宿主注册给 `CangJieOpenTelemetryProvider` 的 SDK 实例。
     */
    val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
        .setMeterProvider(
            SdkMeterProvider.builder()
                .registerMetricReader(inMemoryReader)
                .apply { optionalReaders.forEach { registerMetricReader(it) } }
                .build()
        )
        .build()

    /**
     * 读取当前所有 long 型指标，返回“指标名 → 累积值”。
     *
     * 同名指标可能来自不同 session 的缓存，这里按名字求和。
     */
    fun collectLongCounters(): Map<String, Long> {
        val counters = LinkedHashMap<String, Long>()
        inMemoryReader.collectAllMetrics().forEach { metric ->
            val sum = metric.longSumData ?: return@forEach
            counters[metric.name] = (counters[metric.name] ?: 0L) + sum.points.sumOf { it.value }
        }
        return counters
    }
}