package org.cangnova.cangjie.codegen.parity

data class LlvmIrNormalizationOptions(
    val sortTopLevelDeclarations: Boolean = true,
    val collapseEmptyLines: Boolean = true,
    val ignoreCommentLines: Boolean = true,
)

data class LlvmIrDiffEntry(
    val lineNumber: Int,
    val expected: String?,
    val actual: String?,
)

data class LlvmIrComparisonResult(
    val matches: Boolean,
    val normalizedExpected: String,
    val normalizedActual: String,
    val firstDiff: LlvmIrDiffEntry?,
)

class LlvmIrParityComparator(
    private val options: LlvmIrNormalizationOptions = LlvmIrNormalizationOptions(),
) {
    fun normalize(ir: String): String {
        val normalizedLines = ir
            .replace("\r\n", "\n")
            .split('\n')
            .map { it.removePrefix("\uFEFF").trimEnd() }

        val maybeSorted = if (options.sortTopLevelDeclarations) {
            sortTopLevelDeclarations(normalizedLines)
        } else {
            normalizedLines
        }

        val withoutComments = if (options.ignoreCommentLines) {
            maybeSorted.filterNot { it.trimStart().startsWith(";") }
        } else {
            maybeSorted
        }

        val compacted = if (options.collapseEmptyLines) {
            collapseEmptyLines(withoutComments)
        } else {
            withoutComments
        }

        return trimOuterEmptyLines(compacted).joinToString("\n").trimEnd()
    }

    fun compare(expected: String, actual: String): LlvmIrComparisonResult {
        val normalizedExpected = normalize(expected)
        val normalizedActual = normalize(actual)
        val diff = firstDiff(normalizedExpected, normalizedActual)
        return LlvmIrComparisonResult(
            matches = diff == null,
            normalizedExpected = normalizedExpected,
            normalizedActual = normalizedActual,
            firstDiff = diff,
        )
    }

    fun formatFirstDiffReport(result: LlvmIrComparisonResult): String {
        val diff = result.firstDiff ?: return "LLVM-IR parity matched."
        val expected = diff.expected ?: "<missing>"
        val actual = diff.actual ?: "<missing>"
        return buildString {
            appendLine("LLVM-IR parity mismatch")
            appendLine("line: ${diff.lineNumber}")
            appendLine("expected: $expected")
            appendLine("actual  : $actual")
        }.trimEnd()
    }

    private fun firstDiff(expected: String, actual: String): LlvmIrDiffEntry? {
        val expectedLines = expected.split('\n')
        val actualLines = actual.split('\n')
        val limit = maxOf(expectedLines.size, actualLines.size)
        for (index in 0 until limit) {
            val left = expectedLines.getOrNull(index)
            val right = actualLines.getOrNull(index)
            if (left != right) {
                return LlvmIrDiffEntry(
                    lineNumber = index + 1,
                    expected = left,
                    actual = right,
                )
            }
        }
        return null
    }

    private fun sortTopLevelDeclarations(lines: List<String>): List<String> {
        val prefix = mutableListOf<String>()
        val declarations = mutableListOf<String>()
        val body = mutableListOf<String>()

        var inFunctionBody = false
        lines.forEach { rawLine ->
            val line = rawLine
            when {
                line.trimStart().startsWith("define ") -> {
                    inFunctionBody = true
                    body += line
                }
                inFunctionBody -> {
                    body += line
                    if (line == "}") {
                        inFunctionBody = false
                    }
                }
                line.startsWith("declare ") || line.startsWith("%") || line.startsWith("@") -> declarations += line
                else -> prefix += line
            }
        }

        return prefix + declarations.sorted() + body
    }

    private fun collapseEmptyLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var previousWasEmpty = false
        lines.forEach { line ->
            val currentIsEmpty = line.isBlank()
            if (!(currentIsEmpty && previousWasEmpty)) {
                result += line
            }
            previousWasEmpty = currentIsEmpty
        }
        return result
    }

    private fun trimOuterEmptyLines(lines: List<String>): List<String> {
        if (lines.isEmpty()) return lines
        var start = 0
        var end = lines.lastIndex
        while (start <= end && lines[start].isBlank()) start++
        while (end >= start && lines[end].isBlank()) end--
        if (start > end) return emptyList()
        return lines.subList(start, end + 1)
    }
}
