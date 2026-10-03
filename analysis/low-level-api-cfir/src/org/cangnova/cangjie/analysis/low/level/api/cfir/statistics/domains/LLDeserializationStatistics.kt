package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.DURATION_BUCKETS_MS
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsScopes
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.NANOS_PER_MILLI
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.getMeter
import org.cangnova.cangjie.cfir.serialization.provider.CfirCjoDeserializationStage
import org.cangnova.cangjie.cfir.serialization.provider.CfirCjoDeserializationTimingObserver

/**
 * 反序列化耗时统计域，两条通道。
 *
 * **stub 通道**（`deserialization.classLike`）：库模块的 class-like 符号首次被查询时才会从 stub
 * 反序列化成 CFIR 声明，这一步发生在用户动作中间而不是工程打开时，因此它是
 * "IDE 卡在某个符号上"的常见来源：一次类型解析可能触发一整棵嵌套类的反序列化。
 * 只记耗时与次数，不记产物大小——反序列化的类成员数没有稳定的观测点可取，
 * 硬凑一个会给出错误的心智模型。
 *
 * **`.cjo` 通道**（`deserialization.cjo.<stage>`）：stdlib 等库在 IDE 里是以 `.cjo` 加载的，
 * 打开工程时的开销主要落在这里。它经 [CfirCjoDeserializationTimingObserver] 从 `.cjo` 反序列化侧
 * 上报，因此同一个域对象同时服务两条通道：stub 侧直接调方法，`.cjo` 侧走 session 组件。
 * 两条通道在同一进程里都会发生（`.cjo` 库与源码库混用），分开统计才有诊断价值。
 *
 * 与 Kotlin 对照：Kotlin 的 `LLStatisticsService` 不统计反序列化；这是仓颉为定位 IDE 卡顿补出的域。
 */
class LLDeserializationStatistics(statisticsService: LLStatisticsService) :
    CfirCjoDeserializationTimingObserver,
    LLStatisticsDomain {
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
     * `.cjo` 各阶段反序列化耗时直方图（毫秒）。
     */
    private val cjoDurations: Map<CfirCjoDeserializationStage, LongHistogram> =
        CfirCjoDeserializationStage.entries.associateWith { stage ->
            meter.histogramBuilder(LLStatisticsScopes.Deserialization.Cjo.stage(stage).duration())
                .setDescription("CJO deserialization duration by stage")
                .setUnit("ms")
                .ofLongs()
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS_MS)
                .build()
        }

    /**
     * `.cjo` 各阶段反序列化次数。
     */
    private val cjoRuns: Map<CfirCjoDeserializationStage, LongCounter> =
        CfirCjoDeserializationStage.entries.associateWith { stage ->
            meter.counterBuilder(LLStatisticsScopes.Deserialization.Cjo.stage(stage).runs())
                .setDescription("Number of CJO deserialization runs by stage")
                .build()
        }

    /**
     * `.cjo` 按声明惰性反序列化的声明总数。
     */
    private val cjoDeclarations: LongCounter =
        meter.counterBuilder(LLStatisticsScopes.Deserialization.Cjo.Declaration.declarations())
            .setDescription("Number of declarations deserialized from CJO on demand")
            .build()

    /**
     * 记录一次 class-like stub 反序列化。
     */
    fun onClassLikeDeserialized(elapsedNanos: Long) {
        duration.record(elapsedNanos / NANOS_PER_MILLI)
        runs.add(1)
    }

    /**
     * 记录一次 `.cjo` 反序列化阶段。
     *
     * 声明数只在按声明惰性反序列化阶段计入：包加载加载的是整个包，"这次涉及几个声明"
     * 没有观测点，强行折算会造出一个看起来精确、实际是编的数字。
     */
    override fun onCjoDeserializationFinished(
        stage: CfirCjoDeserializationStage,
        elapsedNanos: Long,
        declarationCount: Int,
    ) {
        cjoDurations[stage]?.record(elapsedNanos / NANOS_PER_MILLI)
        cjoRuns[stage]?.add(1)
        if (stage == CfirCjoDeserializationStage.DECLARATION) {
            cjoDeclarations.add(declarationCount.toLong())
        }
    }
}
