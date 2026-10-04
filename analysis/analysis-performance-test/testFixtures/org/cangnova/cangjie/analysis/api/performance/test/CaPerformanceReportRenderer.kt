package org.cangnova.cangjie.analysis.api.performance.test

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeText

/**
 * 把一次运行的对比结果渲染成控制台文本与单文件 HTML。
 *
 * 两种呈现共用同一份对比结果与同一套取值格式化，避免两边数字对不上。
 *
 * HTML 是**自包含**的：样式内联、不引外部资源、不依赖网络。性能数据经常在无外网的环境里看，
 * 一个还要联网才能显示的图表等于没有。
 */
object CaPerformanceReportRenderer {
    /**
     * 报告文件名。
     */
    const val REPORT_FILE_NAME: String = "performance-report.html"

    /**
     * 纯文本摘要文件名；与 HTML 同内容，供终端查看与 CI 日志留档。
     */
    const val TEXT_REPORT_FILE_NAME: String = "performance-report.txt"

    private val timestampFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    /**
     * 渲染控制台摘要；只列需要关注的信号，避免刷屏。
     */
    fun renderConsole(report: CaPerformanceComparisonReport, run: CaPerformanceRunRecord): String {
        val lines = StringBuilder()
        lines.appendLine("─".repeat(78))
        lines.appendLine("性能基线对比  运行 ${report.runId}  基线 ${report.baselineRunId ?: "无（首次运行）"}")
        run.gitCommit?.let { lines.appendLine("  commit $it") }
        lines.appendLine("─".repeat(78))

        if (report.baselineRunId == null) {
            lines.appendLine("  本次已记录 ${run.cases.size} 个用例的基线数据；下次运行起可对比。")
            lines.append("─".repeat(78))
            return lines.toString()
        }

        val regressions = report.regressions
        if (regressions.isEmpty()) {
            lines.appendLine("  没有检出超过抖动阈值的变慢信号。")
        } else {
            regressions.forEach { case ->
                lines.appendLine("  ✗ ${case.caseId}")
                case.signals.filter { it.verdict == CaPerformanceVerdict.REGRESSED }.forEach {
                    lines.appendLine(
                        "      ${it.signalName}: ${formatValue(it.signalName, it.current)}" +
                            " vs ${it.baselineDescription} ${formatValue(it.signalName, it.baseline)}" +
                            " (+${formatPercent(it.relativeChange)})",
                    )
                }
            }
        }

        val improved = report.cases.flatMap { it.signals }.filter { it.verdict == CaPerformanceVerdict.IMPROVED }
        if (improved.isNotEmpty()) {
            lines.appendLine("  变快信号 ${improved.size} 项（见 HTML 报告）")
        }
        val insufficient = report.cases.flatMap { it.signals }
            .count { it.verdict == CaPerformanceVerdict.INSUFFICIENT_DATA }
        if (insufficient > 0) {
            lines.appendLine("  其中 $insufficient 项历史样本不足，暂不能判定")
        }
        lines.append("─".repeat(78))
        return lines.toString()
    }

    /**
     * 渲染并写出单文件 HTML 与纯文本摘要；失败只返回 false，不抛出。
     *
     * 文本摘要必须落盘：Gradle 在构建成功时默认丢弃测试进程的 stdout，只把摘要写进
     * 测试进程里等于没写——这条通路曾经就是这样静默失效的。
     */
    fun writeReports(report: CaPerformanceComparisonReport, run: CaPerformanceRunRecord, directory: Path): Boolean {
        val htmlOk = runCatching {
            Files.createDirectories(directory)
            directory.resolve(REPORT_FILE_NAME).writeText(renderHtml(report, run))
            true
        }.getOrElse { false }
        val textOk = runCatching {
            Files.createDirectories(directory)
            directory.resolve(TEXT_REPORT_FILE_NAME).writeText(renderConsole(report, run))
            true
        }.getOrElse { false }
        return htmlOk && textOk
    }

    /**
     * 渲染单文件 HTML。
     */
    fun renderHtml(report: CaPerformanceComparisonReport, run: CaPerformanceRunRecord): String {
        val body = StringBuilder()
        body.append(htmlHeader())
        body.append("<h1>性能基线对比</h1>")
        body.append(summaryBlock(report, run))

        if (report.baselineRunId == null) {
            body.append(
                "<p class=\"hint\">本次是基线起点：已记录 ${run.cases.size} 个用例的数据，" +
                    "下次运行起会与它对比。连续积累 ${CaPerformanceBaselineComparator.MIN_BASELINE_SAMPLES} 次后，" +
                    "基线自动改用最近若干次的中位数。</p>",
            )
        } else {
            report.cases.forEach { case -> body.append(caseBlock(case)) }
        }

        body.append(htmlFooter(run))
        return body.toString()
    }

