package org.cangnova.cangjie.analysis.api.performance.test

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricReader
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import java.util.concurrent.TimeUnit

/**
 * 性能测试宿主使用的 OpenTelemetry SDK 实例。
 *
 * Analysis API 的统计指标只写进 [CangJieOpenTelemetryProvider.openTelemetry] 返回的
 * `MeterProvider`（对齐 Kotlin 的 `KotlinGlobalOpenTelemetryProvider`），链路 span 只写进它的
 * `TracerProvider`。没有 SDK 时 `GlobalOpenTelemetry.get()` 返回 noop 实现，计数与 span 被
 * 直接丢弃，测试无法断言任何东西。本类为测试宿主提供真实 SDK：
 *
 * 1. [InMemoryMetricReader] 与 [InMemorySpanExporter] 常驻，用于在用例内读取指标与 span；
 * 2. 可选 OTLP 导出器：设置系统属性 [OTLP_ENDPOINT_PROPERTY]（如 `http://localhost:4317`）后，
 *    指标与 span 会同时以固定周期推送到本地 collector，再由它分流到 Prometheus（指标）
 *    与 Jaeger（链路）；
 * 3. 指标按 JVM 累积（cumulative temporality），用例应通过 [AbstractCaPerformanceTest.counterDelta]
 *    取增量后再断言；span 通过 [resetSpans] 清空快照后断言本次操作产生的链路。
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

    /**
     * 测试 SDK 的资源标识，出现在 span 的 `service.name` 里。
     */
    private const val SERVICE_NAME: String = "cangjie-analysis-performance-test"

    /**
     * 可选的 OTLP endpoint；未配置时为 `null`。
     */
    private val otlpEndpoint: String? =
        System.getProperty(OTLP_ENDPOINT_PROPERTY)?.takeIf { it.isNotBlank() }

    private val inMemoryReader: InMemoryMetricReader = InMemoryMetricReader.create()

    private val inMemorySpanExporter: InMemorySpanExporter = InMemorySpanExporter.create()

    private val optionalReaders: List<MetricReader> = buildList {
        otlpEndpoint?.let { endpoint ->
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
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(
                    Resource.getDefault().merge(
                        Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), SERVICE_NAME))
                    )
                )
                // 内存导出器用 SimpleSpanProcessor：span 一结束就导出，用例断言前不需要 forceFlush。
                .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
                .apply {
                    otlpEndpoint?.let { endpoint ->
                        addSpanProcessor(
                            BatchSpanProcessor.builder(
                                OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build()
                            ).build()
                        )
                    }
                }
                .build()
        )
        .build()

    /**
     * 读取已结束的全部 span。
     */
    fun finishedSpans(): List<SpanData> = inMemorySpanExporter.finishedSpanItems

    /**
     * 清空已结束 span 的快照。
     *
     * 用例在操作前调用，保证断言的是本次操作产生的 span，而不是测试宿主装配阶段的残留。
     */
    fun resetSpans() {
        inMemorySpanExporter.reset()
    }

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

    /**
     * 读取所有直方图指标，返回“指标名 → 采样点数”。
     *
     * 采样点数即该指标的记录次数；耗时类指标据此判断“是否被记录过”，
     * 具体数值用 [collectHistogramSums] 与 [collectHistogramMaxima]。
     */
    fun collectHistogramPointCounts(): Map<String, Long> {
        val counts = LinkedHashMap<String, Long>()
        inMemoryReader.collectAllMetrics().forEach { metric ->
            val histogram = metric.histogramData ?: return@forEach
            counts[metric.name] = (counts[metric.name] ?: 0L) + histogram.points.sumOf { it.count }
        }
        return counts
    }

    /**
     * 读取所有直方图指标，返回“指标名 → 采样值之和”。
     */
    fun collectHistogramSums(): Map<String, Double> {
        val sums = LinkedHashMap<String, Double>()
        inMemoryReader.collectAllMetrics().forEach { metric ->
            val histogram = metric.histogramData ?: return@forEach
            sums[metric.name] = (sums[metric.name] ?: 0.0) + histogram.points.sumOf { it.sum }
        }
        return sums
    }

    /**
     * 读取所有直方图指标，返回“指标名 → 最大采样值”。
     */
    fun collectHistogramMaxima(): Map<String, Double> {
        val maxima = LinkedHashMap<String, Double>()
        inMemoryReader.collectAllMetrics().forEach { metric ->
            val histogram = metric.histogramData ?: return@forEach
            val max = histogram.points.maxOfOrNull { if (it.hasMax()) it.max else it.sum } ?: return@forEach
            maxima[metric.name] = maxOf(maxima[metric.name] ?: Double.NEGATIVE_INFINITY, max)
        }
        return maxima
    }
}
