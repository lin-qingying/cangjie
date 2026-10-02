package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter

/**
 * stub 反序列化耗时统计域。
 *
 * 库模块的 class-like 符号首次被查询时才会从 stub 反序列化成 CFIR 声明，这一步发生在用户
 * 动作中间而不是工程打开时，因此它是"IDE 卡在某个符号上"的常见来源：一次类型解析可能触发
 * 一整棵嵌套类的反序列化。只记耗时与次数，不记产物大小——反序列化的类成员数没有稳定的
 * 观测点可取，硬凑一个会给出错误的心智模型。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计 stub 反序列化；
 * 这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLDeserializationStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    /**
     * 反序列化耗时的 meter 名字。
     */
    private val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.Deserialization)

    /**
     * class-like 反序列化耗时直方图（毫秒）。
     */
    private val duration: LongHistogram = meter.histogramBuilder(LLStatisticsScopes.Deserialization.ClassLike.duration())
        .setDescription("Stub class-like deserialization duration")
        .setUnit("ms")
        .ofLongs()
        .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
        .build()

    /**
     * class-like 反序列化次数。
     */
    private val runs: LongCounter = meter.counterBuilder(LLStatisticsScopes.Deserialization.ClassLike.runs())
        .setDescription("Number of stub class-like deserializations")
        .build()

    /**
     * 记录一次 class-like 反序列化。
     */
    fun onClassLikeDeserialized(elapsedNanos: Long) {
        duration.record(elapsedNanos / NANOS_PER_MILLI)
        runs.add(1)
    }
}
