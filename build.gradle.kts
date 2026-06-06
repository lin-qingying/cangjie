import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.inject.Inject

plugins {
    base
    idea
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinJvm) apply false
    id("common-configuration") apply false
    id("cangjie-publishing") apply false
    id("analysis-coverage-convention") apply false
    id("project-tests-convention") apply false

 
}

val cangjieVersion = providers.gradleProperty("cangjieVersion").get()

allprojects {
    group = "org.cangnova.cangjie"
    version = cangjieVersion

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xskip-prerelease-check")
            freeCompilerArgs.add("-Xjvm-default=all")
            freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    pluginManager.apply("common-configuration")
}

dependencies {
    // 根聚合覆盖率只消费各模块发布的 Kover artifact，不能传递解析模块的普通 runtime 依赖。
    kover(project(":analysis:analysis-api-cfir")) { isTransitive = false }
    kover(project(":analysis:cj-references")) { isTransitive = false }
    kover(project(":analysis:light-declarations")) { isTransitive = false }
    kover(project(":analysis:symbol-light-declarations")) { isTransitive = false }
    kover(project(":analysis:stubs")) { isTransitive = false }
    kover(project(":analysis:decompiled")) { isTransitive = false }
}

kover {
    reports {
        total {
            filters {
                excludes {
                    annotatedBy("*Generated*")
                }
            }
            html {
                onCheck = false
                title = "Analysis Coverage"
            }
            xml {
                onCheck = false
            }
        }
    }
}

tasks.register("reportAnalysisCoverage") {
    group = "verification"
    description = "Generate per-module and aggregated analysis coverage reports."
    dependsOn("koverHtmlReport", "koverXmlReport")
    dependsOn(
        ":analysis:analysis-api-cfir:koverHtmlReport",
        ":analysis:analysis-api-cfir:koverXmlReport",
        ":analysis:cj-references:koverHtmlReport",
        ":analysis:cj-references:koverXmlReport",
        ":analysis:light-declarations:koverHtmlReport",
        ":analysis:light-declarations:koverXmlReport",
        ":analysis:symbol-light-declarations:koverHtmlReport",
        ":analysis:symbol-light-declarations:koverXmlReport",
        ":analysis:stubs:koverHtmlReport",
        ":analysis:stubs:koverXmlReport",
        ":analysis:decompiled:koverHtmlReport",
        ":analysis:decompiled:koverXmlReport",
    )
}

tasks.register("verifyAnalysisCoverage") {
    group = "verification"
    description = "Verify module-level analysis coverage thresholds."
    dependsOn(
        ":analysis:analysis-api-cfir:koverVerify",
        ":analysis:cj-references:koverVerify",
        ":analysis:light-declarations:koverVerify",
        ":analysis:symbol-light-declarations:koverVerify",
        ":analysis:stubs:koverVerify",
        ":analysis:decompiled:koverVerify",
    )
}

tasks.matching { it.name == "checkAnalysisFramework" }.configureEach {
    dependsOn("verifyAnalysisCoverage")
}

abstract class CodeStatsToolTask : DefaultTask() {
    @get:Input
    abstract val toolExecutable: Property<String>

    @get:Input
    abstract val toolArguments: ListProperty<String>

    @get:Input
    abstract val captureStandardOutput: Property<Boolean>

    @get:Internal
    abstract val executionDirectory: DirectoryProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:OutputFile
    abstract val errorLogFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        val outputFile = reportFile.get().asFile
        val errorFile = errorLogFile.get().asFile
        outputFile.parentFile.mkdirs()
        errorFile.parentFile.mkdirs()

        fun executeWith(standardOutputStream: OutputStream?, errorOutputStream: OutputStream) {
            execOperations.exec {
                executable = toolExecutable.get()
                args(toolArguments.get())
                workingDir(executionDirectory.get().asFile)
                if (standardOutputStream != null) {
                    standardOutput = standardOutputStream
                }
                errorOutput = errorOutputStream
            }
        }

        errorFile.outputStream().use { error ->
            if (captureStandardOutput.get()) {
                outputFile.outputStream().use { output ->
                    executeWith(output, error)
                }
            } else {
                execOperations.exec {
                    executable = toolExecutable.get()
                    args(toolArguments.get())
                    workingDir(executionDirectory.get().asFile)
                    errorOutput = error
                }
            }
        }
    }
}

