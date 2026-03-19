package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.test.codeMetaInfo.CodeMetaInfoRenderer
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.test.codeMetaInfo.model.ParsedCodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.clearTextFromDiagnosticMarkup

object CfirInlineDiagnosticsDiff {
    fun compare(
        filePath: String,
        originalText: String,
        actualDiagnostics: List<CjDiagnostic>,
    ): StructuredDiagnosticMismatch? {
        val parsed = parseInlineDiagnostics(originalText)
        val actual = actualDiagnostics.map { diagnostic ->
            DiagnosticMark(
                name = normalizeDiagnosticName(diagnostic.factoryName),
                startOffset = diagnostic.firstRange.startOffset,
                endOffset = diagnostic.firstRange.endOffset,
            )
        }

        val expectedCounts = parsed.expectedDiagnostics.groupingBy { it }.eachCount()
        val actualCounts = actual.groupingBy { it }.eachCount()
        val allKeys = (expectedCounts.keys + actualCounts.keys).sortedWith(
            compareBy<DiagnosticMark> { it.startOffset }
                .thenBy { it.endOffset }
                .thenBy { it.name },
        )

        val missing = mutableListOf<DiagnosticEntry>()
        val unexpected = mutableListOf<DiagnosticEntry>()
        for (key in allKeys) {
            val expectedCount = expectedCounts[key] ?: 0
            val actualCount = actualCounts[key] ?: 0
            if (expectedCount > actualCount) {
                repeat(expectedCount - actualCount) { missing += renderEntry(key, parsed.cleanText) }
            } else if (actualCount > expectedCount) {
                repeat(actualCount - expectedCount) { unexpected += renderEntry(key, parsed.cleanText) }
            }
        }

        if (missing.isEmpty() && unexpected.isEmpty()) return null
        return StructuredDiagnosticMismatch(
            filePath = filePath,
            missing = missing,
            unexpected = unexpected,
            expectedContent = originalText,
            actualContent = renderActualDiagnostics(parsed.cleanText, actual),
        )
    }

    private fun parseInlineDiagnostics(source: String): ParsedInlineDiagnostics {
        val clean = StringBuilder(source.length)
        val expected = mutableListOf<DiagnosticMark>()
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
                        expected += DiagnosticMark(name, opened.startOffset, clean.length)
                    }
                } else {
                    val names = splitTopLevelByComma(payload)
                        .asSequence()
                        .map { it.substringBefore("(").trim() }
                        .filter { it.isNotEmpty() }
                        .map(::normalizeDiagnosticName)
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

    fun normalizeDiagnosticName(name: String): String {
        return name.removePrefix("CFIR_").trim()
    }

    private fun renderEntry(mark: DiagnosticMark, cleanText: String): DiagnosticEntry {
        val start = mark.startOffset.coerceIn(0, cleanText.length)
        val end = mark.endOffset.coerceIn(start, cleanText.length)
        val startPos = lineAndColumn(cleanText, start)
        val endPos = lineAndColumn(cleanText, end)
        val snippet = cleanText.substring(start, end).ifEmpty {
            clearTextFromDiagnosticMarkup(cleanText).let { normalized ->
                normalized.substring(start.coerceAtMost(normalized.length), end.coerceAtMost(normalized.length))
            }
        }.replace("\n", "\\n")

        return DiagnosticEntry(
            name = mark.name,
            startOffset = mark.startOffset,
            endOffset = mark.endOffset,
            startLine = startPos.first,
            startColumn = startPos.second,
            endLine = endPos.first,
            endColumn = endPos.second,
            snippet = if (snippet.isEmpty()) "<empty>" else snippet,
        )
    }

    private fun lineAndColumn(text: String, offset: Int): Pair<Int, Int> {
        val safeOffset = offset.coerceIn(0, text.length)
        var line = 1
        var column = 1
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
        return line to column
    }

    private data class OpenDiagnosticMark(val names: List<String>, val startOffset: Int)

    private data class ParsedInlineDiagnostics(
        val cleanText: String,
        val expectedDiagnostics: List<DiagnosticMark>,
    )

    data class DiagnosticMark(
        val name: String,
        val startOffset: Int,
        val endOffset: Int,
    )

    data class DiagnosticEntry(
        val name: String,
        val startOffset: Int,
        val endOffset: Int,
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int,
        val snippet: String,
    ) {
        fun render(): String {
            return "$name @ $startLine:$startColumn..$endLine:$endColumn " +
                "(offsets [$startOffset, $endOffset)) on \"$snippet\""
        }

        fun renderAt(filePath: String): String {
            return "$name ($filePath:$startLine:$startColumn) " +
                "(to $endLine:$endColumn, offsets [$startOffset, $endOffset), snippet \"$snippet\")"
        }

        fun renderSummary(kind: String): String {
            return "$kind.$name at line $startLine, col $startColumn..$endColumn " +
                "[offsets [$startOffset, $endOffset), snippet \"$snippet\"]"
        }
    }

    data class StructuredDiagnosticMismatch(
        val filePath: String,
        val missing: List<DiagnosticEntry>,
        val unexpected: List<DiagnosticEntry>,
        val expectedContent: String,
        val actualContent: String,
    )

    private fun renderActualDiagnostics(cleanText: String, diagnostics: List<DiagnosticMark>): String {
        val actualInfos = diagnostics.map { diagnostic ->
            ParsedCodeMetaInfo(
                start = diagnostic.startOffset,
                end = diagnostic.endOffset,
                attributes = mutableListOf(),
                tag = diagnostic.name,
                description = null,
            )
        }
        return CodeMetaInfoRenderer.renderTagsToText(actualInfos, cleanText).toString()
    }
}
