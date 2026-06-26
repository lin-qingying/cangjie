package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.test.codeMetaInfo.CodeMetaInfoRenderer
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.test.codeMetaInfo.model.ParsedCodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.clearTextFromDiagnosticMarkup

/**
 * CFIR 内联诊断结构化 diff 工具。
 *
 * 该对象把测试数据中的 `<!DIAG!>...<!>` 标记解析为期望诊断集合，
 * 并与 CFIR 前端实际收集到的诊断按名称和文本偏移做多重集合比较。
 */
object CfirInlineDiagnosticsDiff {
    /**
     * 比较单个文件的期望内联诊断与实际 CFIR 诊断。
     *
     * 返回 null 表示完全一致；否则返回包含缺失、额外诊断和可写回实际标记文本的结构化 mismatch。
     */
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

    /**
     * 解析内联诊断标记并生成清理后的源码文本。
     *
     * 标记的偏移全部映射到 clean text，保证可以直接与 CFIR diagnostic textRange 对齐。
     */
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

    /**
     * 按顶层逗号切分诊断标记负载。
     *
     * 诊断参数括号中的逗号会被保留，不作为多个诊断的分隔符。
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
     * 规范化项目诊断名称。
     *
     * 兼容旧标记中的 `CFIR_` 前缀，内部比较统一使用无前缀名称。
     */
    fun normalizeDiagnosticName(name: String): String {
        return name.removePrefix("CFIR_").trim()
    }

    /**
     * 将诊断标记渲染为带行列、偏移和源码片段的结构化条目。
     */
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

    /**
     * 把文本偏移转换为一基行列坐标。
     */
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

    /**
     * 正在等待闭合的诊断标记。
     *
     * @property names 标记声明的诊断名称。
     * @property startOffset 标记在 clean text 中的起始偏移。
     */
    private data class OpenDiagnosticMark(val names: List<String>, val startOffset: Int)

    /**
     * 内联诊断解析结果。
     *
     * @property cleanText 移除内联诊断标记后的源码文本。
     * @property expectedDiagnostics 从标记解析出的期望诊断集合。
     */
    private data class ParsedInlineDiagnostics(
        /** 移除内联诊断标记后的源码文本。 */
        val cleanText: String,
        /** 从标记解析出的期望诊断集合。 */
        val expectedDiagnostics: List<DiagnosticMark>,
    )

    /**
     * 诊断比较的最小键。
     *
     * @property name 规范化后的诊断名称。
     * @property startOffset 诊断范围起始偏移。
     * @property endOffset 诊断范围结束偏移。
     */
    data class DiagnosticMark(
        /** 规范化后的诊断名称。 */
        val name: String,
        /** 诊断范围起始偏移。 */
        val startOffset: Int,
        /** 诊断范围结束偏移。 */
        val endOffset: Int,
    )

    /**
     * 适合错误报告展示的诊断条目。
     *
     * @property name 诊断名称。
     * @property startOffset 起始偏移。
     * @property endOffset 结束偏移。
     * @property startLine 起始行号。
     * @property startColumn 起始列号。
     * @property endLine 结束行号。
     * @property endColumn 结束列号。
     * @property snippet 诊断覆盖的源码片段。
     */
    data class DiagnosticEntry(
        /** 诊断名称。 */
        val name: String,
        /** 起始偏移。 */
        val startOffset: Int,
        /** 结束偏移。 */
        val endOffset: Int,
        /** 起始行号。 */
        val startLine: Int,
        /** 起始列号。 */
        val startColumn: Int,
        /** 结束行号。 */
        val endLine: Int,
        /** 结束列号。 */
        val endColumn: Int,
        /** 诊断覆盖的源码片段。 */
        val snippet: String,
    ) {
        /**
         * 渲染完整诊断位置与片段。
         */
        fun render(): String {
            return "$name @ $startLine:$startColumn..$endLine:$endColumn " +
                "(offsets [$startOffset, $endOffset)) on \"$snippet\""
        }

        /**
         * 渲染带文件路径的诊断位置。
         */
        fun renderAt(filePath: String): String {
            return "$name ($filePath:$startLine:$startColumn) " +
                "(to $endLine:$endColumn, offsets [$startOffset, $endOffset), snippet \"$snippet\")"
        }

        /**
         * 渲染适合 mismatch 汇总列表使用的单行摘要。
         */
        fun renderSummary(kind: String): String {
            return "$kind.$name at line $startLine, col $startColumn..$endColumn " +
                "[offsets [$startOffset, $endOffset), snippet \"$snippet\"]"
        }
    }

    /**
     * 单个文件的结构化诊断 mismatch。
     *
     * @property filePath 文件路径。
     * @property missing 期望存在但实际缺失的诊断。
     * @property unexpected 实际出现但期望没有的诊断。
     * @property expectedContent 原始期望源码文本。
     * @property actualContent 根据实际诊断重新渲染出的源码文本。
     */
    data class StructuredDiagnosticMismatch(
        /** 文件路径。 */
        val filePath: String,
        /** 期望存在但实际缺失的诊断。 */
        val missing: List<DiagnosticEntry>,
        /** 实际出现但期望没有的诊断。 */
        val unexpected: List<DiagnosticEntry>,
        /** 原始期望源码文本。 */
        val expectedContent: String,
        /** 根据实际诊断重新渲染出的源码文本。 */
        val actualContent: String,
    )

    /**
     * 把实际诊断集合渲染回测试框架使用的 meta-info 标记文本。
     */
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