data class CodeStatsEntry(
    val language: String,
    val fileName: String,
    val blank: Long,
    val comment: Long,
    val code: Long,
) {
    val total: Long
        get() = blank + comment + code
}

data class CodeStatsGroup(
    val name: String,
    val files: Int,
    val blank: Long,
    val comment: Long,
    val code: Long,
) {
    val total: Long
        get() = blank + comment + code
}

/** 使用仓库自定义中文模板渲染代码统计报告。 */
abstract class RenderCodeStatsReportTask : DefaultTask() {
    @get:InputFile
    abstract val clocCsvFile: RegularFileProperty

    @get:Input
    abstract val excludedDirectories: ListProperty<String>

    @get:OutputFile
    abstract val markdownReport: RegularFileProperty

    @get:OutputFile
    abstract val htmlReport: RegularFileProperty

    @TaskAction
    fun render() {
        val entries = readClocCsv(clocCsvFile.get().asFile)
        val fileEntries = entries.filter { it.fileName.isNotBlank() && it.language != "SUM" }
        val total = entries.firstOrNull { it.language == "SUM" }
            ?: CodeStatsEntry(
                language = "SUM",
                fileName = "",
                blank = fileEntries.sumOf { it.blank },
                comment = fileEntries.sumOf { it.comment },
                code = fileEntries.sumOf { it.code },
            )

        val byLanguage = fileEntries.groupStatsBy { it.language }
        val byModule = fileEntries.groupStatsBy { entry ->
            entry.fileName
                .replace('\\', '/')
                .substringBefore('/', missingDelimiterValue = "根目录")
                .ifBlank { "根目录" }
        }

        val markdown = renderMarkdown(total, fileEntries.size, byLanguage, byModule)
        val html = renderHtml(total, fileEntries.size, byLanguage, byModule)

        markdownReport.get().asFile.writeText(markdown, Charsets.UTF_8)
        htmlReport.get().asFile.writeText(html, Charsets.UTF_8)
    }

