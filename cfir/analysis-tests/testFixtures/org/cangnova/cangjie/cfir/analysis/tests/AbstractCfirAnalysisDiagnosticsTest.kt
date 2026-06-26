package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import java.io.File

/**
 * 诊断测试基类：支持 Kotlin/CFIR 风格内联标注
 * `<!DIAGNOSTIC_NAME!>...<!>`。
 */
abstract class AbstractCfirAnalysisDiagnosticsTest : AbstractCfirAnalysisResolveTest() {

    /**
     * 执行单个内联诊断测试。
     *
     * 方法会移除源码中的 `<!...!>` 标记，运行 CFIR 诊断收集器，并把实际诊断
     * 与标记中声明的诊断名称及区间逐项比较。
     */
    protected fun runDiagnosticsTest(testDataFilePath: String) {
        val sourceFile = resolveTestDataPath(testDataFilePath)
        val originalText = loadFile(sourceFile.path)
        val parsed = parseInlineDiagnostics(originalText)
        val cjFile = createCjFile(sourceFile.nameWithoutExtension, parsed.cleanText)

        val session = createTestSession()
        val cfirFile = cjFile.toCfirFile(session = session)
        val diagnosticCollector = CfirDiagnosticCollector()
        resolveToPhase(cfirFile, session, targetPhase, diagnosticCollector)

        val actual = diagnosticCollector.rawDiagnostics.map { diagnostic ->
            MarkedDiagnostic(
                name = normalizeDiagnosticName(diagnostic.factoryName),
                startOffset = diagnostic.firstRange.startOffset,
                endOffset = diagnostic.firstRange.endOffset,
            )
        }

        assertDiagnosticsEqual(
            sourceFile = sourceFile,
            expected = parsed.expectedDiagnostics,
            actual = actual,
            cleanText = parsed.cleanText,
        )
    }

    /**
     * 解析 Kotlin 风格的内联诊断标记。
     *
     * 返回的 clean text 可直接送入 PSI/CFIR 构建，expected diagnostics 保留标记在
     * clean text 中对应的起止偏移。
     */
    private fun parseInlineDiagnostics(source: String): ParsedInlineDiagnostics {
        val clean = StringBuilder(source.length)
        val expected = mutableListOf<MarkedDiagnostic>()
        val stack = mutableListOf<OpenDiagnosticMark>()

        var index = 0
        while (index < source.length) {
            if (source.startsWith("<!", index)) {
                val markerEnd = source.indexOf('>', index + 2)
                require(markerEnd >= 0) { "Unterminated diagnostic marker near offset $index" }
                val payload = source.substring(index + 2, markerEnd).removeSuffix("!").trim()
                if (payload.isEmpty()) {
                    require(stack.isNotEmpty()) { "Closing marker <!> without opening marker near offset $index" }
                    val opened = stack.removeAt(stack.lastIndex)
                    for (name in opened.names) {
                        expected += MarkedDiagnostic(name, opened.startOffset, clean.length)
                    }
                } else {
                    val names = splitTopLevelByComma(payload)
                        .asSequence()
                        .map { it.substringBefore("(").trim() }
                        .filter { it.isNotEmpty() }
                        .map { normalizeDiagnosticName(it) }
                        .toList()
                    require(names.isNotEmpty()) { "No diagnostic name in marker '<!$payload!>'" }
                    stack += OpenDiagnosticMark(names, clean.length)
                }
                index = markerEnd + 1
                continue
            }

            clean.append(source[index])
            index++
        }

        require(stack.isEmpty()) {
            val unclosed = stack.joinToString(", ") { it.names.joinToString("+") }
            "Unclosed diagnostic marker(s): $unclosed"
        }

        return ParsedInlineDiagnostics(clean.toString(), expected)
    }

