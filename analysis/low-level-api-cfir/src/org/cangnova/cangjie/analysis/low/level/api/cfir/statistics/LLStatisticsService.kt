/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.opentelemetry.api.OpenTelemetry
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.statistics.CaStatisticsService
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieOpenTelemetryProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLAnalysisSessionStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticPassStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLMacroConstructionStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLRawBuildStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLResolvePhaseStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSessionStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLStatisticsDomain
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSymbolProviderStatistics

/**
 * [LLStatisticsService] is the facilitator of low-level API statistics collection and reporting. The service manages the scheduler and the
 * individual [LLStatisticsDomain]s.
 *
 * The Analysis API uses [OpenTelemetry](https://opentelemetry.io) as a telemetry backend to report metrics to the Analysis API platform,
 * such as IntelliJ and its performance tests. See [CangJieOpenTelemetryProvider].
 *
 * This class is the only IntelliJ project service registered for low-level API statistics collection. The single entry point simplifies
 * handling of whether statistics are enabled (see [CaStatisticsService.areStatisticsEnabled]).
 */
class LLStatisticsService(internal val project: Project) : Disposable {
    /**
     * 周期性更新统计域的调度器。
     */
    internal val scheduler: LLStatisticsScheduler = LLStatisticsScheduler(this)

    /**
     * analysis session 统计域。
     */
    val analysisSessions: LLAnalysisSessionStatistics = LLAnalysisSessionStatistics(this)

    /**
     * symbol provider 统计域。
     */
    internal val symbolProviders: LLSymbolProviderStatistics = LLSymbolProviderStatistics(this)

    /**
     * 语义解析阶段耗时统计域；各 session 注册的阶段耗时观察者即指向该域。
     */
    internal val resolvePhases: LLResolvePhaseStatistics = LLResolvePhaseStatistics(this)

    /**
     * raw CFIR 构建耗时统计域；各 session 注册的 raw build 观察者即指向该域。
     */
    val rawBuild: LLRawBuildStatistics = LLRawBuildStatistics(this)

    /**
     * macro construction 耗时统计域；各 session 注册的宏构造观察者即指向该域。
     */
    val macroConstruction: LLMacroConstructionStatistics = LLMacroConstructionStatistics(this)

    /**
     * IDE 请求级诊断收集耗时统计域。
     *
     * 这三个域（诊断、session 创建、stub 反序列化）不由 session 观察者驱动，而是被调用方
     * 直接持有：它们的位置都在 low-level 模块内，与本服务同模块，无需绕 session component。
     */
    val diagnostics: LLDiagnosticsStatistics = LLDiagnosticsStatistics(this)

    /**
     * 诊断遍历分阶段耗时统计域；session 注册的遍历观察者指向该域。
     */
    val diagnosticPasses: LLDiagnosticPassStatistics = LLDiagnosticPassStatistics(this)

    /**
     * session 创建与 scope session 统计域。
     */
    val sessions: LLSessionStatistics = LLSessionStatistics(this)

    /**
     * stub 反序列化统计域。
     */
    val deserialization: LLDeserializationStatistics = LLDeserializationStatistics(this)

    /**
     * 当前服务管理的所有统计域。
     */
    internal val domains: List<LLStatisticsDomain> =
        listOf(
            analysisSessions,
            symbolProviders,
            resolvePhases,
            rawBuild,
            macroConstruction,
            diagnostics,
            diagnosticPasses,
            sessions,
            deserialization,
        )

    @OptIn(CaPlatformInterface::class)
    /**
     * 平台提供的 OpenTelemetry 实例。
     */
    internal val openTelemetry: OpenTelemetry
        get() = CangJieOpenTelemetryProvider.getInstance(project)?.openTelemetry
            ?: error("${LLStatisticsService::class.simpleName} should not be used when OpenTelemetry is not available.")

    /**
     * 统计服务是否已经启动。
     */
    private var hasStarted: Boolean = false

    /**
     * Schedules periodic updates and information gathering if statistics collection is [enabled][CaStatisticsService.areStatisticsEnabled].
     * These tasks contribute to the collected statistics.
     *
     * Meters don't require [start] for initialization and can be used even before [start] has been called.
     *
     * Statistics collection will remain active until disposal of this service.
     */
    fun start() {
        synchronized(this) {
            if (hasStarted) return

            scheduler.schedule()
            hasStarted = true
        }
    }

    /**
     * 停止周期统计任务。
     */
    override fun dispose() {
        synchronized(this) {
            if (hasStarted) {
                scheduler.cancel()
            }
        }
    }

    companion object {
        /**
         * Returns an instance of [LLStatisticsService] *if* statistics are [enabled][CaStatisticsService.areStatisticsEnabled] and an
         * [OpenTelemetry] instance is available via [CangJieOpenTelemetryProvider].
         */
        @OptIn(CaPlatformInterface::class)
        fun getInstance(project: Project): LLStatisticsService? {
            if (!CaStatisticsService.areStatisticsEnabled) {
                return null
            }

            // To avoid a nullable OpenTelemetry instance throughout the statistics code, we require OpenTelemetry to be available for
            // `LLStatisticsService`.
            if (CangJieOpenTelemetryProvider.getInstance(project)?.openTelemetry == null) {
                return null
            }

            return project.service()
        }
    }
}
