package org.cangnova.gradle.tests

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * 为 Gradle 测试报告中的期望/实际内容差异添加可读的逐行颜色对比。
 *
 * Gradle 自身负责生成报告；本任务只增强失败详情页面，并保留原始异常视图。
 */
abstract class EnhanceTestFailureReportsTask : DefaultTask() {
    @get:Internal
    abstract val reportDirectory: DirectoryProperty

    @TaskAction
    fun enhanceReports() {
        val reportRoot = reportDirectory.orNull?.asFile?.takeIf(File::isDirectory) ?: return
        val targetPages = reportRoot.walkTopDown()
            .filter { it.isFile && it.extension.equals("html", ignoreCase = true) }
            .map { it to it.readText(Charsets.UTF_8) }
            .filter { (_, html) -> html.contains(EXPECTED_MARKER) && html.contains(ACTUAL_MARKER) }
            .toList()
        if (targetPages.isEmpty()) return

        val css = readAsset("failure-diff.css")
        val javascript = readAsset("failure-diff.js")
        writeIfChanged(reportRoot.resolve(CSS_FILE), css)
        writeIfChanged(reportRoot.resolve(JAVASCRIPT_FILE), javascript)

        var enhancedPages = 0
        targetPages.forEach { (page, html) ->
            val relativeAssetPrefix = page.parentFile.toPath()
                .relativize(reportRoot.toPath())
                .toString()
                .replace(File.separatorChar, '/')
                .let { path -> if (path.isEmpty()) "" else "$path/" }
            val cleanHtml = html
                .replace(CSS_REFERENCE, "")
                .replace(JAVASCRIPT_REFERENCE, "")
            val headEnd = cleanHtml.lastIndexOf("</head>", ignoreCase = true)
            check(headEnd >= 0) { "Gradle test report has no closing head tag: $page" }
            val styledHtml = cleanHtml.substring(0, headEnd) +
                "<link rel=\"stylesheet\" href=\"${relativeAssetPrefix}$CSS_FILE\">" +
                cleanHtml.substring(headEnd)

            val bodyEnd = styledHtml.lastIndexOf("</body>", ignoreCase = true)
            check(bodyEnd >= 0) { "Gradle test report has no closing body tag: $page" }
            val enhancedHtml = styledHtml.substring(0, bodyEnd) +
                "<script defer src=\"${relativeAssetPrefix}$JAVASCRIPT_FILE\"></script>" +
                styledHtml.substring(bodyEnd)
            if (enhancedHtml != html) {
                page.writeText(enhancedHtml, Charsets.UTF_8)
                enhancedPages++
            }
        }

        if (enhancedPages > 0) {
            logger.lifecycle("Added colored diffs to $enhancedPages Gradle test failure report(s)")
        }
    }

    private fun readAsset(name: String): String =
        EnhanceTestFailureReportsTask::class.java.getResourceAsStream(name)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: error("Missing test report asset: $name")

    private fun writeIfChanged(file: File, contents: String) {
        if (!file.isFile || file.readText(Charsets.UTF_8) != contents) {
            file.writeText(contents, Charsets.UTF_8)
        }
    }

    private companion object {
        const val EXPECTED_MARKER = "=====预期======"
        const val ACTUAL_MARKER = "=====得到======"
        const val CSS_FILE = "cangjie-failure-diff.css"
        const val JAVASCRIPT_FILE = "cangjie-failure-diff.js"
        val CSS_REFERENCE = Regex("""<link rel="stylesheet" href="[^"]*cangjie-failure-diff\.css">""")
        val JAVASCRIPT_REFERENCE = Regex("""<script defer src="[^"]*cangjie-failure-diff\.js"></script>""")
    }
}