    private fun readClocCsv(file: File): List<CodeStatsEntry> = file.readLines(Charsets.UTF_8)
        .drop(1)
        .mapNotNull { line ->
            val fields = parseCsvLine(line)
            if (fields.size < 5) return@mapNotNull null
            CodeStatsEntry(
                language = fields[0],
                fileName = fields[1],
                blank = fields[2].toLongOrNull() ?: 0L,
                comment = fields[3].toLongOrNull() ?: 0L,
                code = fields[4].toLongOrNull() ?: 0L,
            )
        }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        result += current.toString()
        return result
    }

    private fun List<CodeStatsEntry>.groupStatsBy(selector: (CodeStatsEntry) -> String): List<CodeStatsGroup> =
        groupBy(selector)
            .map { (name, entries) ->
                CodeStatsGroup(
                    name = name,
                    files = entries.size,
                    blank = entries.sumOf { it.blank },
                    comment = entries.sumOf { it.comment },
                    code = entries.sumOf { it.code },
                )
            }
            .sortedWith(compareByDescending<CodeStatsGroup> { it.code }.thenBy { it.name })

    private fun renderMarkdown(
        total: CodeStatsEntry,
        fileCount: Int,
        byLanguage: List<CodeStatsGroup>,
        byModule: List<CodeStatsGroup>,
    ): String {
        val generatedAt = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
        return buildString {
            appendLine("# Cangjie 仓库代码统计报告")
            appendLine()
            appendLine("- 生成时间：$generatedAt")
            appendLine("- 数据源：`cloc --by-file --csv`")
            appendLine("- 统计范围：Git 跟踪文件")
            appendLine("- 排除目录：${excludedDirectories.get().joinToString("、") { "`$it`" }}")
            appendLine()
            appendLine("## 总览")
            appendLine()
            appendLine("| 指标 | 数值 |")
            appendLine("| --- | ---: |")
            appendLine("| 文件数 | ${fileCount.formatNumber()} |")
            appendLine("| 代码行 | ${total.code.formatNumber()} |")
            appendLine("| 注释行 | ${total.comment.formatNumber()} |")
            appendLine("| 空行 | ${total.blank.formatNumber()} |")
            appendLine("| 总行数 | ${total.total.formatNumber()} |")
            appendLine()
            appendLine("## 按语言统计")
            appendLine()
            appendMarkdownTable(byLanguage)
            appendLine()
            appendLine("## 按顶层目录统计")
            appendLine()
            appendMarkdownTable(byModule.take(30))
        }
    }

    private fun StringBuilder.appendMarkdownTable(groups: List<CodeStatsGroup>) {
        appendLine("| 名称 | 文件数 | 代码行 | 注释行 | 空行 | 总行数 |")
        appendLine("| --- | ---: | ---: | ---: | ---: | ---: |")
        groups.forEach { group ->
            appendLine(
                "| ${group.name} | ${group.files.formatNumber()} | ${group.code.formatNumber()} | " +
                        "${group.comment.formatNumber()} | ${group.blank.formatNumber()} | ${group.total.formatNumber()} |"
            )
        }
    }

    private fun renderHtml(
        total: CodeStatsEntry,
        fileCount: Int,
        byLanguage: List<CodeStatsGroup>,
        byModule: List<CodeStatsGroup>,
    ): String {
        val generatedAt = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Cangjie 仓库代码统计报告</title>
              <style>
                body { margin: 0; font: 14px/1.55 "Segoe UI", "Microsoft YaHei", sans-serif; color: #202124; background: #f6f7f9; }
                main { max-width: 1180px; margin: 0 auto; padding: 32px 24px 48px; }
                h1 { margin: 0 0 8px; font-size: 28px; }
                h2 { margin: 32px 0 12px; font-size: 20px; }
                .meta { color: #5f6368; margin-bottom: 24px; }
                .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
                .metric { background: #fff; border: 1px solid #dfe3e8; border-radius: 8px; padding: 16px; }
                .metric span { display: block; color: #5f6368; }
                .metric strong { display: block; margin-top: 6px; font-size: 24px; }
                table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #dfe3e8; }
                th, td { padding: 9px 10px; border-bottom: 1px solid #edf0f2; text-align: right; white-space: nowrap; }
                th:first-child, td:first-child { text-align: left; }
                th { background: #f0f3f6; font-weight: 600; }
                tr:last-child td { border-bottom: 0; }
                .note { color: #5f6368; }
              </style>
            </head>
            <body>
              <main>
                <h1>Cangjie 仓库代码统计报告</h1>
                <div class="meta">生成时间：${generatedAt.escapeHtml()}；数据源：cloc by-file CSV；统计范围：Git 跟踪文件；排除目录：${excludedDirectories.get().joinToString("、").escapeHtml()}</div>
                <section class="summary">
                  ${metric("文件数", fileCount.formatNumber())}
                  ${metric("代码行", total.code.formatNumber())}
                  ${metric("注释行", total.comment.formatNumber())}
                  ${metric("空行", total.blank.formatNumber())}
                  ${metric("总行数", total.total.formatNumber())}
                </section>
                <h2>按语言统计</h2>
                ${htmlTable(byLanguage)}
                <h2>按顶层目录统计</h2>
                ${htmlTable(byModule.take(30))}
                <p class="note">原始工具报告仍保留在同目录，便于审计和二次处理。</p>
              </main>
            </body>
            </html>
        """.trimIndent()
    }

    private fun metric(name: String, value: String): String =
        """<div class="metric"><span>${name.escapeHtml()}</span><strong>${value.escapeHtml()}</strong></div>"""

    private fun htmlTable(groups: List<CodeStatsGroup>): String = buildString {
        appendLine("<table>")
        appendLine("<thead><tr><th>名称</th><th>文件数</th><th>代码行</th><th>注释行</th><th>空行</th><th>总行数</th></tr></thead>")
        appendLine("<tbody>")
        groups.forEach { group ->
            appendLine(
                "<tr><td>${group.name.escapeHtml()}</td><td>${group.files.formatNumber()}</td>" +
                        "<td>${group.code.formatNumber()}</td><td>${group.comment.formatNumber()}</td>" +
                        "<td>${group.blank.formatNumber()}</td><td>${group.total.formatNumber()}</td></tr>"
            )
        }
        appendLine("</tbody></table>")
    }

    private fun Int.formatNumber(): String = toLong().formatNumber()

    private fun Long.formatNumber(): String = String.format(Locale.US, "%,d", this)

    private fun String.escapeHtml(): String = buildString {
        for (char in this@escapeHtml) {
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&#39;"
                    else -> char
                }
            )
        }
    }
}

abstract class GenerateCodeStatsFileListTask : DefaultTask() {
    @get:Input
    abstract val excludedDirectories: ListProperty<String>

    @get:Internal
    abstract val executionDirectory: DirectoryProperty

    @get:OutputFile
    abstract val listFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        val outputFile = listFile.get().asFile
        outputFile.parentFile.mkdirs()

        val stdout = ByteArrayOutputStream()
        execOperations.exec {
            executable = "git"
            args("ls-files")
            workingDir(executionDirectory.get().asFile)
            standardOutput = stdout
        }

        val excludedPrefixes = excludedDirectories.get()
            .map { it.trim('/', '\\') }
            .filter { it.isNotEmpty() }
            .map { "$it/" }
            .toSet()

        val files = stdout.toString(Charsets.UTF_8)
            .lineSequence()
            .filter { path -> excludedPrefixes.none { excluded -> path == excluded.dropLast(1) || path.startsWith(excluded) } }
            .filter { path -> executionDirectory.get().asFile.resolve(path.replace('/', File.separatorChar)).isFile }
            .toList()

        Files.write(outputFile.toPath(), files, Charsets.UTF_8)
    }
}

abstract class DownloadCodeStatsToolTask : DefaultTask() {
    @get:Input
    abstract val downloadUrl: Property<String>

    @get:OutputFile
    abstract val downloadedFile: RegularFileProperty

    @TaskAction
    fun download() {
        val outputFile = downloadedFile.get().asFile
        val temporaryFile = outputFile.resolveSibling("${outputFile.name}.tmp")
        outputFile.parentFile.mkdirs()

        logger.lifecycle("Downloading ${downloadUrl.get()} to ${outputFile.absolutePath}")
        val connection = URI(downloadUrl.get()).toURL().openConnection()
        connection.setRequestProperty("User-Agent", "cangjie-gradle-code-stats")
        connection.getInputStream().use { input ->
            Files.copy(input, temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        Files.move(temporaryFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        outputFile.setExecutable(true)
    }
}

abstract class ExtractCodeStatsZipToolTask : DefaultTask() {
    @get:InputFile
    abstract val archiveFile: RegularFileProperty

    @get:Input
    abstract val executableName: Property<String>

    @get:OutputFile
    abstract val toolFile: RegularFileProperty

    @TaskAction
    fun extract() {
        val archive = archiveFile.get().asFile
        val outputFile = toolFile.get().asFile
        val temporaryFile = outputFile.resolveSibling("${outputFile.name}.tmp")
        outputFile.parentFile.mkdirs()

        ZipInputStream(Files.newInputStream(archive.toPath())).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { entry ->
                    !entry.isDirectory && entry.name.replace('\\', '/').substringAfterLast('/') == executableName.get()
                }
                ?: error("Archive ${archive.absolutePath} does not contain ${executableName.get()}")

            Files.copy(zip, temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        Files.move(temporaryFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        outputFile.setExecutable(true)
    }
}

val codeStatsReportDirectory = layout.buildDirectory.dir("reports/code-stats")
val codeStatsToolDirectory = layout.buildDirectory.dir("code-stats-tools")
val codeStatsDownloadDirectory = codeStatsToolDirectory.map { it.dir("downloads") }
val codeStatsExcludedDirectories = listOf("external", "build", ".gradle", "out", ".idea", ".git")
val codeStatsExcludedDirectoryArgument = codeStatsExcludedDirectories.joinToString(",")

val codeStatsClocVersion = providers.gradleProperty("codeStats.cloc.version").orElse("2.08")
val codeStatsTokeiVersion = providers.gradleProperty("codeStats.tokei.version").orElse("13.0.0-alpha.0")
val codeStatsSccVersion = providers.gradleProperty("codeStats.scc.version").orElse("3.7.0")

val generateCodeStatsFileList by tasks.registering(GenerateCodeStatsFileListTask::class) {
    group = "reporting"
    description = "生成代码统计使用的 Git 跟踪文件清单。"
    excludedDirectories.set(codeStatsExcludedDirectories)
    executionDirectory.set(layout.projectDirectory)
    listFile.set(codeStatsReportDirectory.map { it.file("git-files.txt") })
}

val downloadCodeStatsCloc by tasks.registering(DownloadCodeStatsToolTask::class) {
    group = "reporting"
    description = "下载 cloc 代码统计工具。"
    downloadUrl.set(codeStatsClocVersion.map { version ->
        "https://github.com/AlDanial/cloc/releases/download/v$version/cloc-$version.exe"
    })
    downloadedFile.set(codeStatsToolDirectory.map { it.file("cloc/cloc.exe") })
}

val downloadCodeStatsTokei by tasks.registering(DownloadCodeStatsToolTask::class) {
    group = "reporting"
    description = "下载 tokei 代码统计工具。"
    downloadUrl.set(codeStatsTokeiVersion.map { version ->
        "https://github.com/XAMPPRocky/tokei/releases/download/v$version/tokei-x86_64-pc-windows-msvc.exe"
    })
    downloadedFile.set(codeStatsToolDirectory.map { it.file("tokei/tokei.exe") })
}

val downloadCodeStatsSccArchive by tasks.registering(DownloadCodeStatsToolTask::class) {
    group = "reporting"
    description = "下载 scc 代码统计工具压缩包。"
    downloadUrl.set(codeStatsSccVersion.map { version ->
        "https://github.com/boyter/scc/releases/download/v$version/scc_Windows_x86_64.zip"
    })
    downloadedFile.set(codeStatsDownloadDirectory.map { it.file("scc_Windows_x86_64.zip") })
}

val extractCodeStatsScc by tasks.registering(ExtractCodeStatsZipToolTask::class) {
    group = "reporting"
    description = "解压 scc 代码统计工具。"
    dependsOn(downloadCodeStatsSccArchive)
    archiveFile.set(downloadCodeStatsSccArchive.flatMap { it.downloadedFile })
    executableName.set("scc.exe")
    toolFile.set(codeStatsToolDirectory.map { it.file("scc/scc.exe") })
}

val codeStatsClocMarkdownReport = codeStatsReportDirectory.map { it.file("cloc.md") }
val codeStatsClocMarkdown by tasks.registering(CodeStatsToolTask::class) {
    group = "reporting"
    description = "使用 cloc 生成仓库代码行数 Markdown 统计报告。"
    dependsOn(downloadCodeStatsCloc, generateCodeStatsFileList)
    toolExecutable.set(downloadCodeStatsCloc.flatMap { it.downloadedFile }.map { it.asFile.absolutePath })
    toolArguments.set(generateCodeStatsFileList.flatMap { it.listFile }.zip(codeStatsClocMarkdownReport) { listFile, report ->
        listOf(
            "--list-file=${listFile.asFile.absolutePath}",
            "--skip-uniqueness",
            "--md",
            "--out=${report.asFile.absolutePath}",
        )
    })
    captureStandardOutput.set(false)
    executionDirectory.set(layout.projectDirectory)
    reportFile.set(codeStatsClocMarkdownReport)
    errorLogFile.set(codeStatsReportDirectory.map { it.file("cloc.md.stderr.log") })
    outputs.upToDateWhen { false }
}

val codeStatsClocCsvReport = codeStatsReportDirectory.map { it.file("cloc-by-file.csv") }
val codeStatsClocCsv by tasks.registering(CodeStatsToolTask::class) {
    group = "reporting"
    description = "使用 cloc 生成仓库逐文件 CSV 代码统计报告。"
    dependsOn(downloadCodeStatsCloc, generateCodeStatsFileList)
    mustRunAfter(codeStatsClocMarkdown)
    toolExecutable.set(downloadCodeStatsCloc.flatMap { it.downloadedFile }.map { it.asFile.absolutePath })
    toolArguments.set(generateCodeStatsFileList.flatMap { it.listFile }.zip(codeStatsClocCsvReport) { listFile, report ->
        listOf(
            "--list-file=${listFile.asFile.absolutePath}",
            "--skip-uniqueness",
            "--by-file",
            "--csv",
            "--out=${report.asFile.absolutePath}",
        )
    })
    captureStandardOutput.set(false)
    executionDirectory.set(layout.projectDirectory)
    reportFile.set(codeStatsClocCsvReport)
    errorLogFile.set(codeStatsReportDirectory.map { it.file("cloc-by-file.csv.stderr.log") })
    outputs.upToDateWhen { false }
}

val codeStatsTokeiJson by tasks.registering(CodeStatsToolTask::class) {
    group = "reporting"
    description = "使用 tokei 生成仓库 JSON 代码统计报告。"
    dependsOn(downloadCodeStatsTokei)
    toolExecutable.set(downloadCodeStatsTokei.flatMap { it.downloadedFile }.map { it.asFile.absolutePath })
    toolArguments.set(
        listOf(".", "--exclude") +
                codeStatsExcludedDirectories +
                listOf("--output", "json")
    )
    captureStandardOutput.set(true)
    executionDirectory.set(layout.projectDirectory)
    reportFile.set(codeStatsReportDirectory.map { it.file("tokei.json") })
    errorLogFile.set(codeStatsReportDirectory.map { it.file("tokei.json.stderr.log") })
    outputs.upToDateWhen { false }
}

val codeStatsSccHtml by tasks.registering(CodeStatsToolTask::class) {
    group = "reporting"
    description = "使用 scc 生成仓库 HTML 代码统计报告。"
    dependsOn(extractCodeStatsScc)
    toolExecutable.set(extractCodeStatsScc.flatMap { it.toolFile }.map { it.asFile.absolutePath })
    toolArguments.set(
        listOf(
            ".",
            "--exclude-dir",
            codeStatsExcludedDirectoryArgument,
            "--format",
            "html",
        )
    )
    captureStandardOutput.set(true)
    executionDirectory.set(layout.projectDirectory)
    reportFile.set(codeStatsReportDirectory.map { it.file("scc.html") })
    errorLogFile.set(codeStatsReportDirectory.map { it.file("scc.html.stderr.log") })
    outputs.upToDateWhen { false }
}

val renderCodeStatsReport by tasks.registering(RenderCodeStatsReportTask::class) {
    group = "reporting"
    description = "使用仓库自定义中文模板生成代码统计 Markdown 和 HTML 报告。"
    dependsOn(codeStatsClocCsv)
    clocCsvFile.set(codeStatsClocCsv.flatMap { it.reportFile })
    excludedDirectories.set(codeStatsExcludedDirectories)
    markdownReport.set(codeStatsReportDirectory.map { it.file("code-stats.md") })
    htmlReport.set(codeStatsReportDirectory.map { it.file("code-stats.html") })
    outputs.upToDateWhen { false }
}

tasks.register("codeStats") {
    group = "reporting"
    description = "生成自定义中文报告和 cloc/tokei/scc 原始代码统计报告。"
    dependsOn(renderCodeStatsReport, codeStatsClocMarkdown, codeStatsTokeiJson, codeStatsSccHtml)
}

tasks.register("codeStatsMarkdown") {
    group = "reporting"
    description = "生成自定义中文 Markdown 代码统计报告。"
    dependsOn(renderCodeStatsReport)
}

tasks.register("codeStatsHtml") {
    group = "reporting"
    description = "生成自定义中文 HTML 代码统计报告。"
    dependsOn(renderCodeStatsReport)
}

fun md(text: String): String = text.trimIndent()

val publicPublicationArtifacts = linkedMapOf(
    ":prepare:frontend" to ("cangjie-frontend" to md("""
        **Recommended for:** IntelliJ Platform / IDEA plugins and other **controlled-classpath** integrations.

        - Publishes the public Cangjie frontend runtime facade.
        - Keeps host dependency packages unchanged; **no relocation** is applied.
        - Suitable when the host process already provides compatible `com.intellij.*` and related runtime libraries.
        - Choose `cangjie-frontend-embeddable` instead when the host process is uncontrolled or may carry conflicting compiler / IDE dependencies.
    """)),
    ":prepare:frontend-embeddable" to ("cangjie-frontend-embeddable" to md("""
        **Recommended for:** embedding the Cangjie frontend into **host-uncontrolled** JVM processes.

        - Publishes the embeddable frontend runtime facade.
        - Applies **shading + relocation** to host-sensitive dependencies under `org.cangnova.cangjie.*`.
        - Designed to reduce classpath conflicts with IntelliJ, Guava, JDOM, FastUtil, and similar libraries already present in the host.
        - Not the preferred choice for ordinary IDEA plugins that need direct interop with the platform's original `com.intellij.*` packages.
    """)),
    ":prepare:test-infrastructure" to ("cangjie-frontend-test-infrastructure" to md("""
        **Recommended for:** compiler, parser, PSI, and integration tests built outside this repository.

        - Publishes the reusable Cangjie frontend test infrastructure facade.
        - Packages the shared `testFixtures` runtime needed to stand up frontend-oriented test environments.
        - Intended for consumers who want the repository's canonical test scaffolding without depending on internal module layout.
        - Works best together with the public frontend artifacts rather than direct internal project dependencies.
    """)),
    ":prepare:analysis-test-framework" to ("cangjie-frontend-analysis-test-framework" to md("""
        **Recommended for:** external tests targeting the public Analysis API contract.

        - Publishes the reusable Analysis API test framework facade.
        - Aggregates the shared `analysis-test-framework` and frontend test fixtures required by Analysis API test suites.
        - Useful for downstream projects that need the repository's standard analysis assertions, session setup, and fixture conventions.
        - Intended for test code and verification workflows, not for production runtime embedding.
    """)),
)

val idePublicationArtifacts = linkedMapOf(
    ":prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide" to ("cangjie-frontend-common-for-ide" to md("""
        **IDE plugin dependency:** packages :common, :util, :compiler:arguments, :resolution.common into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide" to ("cangjie-frontend-psi-for-ide" to md("""
        **IDE plugin dependency:** packages :psi (with :common, :util) into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-for-ide" to ("cangjie-frontend-code-insight-for-ide" to md("""
        **IDE plugin dependency:** packages :code-insight:api quick-fix API (with :analysis:analysis-api) into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-formatting-for-ide" to ("cangjie-frontend-code-insight-formatting-for-ide" to md("""
        **IDE plugin dependency:** packages :code-insight:formatting (with :psi, :common, :util) into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-folding-for-ide" to ("cangjie-frontend-code-insight-folding-for-ide" to md("""
        **IDE plugin dependency:** packages :code-insight:folding (with :psi, :common, :util) into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-cfir-for-ide" to ("cangjie-frontend-cfir-for-ide" to md("""
        **IDE plugin dependency:** packages :cfir:* full series and :common:diagnostics into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-for-ide" to ("cangjie-frontend-analysis-api-for-ide" to md("""
        **IDE plugin dependency:** packages :analysis:analysis-api, :analysis:analysis-api-platform-interface, :analysis:analysis-api-impl-base into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide" to ("cangjie-frontend-analysis-api-cfir-for-ide" to md("""
        **IDE plugin dependency:** packages :analysis:analysis-api-cfir, :analysis:low-level-api-cfir, :analysis:decompiled, :analysis:symbol-light-declarations into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide" to ("cangjie-frontend-analysis-api-standalone-for-ide" to md("""
        **IDE plugin dependency:** packages :analysis:analysis-api-standalone, :analysis:analysis-internal-utils into a single fat jar.
    """)),
)

extensions.extraProperties["cangjiePublicProjectPaths"] = publicPublicationArtifacts.keys.toSet()

publicPublicationArtifacts.forEach { (projectPath, publication) ->
    val (artifactId, publicationDescription) = publication
    project(projectPath).run {
        extensions.extraProperties["cangjiePublicationArtifactId"] = artifactId
        extensions.extraProperties["cangjiePublicationDescription"] = publicationDescription
        pluginManager.apply("cangjie-publishing")
    }
}

idePublicationArtifacts.forEach { (projectPath, publication) ->
    val (artifactId, publicationDescription) = publication
    project(projectPath).run {
        extensions.extraProperties["cangjiePublicationArtifactId"] = artifactId
        extensions.extraProperties["cangjiePublicationDescription"] = publicationDescription
        pluginManager.apply("cangjie-publishing")
    }
}

val allPublicationArtifacts = publicPublicationArtifacts + idePublicationArtifacts

tasks.register("printPublicArtifactIds") {
    group = "publishing"
    description = "Print all public Maven artifactIds, one per line."
    doLast {
        allPublicationArtifacts.values
            .map { (artifactId, _) -> artifactId }
            .forEach(::println)
    }
}

tasks.register("publishPublicArtifacts") {
    group = "publishing"
    description = "Publish all public frontend and analysis Maven artifacts."
    dependsOn(allPublicationArtifacts.keys.map { "$it:publish" })
}

tasks.register("publish") {
    group = "publishing"
    description = "Publish all public frontend and analysis Maven artifacts."
    dependsOn("publishPublicArtifacts")
}

tasks.register("installPublicArtifacts") {
    group = "publishing"
    description = "Install all public frontend and analysis Maven artifacts to Maven Local."
    dependsOn(allPublicationArtifacts.keys.map { "$it:install" })
}

tasks.register("install") {
    group = "publishing"
    description = "Install all public frontend and analysis Maven artifacts to Maven Local."
    dependsOn("installPublicArtifacts")
}
