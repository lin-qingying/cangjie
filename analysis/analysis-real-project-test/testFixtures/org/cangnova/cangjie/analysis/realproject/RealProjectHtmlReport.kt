package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 总览页里的一个项目条目。 */
data class RealProjectIndexEntry(
    val config: RealProjectConfig,
    val reportDir: Path,
    val ok: Boolean,
    val message: String?,
    val analyses: List<FileAnalysis>,
)

/**
 * 生成自包含的 HTML 报告（无外部资源、无服务端）。
 *
 * - 每个项目一份 `report.html`：汇总卡片、可按级别/关键字过滤的诊断表、异常栈；
 * - 所有项目一份 `index.html`：状态、计数与跳转链接。
 */
object RealProjectHtmlReport {

    private val TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** 单项目报告。 */
    fun writeProject(
        file: Path,
        config: RealProjectConfig,
        checkout: Path,
        buildResults: List<CjpmRunner.Result>,
        dependencyCjoCount: Int,
        analyses: List<FileAnalysis>,
    ) {
        val diagnostics = analyses.flatMap { analysis ->
            analysis.diagnostics.map { diagnostic -> analysis.path to diagnostic }
        }
        val errorCount = diagnostics.count { it.second.severity == "ERROR" }
        val warningCount = diagnostics.count { it.second.severity == "WARNING" }
        val infoCount = diagnostics.count { it.second.severity == "INFO" }
        val exceptionCount = analyses.count { it.error != null }
        val build = buildResults.lastOrNull()

        write(file, page("CFIR · ${esc(config.id)}") {
            header(config, checkout, build, dependencyCjoCount)
            section("summary") {
                cards(
                    "文件" to analyses.size,
                    "ERROR" to errorCount,
                    "WARNING" to warningCount,
                    "INFO" to infoCount,
                    "异常文件" to exceptionCount,
                )
            }
            section("diagnostics") {
                append(filterBar())
                append(diagnosticsTable(diagnostics))
            }
            if (exceptionCount > 0) {
                section("exceptions") {
                    append("<h2>分析异常（${exceptionCount}）</h2>")
                    analyses.filter { it.error != null }.forEach { analysis ->
                        append("<details><summary>${esc(shortPath(analysis.path))}</summary>")
                        append("<pre>${esc(analysis.error.orEmpty())}</pre></details>")
                    }
                }
            }
            section("files") {
                append("<h2>文件</h2>")
                append(filesTable(analyses))
            }
        })
    }

    /** 构建/准备失败时的报告。 */
    fun writeFailure(
        file: Path,
        config: RealProjectConfig,
        checkout: Path?,
        buildResults: List<CjpmRunner.Result>?,
        failure: Throwable,
    ) {
        val output = buildResults?.lastOrNull()?.output.orEmpty()
        write(file, page("CFIR · ${esc(config.id)}") {
            header(config, checkout, buildResults?.lastOrNull(), null)
            section("failure") {
                append("<h2>未进入分析</h2>")
                append("<pre>${esc(failure.stackTraceToString())}</pre>")
                if (output.isNotBlank()) {
                    append("<h3>cjpm 输出</h3><pre>${esc(output.takeLast(20_000))}</pre>")
                }
            }
        })
    }

    /** 总览页。 */
    fun writeIndex(file: Path, entries: List<RealProjectIndexEntry>) {
        write(file, page("CFIR 真实项目报告") {
            append("<header><h1>CFIR 真实项目报告</h1>")
            append("<div class=meta>${entries.size} 个项目 · ${TIME_FORMAT.format(LocalDateTime.now())}</div></header>")
            append("<section class=grid>")
            entries.forEach { entry ->
                val diagnostics = entry.analyses.flatMap { it.diagnostics }
                val errors = diagnostics.count { it.severity == "ERROR" }
                val exceptions = entry.analyses.count { it.error != null }
                val status = when {
                    !entry.ok -> "失败"
                    exceptions > 0 -> "有异常"
                    errors > 0 -> "有错误"
                    else -> "通过"
                }
                val statusClass = when {
                    !entry.ok -> "bad"
                    exceptions > 0 || errors > 0 -> "warn"
                    else -> "good"
                }
                val relative = entry.reportDir.fileName.toString()
                append("<article class=card>")
                append("<h2><a href=\"$relative/report.html\">${esc(entry.config.id)}</a></h2>")
                append("<p class=repo>${esc(entry.config.repo)} @ ${esc(entry.config.ref)}</p>")
                append("<p><span class=\"status $statusClass\">$status</span></p>")
                append("<p>文件 ${entry.analyses.size} · ERROR $errors · 异常文件 $exceptions</p>")
                entry.message?.let { append("<p class=msg>${esc(it)}</p>") }
                append("<p><a href=\"$relative/report.html\">HTML</a> · <a href=\"$relative/cfir.json\">JSON</a></p>")
                append("</article>")
            }
            append("</section>")
        })
    }

