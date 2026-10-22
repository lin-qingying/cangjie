package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path

/** 单条 CFIR 诊断的序列化形态。 */
data class DiagnosticRecord(
    val name: String,
    val severity: String,
    val message: String,
    val startOffset: Int,
    val endOffset: Int,
    val line: Int,
    val column: Int,
    val psiText: String,
    val snippet: String,
)

/** 单个文件的 CFIR 分析结果。 */
data class FileAnalysis(
    val path: String,
    val diagnostics: List<DiagnosticRecord>,
    val error: String?,
    val durationMillis: Long,
    /**
     * 诊断收集期间被 element 级隔离的异常。
     *
     * 这些异常不会逃逸出 analysis 调用，但同样表示诊断不完整；真实项目测试必须断言为空。
     */
    val collectionFailures: List<String> = emptyList(),
)

/**
 * 把真实项目的 cjpm 编译结果与 CFIR 分析结果写成 JSON，便于逐项对照。
 */
object RealProjectReport {

    /** 写出报告；目录不存在时自动创建。 */
    fun write(
        config: RealProjectConfig,
        checkout: Path,
        model: CjpmProjectModel,
        buildResults: List<CjpmRunner.Result>,
        dependencyCjoCount: Int,
        analyses: List<FileAnalysis>,
        reportFile: Path,
    ) {
        Files.createDirectories(reportFile.parent)
        val errorCount = analyses.sumOf { analysis ->
            analysis.diagnostics.count { it.severity == "ERROR" }
        }
        val json = buildString {
            appendLine("{")
            appendLine("  \"project\": ${quote(config.id)},")
            appendLine("  \"repo\": ${quote(config.repo)},")
            appendLine("  \"ref\": ${quote(config.ref)},")
            appendLine("  \"checkout\": ${quote(checkout.toString())},")
            appendLine("  \"package\": ${quote(model.name)},")
            appendLine("  \"sourceFileCount\": ${model.cjFiles.size},")
            appendLine("  \"projectPackageCount\": ${model.sourcePackages.size},")
            appendLine("  \"dependencyCjoCount\": $dependencyCjoCount,")
            appendLine("  \"errorDiagnosticCount\": $errorCount,")
            appendLine("  \"exceptionFileCount\": ${analyses.count { it.error != null }},")
            appendLine("  \"collectionFailureFileCount\": ${analyses.count { it.collectionFailures.isNotEmpty() }},")
            appendLine("  \"collectionFailureCount\": ${analyses.sumOf { it.collectionFailures.size }},")
            appendLine("  \"cjpm\": [")
            buildResults.forEachIndexed { index, result ->
                val comma = if (index == buildResults.lastIndex) "" else ","
                appendLine(
                    "    {\"command\": ${quote(result.command)}, \"exitCode\": ${result.exitCode}, " +
                        "\"durationMillis\": ${result.durationMillis}}$comma",
                )
            }
            appendLine("  ],")
            appendLine("  \"files\": [")
            analyses.forEachIndexed { index, analysis ->
                val comma = if (index == analyses.lastIndex) "" else ","
                appendLine("    {")
                appendLine("      \"path\": ${quote(analysis.path)},")
                appendLine("      \"durationMillis\": ${analysis.durationMillis},")
                appendLine("      \"error\": ${analysis.error?.let(::quote) ?: "null"},")
                appendLine("      \"collectionFailures\": [${analysis.collectionFailures.joinToString(", ") { quote(it) }}],")
                appendLine("      \"diagnostics\": [")
                analysis.diagnostics.forEachIndexed { diagIndex, diagnostic ->
                    val diagComma = if (diagIndex == analysis.diagnostics.lastIndex) "" else ","
                    appendLine(
                        "        {\"name\": ${quote(diagnostic.name)}, \"severity\": ${quote(diagnostic.severity)}, " +
                            "\"start\": ${diagnostic.startOffset}, \"end\": ${diagnostic.endOffset}, " +
                            "\"line\": ${diagnostic.line}, \"column\": ${diagnostic.column}, " +
                            "\"message\": ${quote(diagnostic.message)}, \"psiText\": ${quote(diagnostic.psiText)}, " +
                            "\"snippet\": ${quote(diagnostic.snippet)}}$diagComma",
                    )
                }
                appendLine("      ]")
                appendLine("    }$comma")
            }
            appendLine("  ]")
            appendLine("}")
        }
        Files.writeString(reportFile, json)
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
}
