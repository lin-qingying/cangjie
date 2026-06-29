package org.cangnova.cangjie.codegen.parity

/**
 * LLVM IR parity 比较前的规范化选项。
 */
data class LlvmIrNormalizationOptions(
    /**
     * 是否排序顶层声明以消除声明顺序差异。
     */
    val sortTopLevelDeclarations: Boolean = true,
    /**
     * 是否折叠连续空行。
     */
    val collapseEmptyLines: Boolean = true,
    /**
     * 是否忽略 LLVM 注释行。
     */
    val ignoreCommentLines: Boolean = true,
)

/**
 * LLVM IR 首个差异项。
 */
data class LlvmIrDiffEntry(
    /**
     * 差异所在的 1-based 行号。
     */
    val lineNumber: Int,
    /**
     * 期望 IR 中的行；实际多出行时为空。
     */
    val expected: String?,
    /**
     * 实际 IR 中的行；期望多出行时为空。
     */
    val actual: String?,
)

/**
 * LLVM IR parity 比较结果。
 */
data class LlvmIrComparisonResult(
    /**
     * 规范化后 IR 是否完全一致。
     */
    val matches: Boolean,
    /**
     * 规范化后的期望 IR。
     */
    val normalizedExpected: String,
    /**
     * 规范化后的实际 IR。
     */
    val normalizedActual: String,
    /**
     * 首个差异；完全一致时为空。
     */
    val firstDiff: LlvmIrDiffEntry?,
)

/**
 * LLVM IR 文本 parity 比较器。
 */
class LlvmIrParityComparator(
    /**
     * 当前比较器使用的规范化选项。
     */
    private val options: LlvmIrNormalizationOptions = LlvmIrNormalizationOptions(),
) {
    /**
     * 对 LLVM IR 文本做稳定规范化。
     */
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

    /**
     * 比较两段 LLVM IR 文本的规范化结果。
     */
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

    /**
     * 格式化首个差异报告。
     */
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

    /**
     * 查找两段规范化 IR 文本的首个行级差异。
     */
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

    /**
     * 排序函数体之外的顶层声明，保留函数体内部顺序。
     */
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

    /**
     * 折叠连续空行。
     */
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

    /**
     * 裁剪首尾空行。
     */
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