    private fun HtmlBuilder.header(
        config: RealProjectConfig,
        checkout: Path?,
        build: CjpmRunner.Result?,
        dependencyCjoCount: Int?,
    ) {
        append("<header><h1>${esc(config.id)}</h1>")
        append("<div class=meta>${esc(config.repo)} @ ${esc(config.ref)}")
        checkout?.let { append(" · ${esc(it.toString())}") }
        build?.let { append(" · cjpm exit ${it.exitCode} · ${it.durationMillis} ms") }
        dependencyCjoCount?.let { append(" · 依赖 cjo $it") }
        append(" · ${TIME_FORMAT.format(LocalDateTime.now())}</div></header>")
    }

    private fun HtmlBuilder.cards(vararg items: Pair<String, Int>) {
        append("<div class=cards>")
        items.forEach { (label, value) ->
            append("<div class=card><div class=num>$value</div><div class=label>${esc(label)}</div></div>")
        }
        append("</div>")
    }

    private fun filterBar(): String = """
        <div class=toolbar>
          <input id=q placeholder="搜索文件 / 诊断名 / 消息">
          <label class=sev><input type=checkbox data-sev=ERROR checked> ERROR</label>
          <label class=sev><input type=checkbox data-sev=WARNING checked> WARNING</label>
          <label class=sev><input type=checkbox data-sev=INFO checked> INFO</label>
          <span id=count></span>
        </div>
    """.trimIndent()

    private fun diagnosticsTable(diagnostics: List<Pair<String, DiagnosticRecord>>): String = buildString {
        append("<table id=diag><thead><tr>")
        append("<th>位置</th><th>级别</th><th>诊断</th><th>消息</th><th>代码</th>")
        append("</tr></thead><tbody>")
        diagnostics
            .sortedWith(compareBy({ severityRank(it.second.severity) }, { it.first }, { it.second.line }))
            .forEach { (path, diagnostic) ->
                val location = "${esc(shortPath(path))}:${diagnostic.line}:${diagnostic.column}"
                append("<tr data-sev=\"${esc(diagnostic.severity)}\">")
                append("<td class=loc>$location</td>")
                append("<td><span class=sev-${diagnostic.severity.lowercase()}>${esc(diagnostic.severity)}</span></td>")
                append("<td><code>${esc(diagnostic.name)}</code></td>")
                append("<td>${esc(diagnostic.message)}</td>")
                append("<td><code>${esc(diagnostic.snippet)}</code></td>")
                append("</tr>")
            }
        append("</tbody></table>")
        append("<script>$FILTER_SCRIPT</script>")
    }

    private fun filesTable(analyses: List<FileAnalysis>): String = buildString {
        append("<table><thead><tr><th>文件</th><th>诊断</th><th>ERROR</th><th>异常</th><th>耗时</th></tr></thead><tbody>")
        analyses.sortedBy { it.path }.forEach { analysis ->
            val errors = analysis.diagnostics.count { it.severity == "ERROR" }
            append("<tr><td>${esc(shortPath(analysis.path))}</td>")
            append("<td>${analysis.diagnostics.size}</td>")
            append("<td>${if (errors > 0) "<b class=sev-error>$errors</b>" else "0"}</td>")
            append("<td>${if (analysis.error != null) "<b class=sev-error>是</b>" else "否"}</td>")
            append("<td>${analysis.durationMillis} ms</td></tr>")
        }
        append("</tbody></table>")
    }

    private fun severityRank(severity: String): Int = when (severity) {
        "ERROR" -> 0
        "WARNING" -> 1
        else -> 2
    }

    private fun shortPath(path: String): String =
        path.replace('\\', '/').substringAfterLast("/src/", path.replace('\\', '/')).removePrefix("src/")

