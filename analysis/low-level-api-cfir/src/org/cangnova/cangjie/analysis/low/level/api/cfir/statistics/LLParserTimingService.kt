package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLParserStatistics
import org.cangnova.cangjie.parsing.CangjiePsiParseKind
import org.cangnova.cangjie.parsing.CangjiePsiParseTimingService

/**
 * 把 PSI 解析入口的上报转发到工程级 low-level 统计服务。
 *
 * 这一层薄壳是必需的：IntelliJ 的 projectService 实现由平台按 `(Project)` 构造，而
 * [LLParserStatistics] 需要 `LLStatisticsService`（它内部还要经过 registry key 与 OpenTelemetry
 * provider 两道开关）。统计未启用时 [LLStatisticsService.getInstance] 返回 null，本类自动
 * 退化为无操作，解析路径不产生任何指标。
 */
class LLParserTimingService(project: Project) : CangjiePsiParseTimingService() {
    /**
     * 实际统计域；统计未启用时为 null。
     */
    private val delegate: LLParserStatistics? = LLStatisticsService.getInstance(project)?.parser

    override fun onParseStarted(kind: CangjiePsiParseKind) {
        delegate?.onParseStarted(kind)
    }

    override fun onParseFinished(kind: CangjiePsiParseKind, elapsedNanos: Long, succeeded: Boolean) {
        delegate?.onParseFinished(kind, elapsedNanos, succeeded)
    }
}
