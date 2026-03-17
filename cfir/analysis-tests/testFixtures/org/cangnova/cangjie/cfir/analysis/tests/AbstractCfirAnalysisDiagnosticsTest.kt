package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import java.io.File

/**
 * 诊断测试基类：支持 Kotlin/CFIR 风格内联标注
 * `<!DIAGNOSTIC_NAME!>...<!>`。
 */
abstract class AbstractCfirAnalysisDiagnosticsTest : AbstractCfirAnalysisResolveTest() {

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

    private fun normalizeDiagnosticName(name: String): String {
        return name.removePrefix("CFIR_").trim()
    }

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

    private fun renderDiagnostic(diagnostic: MarkedDiagnostic, cleanText: String): String {
        val start = diagnostic.startOffset.coerceIn(0, cleanText.length)
        val end = diagnostic.endOffset.coerceIn(start, cleanText.length)
        val snippet = cleanText.substring(start, end).replace("\n", "\\n")
        return "${diagnostic.name} @ [$start, $end): \"$snippet\""
    }

    private data class OpenDiagnosticMark(val names: List<String>, val startOffset: Int)

    private data class ParsedInlineDiagnostics(
        val cleanText: String,
        val expectedDiagnostics: List<MarkedDiagnostic>,
    )

    private data class MarkedDiagnostic(
        val name: String,
        val startOffset: Int,
        val endOffset: Int,
    )
}