    private fun write(file: Path, html: String) {
        Files.createDirectories(file.parent)
        Files.writeString(file, html)
    }

    private fun page(title: String, body: HtmlBuilder.() -> Unit): String = buildString {
        append("<!doctype html><html lang=zh><head><meta charset=utf-8>")
        append("<title>$title</title>")
        append("<style>$STYLE</style></head><body>")
        val builder = HtmlBuilder()
        builder.body()
        append(builder)
        append("</body></html>")
    }

    private fun esc(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private fun HtmlBuilder.section(name: String, body: HtmlBuilder.() -> Unit) {
        append("<section data-section=\"$name\">")
        body()
        append("</section>")
    }

    /** 简易字符串构建器，避免在生成 HTML 时层层字符串拼接。 */
    class HtmlBuilder {
        private val builder = StringBuilder()
        fun append(value: String): HtmlBuilder = apply { builder.append(value) }
        override fun toString(): String = builder.toString()
    }

    private const val STYLE = """
        :root { color-scheme: light dark; }
        body { font: 14px/1.5 -apple-system, "Segoe UI", sans-serif; margin: 24px; color: #222; background: #fff; }
        h1 { margin: 0 0 4px; font-size: 22px; }
        h2 { font-size: 17px; margin: 24px 0 8px; }
        .meta, .repo, .msg { color: #666; font-size: 12px; word-break: break-all; }
        .cards { display: flex; gap: 12px; flex-wrap: wrap; margin: 16px 0; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 10px 14px; min-width: 110px; }
        .num { font-size: 22px; font-weight: 600; }
        .label { color: #666; font-size: 12px; }
        table { border-collapse: collapse; width: 100%; font-size: 13px; }
        th, td { border-bottom: 1px solid #eee; padding: 6px 8px; text-align: left; vertical-align: top; }
        th { position: sticky; top: 0; background: #fafafa; }
        code { font-family: Consolas, monospace; font-size: 12px; }
        .loc { white-space: nowrap; color: #555; }
        .sev-error { color: #c62828; }
        .sev-warning { color: #b26a00; }
        .sev-info { color: #1565c0; }
        .toolbar { display: flex; gap: 12px; align-items: center; margin: 12px 0; flex-wrap: wrap; }
        .toolbar input[type=text], .toolbar input:not([type]) { padding: 6px 8px; width: 280px; border: 1px solid #ccc; border-radius: 6px; }
        .sev { font-size: 12px; }
        pre { background: #f7f7f7; padding: 10px; overflow: auto; max-height: 420px; border-radius: 6px; }
        details { margin: 8px 0; }
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
        .grid .card { min-width: 0; }
        .status { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
        .status.good { background: #e8f5e9; color: #2e7d32; }
        .status.warn { background: #fff4e5; color: #b26a00; }
        .status.bad { background: #fdecea; color: #c62828; }
        a { color: #1565c0; }
        @media (prefers-color-scheme: dark) {
          body { background: #1e1e1e; color: #ddd; }
          .meta, .repo, .msg, .label { color: #999; }
          .card { border-color: #444; }
          th { background: #2a2a2a; }
          th, td { border-color: #333; }
          pre { background: #2a2a2a; }
          code { color: #ddd; }
        }
    """

    private const val FILTER_SCRIPT = """
        (function () {
          var q = document.getElementById('q');
          var boxes = Array.prototype.slice.call(document.querySelectorAll('input[data-sev]'));
          var rows = Array.prototype.slice.call(document.querySelectorAll('#diag tbody tr'));
          var count = document.getElementById('count');
          function apply() {
            var text = (q && q.value || '').toLowerCase();
            var enabled = {};
            boxes.forEach(function (b) { enabled[b.dataset.sev] = b.checked; });
            var shown = 0;
            rows.forEach(function (row) {
              var okSev = enabled[row.dataset.sev] !== false;
              var okText = !text || row.textContent.toLowerCase().indexOf(text) >= 0;
              var visible = okSev && okText;
              row.style.display = visible ? '' : 'none';
              if (visible) shown++;
            });
            if (count) count.textContent = shown + ' / ' + rows.length;
          }
          if (q) q.addEventListener('input', apply);
          boxes.forEach(function (b) { b.addEventListener('change', apply); });
          apply();
        })();
    """
}