    private fun summaryBlock(report: CaPerformanceComparisonReport, run: CaPerformanceRunRecord): String {
        val allSignals = report.cases.flatMap { it.signals }
        val regressions = allSignals.count { it.verdict == CaPerformanceVerdict.REGRESSED }
        val improvements = allSignals.count { it.verdict == CaPerformanceVerdict.IMPROVED }
        val noise = allSignals.count { it.verdict == CaPerformanceVerdict.NOISE }
        val insufficient = allSignals.count { it.verdict == CaPerformanceVerdict.INSUFFICIENT_DATA }
        val missing = allSignals.count { it.verdict == CaPerformanceVerdict.BASELINE_MISSING }

        return buildString {
            append("<div class=\"meta\">")
            append("<div>运行 <code>${escape(report.runId)}</code></div>")
            append("<div>基线 <code>${escape(report.baselineRunId ?: "无")}</code></div>")
            append("<div>用例 <strong>${run.cases.size}</strong></div>")
            run.gitCommit?.let { append("<div>commit <code>${escape(it)}</code></div>") }
            append("<div>${escape(formatTimestamp(run.timestampMillis))}</div>")
            append("</div>")
            append("<div class=\"tiles\">")
            append(tile("变慢", regressions, "bad"))
            append(tile("变快", improvements, "good"))
            append(tile("噪声内", noise, "muted"))
            append(tile("样本不足", insufficient, "warn"))
            append(tile("无基线", missing, "muted"))
            append("</div>")
        }
    }

    private fun tile(label: String, value: Int, kind: String): String =
        "<div class=\"tile $kind\"><div class=\"tile-value\">$value</div><div class=\"tile-label\">$label</div></div>"

    private fun caseBlock(case: CaPerformanceCaseComparison): String = buildString {
        append("<section>")
        append("<h2>${escape(case.caseId)}</h2>")
        append("<table><thead><tr>")
        append("<th>信号</th><th>本次</th><th>基线</th><th>基线来源</th><th>变化</th><th>阈值</th><th>判定</th>")
        append("</tr></thead><tbody>")
        case.signals.forEach { append(signalRow(it)) }
        append("</tbody></table>")
        append("</section>")
    }

    private fun signalRow(signal: CaPerformanceSignalComparison): String {
        val change = signal.relativeChange?.let {
            (if (it > 0) "+" else "") + formatPercent(it)
        } ?: "—"
        val barWidth = signal.relativeChange?.let {
            // 条形长度按变化幅度映射到 0..50%，50% 画满，视觉上区分量级。
            ((kotlin.math.abs(it) / 0.5).coerceIn(0.0, 1.0) * 50).toInt()
        } ?: 0
        return buildString {
            append("<tr class=\"${verdictClass(signal.verdict)}\">")
            append("<td class=\"name\">${escape(signal.signalName)}</td>")
            append("<td>${escape(formatValue(signal.signalName, signal.current))}</td>")
            append("<td>${escape(formatValue(signal.signalName, signal.baseline))}</td>")
            append("<td>${escape(signal.baselineDescription)} (${signal.baselineSamples})</td>")
            append("<td class=\"change\">$change</td>")
            append("<td>${escape(formatPercent(signal.threshold))}</td>")
            append("<td><span class=\"badge ${verdictClass(signal.verdict)}\">${verdictLabel(signal.verdict)}</span>")
            append("<div class=\"bar\"><div class=\"fill\" style=\"width:$barWidth%\"></div></div></td>")
            append("</tr>")
        }
    }

    private fun verdictClass(verdict: CaPerformanceVerdict): String = when (verdict) {
        CaPerformanceVerdict.REGRESSED -> "bad"
        CaPerformanceVerdict.IMPROVED -> "good"
        CaPerformanceVerdict.NOISE -> "muted"
        CaPerformanceVerdict.INSUFFICIENT_DATA -> "warn"
        CaPerformanceVerdict.BASELINE_MISSING -> "muted"
    }

