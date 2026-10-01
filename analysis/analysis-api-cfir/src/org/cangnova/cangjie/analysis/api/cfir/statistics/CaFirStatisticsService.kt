
package org.cangnova.cangjie.analysis.api.cfir.statistics

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.statistics.CaStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService

/**
 * CFIR Analysis API 的统计服务实现（对齐 Kotlin `KaFirStatisticsService`）。
 *
 * 引擎侧统计生命周期只有一处与平台对接：[start] 把周期调度交给
 * [LLStatisticsService]。何时调用它由平台决定（Kotlin 侧由 IntelliJ 的
 * `IdeKotlinStatisticsStartupActivity` 在工程打开后触发），本模块不自行注册启动点。
 *
 * [LLStatisticsService.getInstance] 在统计开关关闭或平台未提供 OpenTelemetry 实例时
 * 返回 `null`，此时 [start] 是空操作。
 */
internal class CaFirStatisticsService(private val project: Project) : CaStatisticsService {
    /**
     * 当前工程可用的 low-level 统计服务；统计未启用时为 `null`。
     */
    private val statisticsService: LLStatisticsService?
        get() = LLStatisticsService.getInstance(project)

    /**
     * 启动 low-level 统计服务的周期调度任务。
     */
    override fun start() {
        statisticsService?.start()
    }
}
