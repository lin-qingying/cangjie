package org.cangnova.cangjie.ide.completion.impl.shared.statistics

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieOpenTelemetryProvider
import org.cangnova.cangjie.ide.completion.api.statistics.CangJieCompletionStatistics
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionRejection
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStage
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStatisticsSink

/**
 * 用 OpenTelemetry 记录补全统计。
 *
 * 与仓颉其它性能信号的采集方式一致（同一 `OpenTelemetry` 实例、同一导出链路），
 * 因此**不另起后端**：面板与告警沿用既有 metrics 栈。
 *
 * 指标名与属性名是**外部契约**（查询、面板、告警都按名字引用），改动即破坏看板，
 * 因此集中在本文件并各自写明语义。
 */
class OTelCompletionStatisticsSink(openTelemetry: OpenTelemetry) : CompletionStatisticsSink {
    /**
     * 各阶段耗时，单位纳秒。
     */
    private val stageDuration = openTelemetry.getMeter(METER_NAME)
        .histogramBuilder(METRIC_STAGE_DURATION)
        .ofLongs()
        .setUnit("ns")
        .setDescription("补全各阶段耗时")
        .build()

    /**
     * 候选量：按 `kind` 属性区分「算出来的」与「交付出去的」。
     */
    private val candidateCount = openTelemetry.getMeter(METER_NAME)
        .counterBuilder(METRIC_CANDIDATES)
        .setDescription("补全候选量（produced/delivered）")
        .build()

    /**
     * 未交付原因计数，按 `reason` 属性区分。
     */
    private val rejectionCount = openTelemetry.getMeter(METER_NAME)
        .counterBuilder(METRIC_REJECTIONS)
        .setDescription("候选未交付的原因计数")
        .build()

    override fun recordStage(stage: CompletionStage, nanos: Long) {
        stageDuration.record(nanos, Attributes.of(STAGE_KEY, stage.name))
    }

    override fun recordCandidates(produced: Int, delivered: Int) {
        candidateCount.add(produced.toLong(), Attributes.of(CANDIDATE_KIND_KEY, "produced"))
        candidateCount.add(delivered.toLong(), Attributes.of(CANDIDATE_KIND_KEY, "delivered"))
    }

    override fun recordRejection(reason: CompletionRejection) {
        rejectionCount.add(1, Attributes.of(REJECTION_REASON_KEY, reason.name))
    }

    companion object {
        /** 指标所在的 meter 名。 */
        const val METER_NAME: String = "org.cangnova.cangjie.completion"

        /** 阶段耗时指标名。 */
        const val METRIC_STAGE_DURATION: String = "cangjie.completion.stage.duration"

        /** 候选量指标名。 */
        const val METRIC_CANDIDATES: String = "cangjie.completion.candidates"

        /** 未交付原因指标名。 */
        const val METRIC_REJECTIONS: String = "cangjie.completion.rejections"

        /** 阶段属性。 */
        const val ATTR_STAGE: String = "cangjie.completion.stage"

        /** 候选量种类属性（produced/delivered）。 */
        const val ATTR_CANDIDATE_KIND: String = "cangjie.completion.candidate.kind"

        /** 未交付原因属性。 */
        const val ATTR_REJECTION_REASON: String = "cangjie.completion.rejection.reason"

        /** 阶段属性键（名与 [ATTR_STAGE] 一致，外部按名查询、代码里用带类型的键）。 */
        private val STAGE_KEY: AttributeKey<String> = AttributeKey.stringKey(ATTR_STAGE)

        /** 候选量种类属性键。 */
        private val CANDIDATE_KIND_KEY: AttributeKey<String> = AttributeKey.stringKey(ATTR_CANDIDATE_KIND)

        /** 未交付原因属性键。 */
        private val REJECTION_REASON_KEY: AttributeKey<String> = AttributeKey.stringKey(ATTR_REJECTION_REASON)
    }
}

/**
 * 工程里有 OpenTelemetry provider 时装上 OTel 统计实现；没有则保持 noop。
 *
 * 由宿主在装配阶段调用（IDE 插件描述符装配、LSP 注册器）。返回是否装上，
 * 便于宿主日志区分「没装」与「装了但不产信号」。
 */
@OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)
fun installOtelCompletionStatistics(project: Project): Boolean {
    val provider = CangJieOpenTelemetryProvider.getInstance(project) ?: return false
    project.service<CangJieCompletionStatistics>().sink = OTelCompletionStatisticsSink(provider.openTelemetry)
    return true
}