    private fun verdictLabel(verdict: CaPerformanceVerdict): String = when (verdict) {
        CaPerformanceVerdict.REGRESSED -> "变慢"
        CaPerformanceVerdict.IMPROVED -> "变快"
        CaPerformanceVerdict.NOISE -> "噪声内"
        CaPerformanceVerdict.INSUFFICIENT_DATA -> "样本不足"
        CaPerformanceVerdict.BASELINE_MISSING -> "无基线"
    }

    /**
     * 按信号类型格式化取值。
     *
     * 耗时信号采集时已统一归一为**纳秒**，因此这里不再需要判断指标单位——
     * 之前按指标名猜 `ms` / `us` 的写法会把逐诊断组件的微秒值显示成毫秒。
     */
    internal fun formatValue(signalName: String, nanosValue: Double): String = when {
        isTimingSignal(signalName) -> {
            val millis = nanosValue / 1_000_000.0
            // 亚毫秒信号保留 4 位小数，否则在噪声底附近会显示成一串 0。
            if (millis >= 1.0) "%.3f ms".format(millis) else "%.4f ms".format(millis)
        }
        signalName.startsWith("计数·") -> "%.0f".format(nanosValue)
        else -> "%.3f".format(nanosValue)
    }

    private fun formatPercent(value: Double?): String =
        value?.let { "%.1f%%".format(it * 100) } ?: "—"

    private fun formatTimestamp(millis: Long): String =
        timestampFormat.format(Instant.ofEpochMilli(millis))

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun htmlHeader(): String = """
        <!DOCTYPE html>
        <html lang="zh-CN"><head><meta charset="utf-8">
        <title>性能基线对比</title>
        <style>
        :root { color-scheme: light dark; }
        body { font: 14px/1.6 -apple-system, "Segoe UI", "Microsoft YaHei", sans-serif; margin: 32px; }
        h1 { font-size: 20px; margin-bottom: 4px; }
        h2 { font-size: 14px; font-family: ui-monospace, Consolas, monospace; margin: 24px 0 6px; }
        .meta { color: #666; font-size: 12px; display: flex; gap: 18px; flex-wrap: wrap; }
        .tiles { display: flex; gap: 10px; margin: 18px 0; flex-wrap: wrap; }
        .tile { border: 1px solid #d8d8d8; border-radius: 8px; padding: 10px 18px; min-width: 92px; }
        .tile-value { font-size: 22px; font-weight: 600; }
        .tile-label { font-size: 12px; color: #666; }
        .tile.bad .tile-value { color: #c0392b; }
        .tile.good .tile-value { color: #1e8449; }
        .tile.warn .tile-value { color: #b9770e; }
        .tile.muted .tile-value { color: #888; }
        table { border-collapse: collapse; width: 100%; font-size: 13px; }
        th, td { border-bottom: 1px solid #e6e6e6; padding: 5px 8px; text-align: left; }
        th { color: #666; font-weight: 500; }
        td.name { font-family: ui-monospace, Consolas, monospace; }
        td.change { font-variant-numeric: tabular-nums; }
        tr.bad td.name { color: #c0392b; }
        tr.good td.name { color: #1e8449; }
        .badge { display: inline-block; padding: 1px 7px; border-radius: 10px; font-size: 11px; }
        .badge.bad { background: #fadbd8; color: #c0392b; }
        .badge.good { background: #d4efdf; color: #1e8449; }
        .badge.warn { background: #fdebd0; color: #b9770e; }
        .badge.muted { background: #eaeaea; color: #777; }
        .bar { height: 3px; background: #eaeaea; margin-top: 4px; border-radius: 2px; }
        .bar .fill { height: 3px; background: #bbb; border-radius: 2px; }
        tr.bad .bar .fill { background: #c0392b; }
        tr.good .bar .fill { background: #1e8449; }
        .hint { background: #eef4fb; border-left: 3px solid #4a90d9; padding: 10px 14px; }
        footer { margin-top: 32px; color: #888; font-size: 12px; border-top: 1px solid #e6e6e6; padding-top: 10px; }
        </style></head><body>
    """.trimIndent()

    private fun htmlFooter(run: CaPerformanceRunRecord): String = buildString {
        append("<footer>")
        append("共 ${run.cases.size} 个用例。判定阈值取「历史样本观测抖动」与「固定下限」的较大者，")
        append("只有超过阈值才判为变慢/变快。基线在攒够 ")
        append("${CaPerformanceBaselineComparator.MIN_BASELINE_SAMPLES} 次历史后自动改用中位数。")
        append("</footer></body></html>")
    }
}