    /**
     * 在诊断标记负载中按顶层逗号切分多个诊断名。
     *
     * 括号内的逗号属于诊断参数，不作为分隔符处理。
     */
    private fun splitTopLevelByComma(raw: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in raw.indices) {
            when (raw[i]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                ',' -> if (depth == 0) {
                    result += raw.substring(start, i)
                    start = i + 1
                }
            }
        }
        result += raw.substring(start)
        return result
    }

    /**
     * 规范化诊断名称，去除旧式 `CFIR_` 前缀并裁剪空白。
     */
    private fun normalizeDiagnosticName(name: String): String {
        return name.removePrefix("CFIR_").trim()
    }

    /**
     * 比较期望诊断和实际诊断的多重集合。
     *
     * 同一个诊断在同一范围重复出现时按次数比较，并输出缺失/多余诊断的源码片段。
     */
    private fun assertDiagnosticsEqual(
        sourceFile: File,
        expected: List<MarkedDiagnostic>,
        actual: List<MarkedDiagnostic>,
        cleanText: String,
    ) {
        val expectedCounts = expected.groupingBy { it }.eachCount().toMutableMap()
        val actualCounts = actual.groupingBy { it }.eachCount().toMutableMap()

        val allKeys = (expectedCounts.keys + actualCounts.keys).sortedWith(
            compareBy<MarkedDiagnostic> { it.startOffset }
                .thenBy { it.endOffset }
                .thenBy { it.name },
        )

        val missing = mutableListOf<String>()
        val unexpected = mutableListOf<String>()
        for (key in allKeys) {
            val expectedCount = expectedCounts[key] ?: 0
            val actualCount = actualCounts[key] ?: 0
            if (expectedCount > actualCount) {
                repeat(expectedCount - actualCount) { missing += renderDiagnostic(key, cleanText) }
            } else if (actualCount > expectedCount) {
                repeat(actualCount - expectedCount) { unexpected += renderDiagnostic(key, cleanText) }
            }
        }

        if (missing.isEmpty() && unexpected.isEmpty()) return

        val message = buildString {
            appendLine("Inline diagnostic assertion failed for: ${sourceFile.path}")
            if (missing.isNotEmpty()) {
                appendLine("Missing diagnostics:")
                missing.forEach { appendLine("  - $it") }
            }
            if (unexpected.isNotEmpty()) {
                appendLine("Unexpected diagnostics:")
                unexpected.forEach { appendLine("  - $it") }
            }
        }
        error(message.trimEnd())
    }

    /**
     * 将单个诊断渲染成稳定的错误说明。
     *
     * 渲染内容包含诊断名、偏移区间以及对应源码片段，便于定位标记错位。
     */
    private fun renderDiagnostic(diagnostic: MarkedDiagnostic, cleanText: String): String {
        val start = diagnostic.startOffset.coerceIn(0, cleanText.length)
        val end = diagnostic.endOffset.coerceIn(start, cleanText.length)
        val snippet = cleanText.substring(start, end).replace("\n", "\\n")
        return "${diagnostic.name} @ [$start, $end): \"$snippet\""
    }

    /**
     * 尚未闭合的内联诊断标记。
     *
     * @property names 该标记声明的诊断名称集合。
     * @property startOffset 标记覆盖范围在 clean text 中的起始偏移。
     */
    private data class OpenDiagnosticMark(val names: List<String>, val startOffset: Int)

    /**
     * 内联诊断源码解析结果。
     *
     * @property cleanText 移除所有诊断标记后的源码文本。
     * @property expectedDiagnostics 从标记中解析出的期望诊断集合。
     */
    private data class ParsedInlineDiagnostics(
        /** 移除所有诊断标记后的源码文本。 */
        val cleanText: String,
        /** 从内联标记中解析出的期望诊断集合。 */
        val expectedDiagnostics: List<MarkedDiagnostic>,
    )

    /**
     * 标记化诊断的最小比较单元。
     *
     * @property name 规范化后的诊断名称。
     * @property startOffset 诊断范围起始偏移。
     * @property endOffset 诊断范围结束偏移。
     */
    private data class MarkedDiagnostic(
        /** 规范化后的诊断名称。 */
        val name: String,
        /** 诊断范围起始偏移。 */
        val startOffset: Int,
        /** 诊断范围结束偏移。 */
        val endOffset: Int,
    )
}
