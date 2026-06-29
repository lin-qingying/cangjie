package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.TextEdit

/**
 * 将重构前后的文件文本转换为 LSP `TextEdit`。
 *
 * Kotlin LSP 在 rename 完成后对 `Renamer.originals` 与当前 PSI 文本做 diff；
 * 仓颉这里保持同一职责边界：refactoring 层只负责修改 PSI，LSP 适配层负责协议编辑表示。
 */
internal object LspTextEditsComputer {
    /**
     * 计算旧文本到新文本之间的 LSP 文本编辑列表。
     *
     * 该方法按 token 粒度寻找公共前后缀和 LCS 锚点，尽量生成局部编辑而不是整文件替换。
     */
    fun computeTextEdits(
        document: LspTextDocument,
        oldText: String,
        newText: String,
    ): List<TextEdit> {
        if (oldText == newText) return emptyList()

        val oldTokens = tokenize(oldText)
        val newTokens = tokenize(newText)
        if (oldTokens.isEmpty() && newTokens.isEmpty()) return emptyList()

        val prefix = commonPrefixLength(oldTokens, newTokens)
        val suffix = commonSuffixLength(oldTokens, newTokens, prefix)
        val oldMiddle = oldTokens.subList(prefix, oldTokens.size - suffix)
        val newMiddle = newTokens.subList(prefix, newTokens.size - suffix)
        val anchors = lcsAnchors(oldMiddle, newMiddle)

        val edits = mutableListOf<TextEdit>()
        var oldIndex = 0
        var newIndex = 0
        for ((oldAnchor, newAnchor) in anchors) {
            addEditIfNeeded(document, oldMiddle, newMiddle, oldIndex, oldAnchor, newIndex, newAnchor, edits)
            oldIndex = oldAnchor + 1
            newIndex = newAnchor + 1
        }
        addEditIfNeeded(document, oldMiddle, newMiddle, oldIndex, oldMiddle.size, newIndex, newMiddle.size, edits)

        return edits
    }

    /**
     * 在两个锚点之间存在差异时追加一个文本编辑。
     *
     * 编辑范围使用旧 token 的 analysis offset，替换文本由新 token 中间段拼接得到。
     */
    private fun addEditIfNeeded(
        document: LspTextDocument,
        oldTokens: List<Token>,
        newTokens: List<Token>,
        oldStart: Int,
        oldEnd: Int,
        newStart: Int,
        newEnd: Int,
        edits: MutableList<TextEdit>,
    ) {
        if (oldStart == oldEnd && newStart == newEnd) return

        val startOffset = when {
            oldStart < oldTokens.size -> oldTokens[oldStart].start
            oldTokens.isNotEmpty() -> oldTokens.last().end
            else -> 0
        }
        val endOffset = when {
            oldEnd > oldStart -> oldTokens[oldEnd - 1].end
            else -> startOffset
        }
        val replacement = newTokens.subList(newStart, newEnd).joinToString(separator = "") { token -> token.text }
        edits += TextEdit(document.analysisRangeOf(startOffset, endOffset), replacement)
    }

    /**
     * 计算两段 token 序列的最长公共子序列锚点。
     *
     * 返回值中的 pair 表示旧序列和新序列中内容相同、可作为差异切分锚点的 token 下标。
     */
    private fun lcsAnchors(
        oldTokens: List<Token>,
        newTokens: List<Token>,
    ): List<Pair<Int, Int>> {
        if (oldTokens.isEmpty() || newTokens.isEmpty()) return emptyList()

        val width = newTokens.size + 1
        val table = IntArray((oldTokens.size + 1) * width)
        fun index(oldIndex: Int, newIndex: Int): Int = oldIndex * width + newIndex

        for (oldIndex in oldTokens.indices.reversed()) {
            for (newIndex in newTokens.indices.reversed()) {
                table[index(oldIndex, newIndex)] =
                    if (oldTokens[oldIndex].text == newTokens[newIndex].text) {
                        table[index(oldIndex + 1, newIndex + 1)] + 1
                    } else {
                        maxOf(table[index(oldIndex + 1, newIndex)], table[index(oldIndex, newIndex + 1)])
                    }
            }
        }

        val anchors = mutableListOf<Pair<Int, Int>>()
        var oldIndex = 0
        var newIndex = 0
        while (oldIndex < oldTokens.size && newIndex < newTokens.size) {
            if (oldTokens[oldIndex].text == newTokens[newIndex].text) {
                anchors += oldIndex to newIndex
                oldIndex++
                newIndex++
            } else if (table[index(oldIndex + 1, newIndex)] >= table[index(oldIndex, newIndex + 1)]) {
                oldIndex++
            } else {
                newIndex++
            }
        }
        return anchors
    }

    /**
     * 计算两段 token 序列从开头开始完全相同的长度。
     */
    private fun commonPrefixLength(
        oldTokens: List<Token>,
        newTokens: List<Token>,
    ): Int {
        val limit = minOf(oldTokens.size, newTokens.size)
        var index = 0
        while (index < limit && oldTokens[index].text == newTokens[index].text) {
            index++
        }
        return index
    }

    /**
     * 计算两段 token 序列在指定公共前缀之后仍然相同的后缀长度。
     */
    private fun commonSuffixLength(
        oldTokens: List<Token>,
        newTokens: List<Token>,
        prefix: Int,
    ): Int {
        var suffix = 0
        val oldLimit = oldTokens.size - prefix
        val newLimit = newTokens.size - prefix
        while (
            suffix < oldLimit &&
            suffix < newLimit &&
            oldTokens[oldTokens.lastIndex - suffix].text == newTokens[newTokens.lastIndex - suffix].text
        ) {
            suffix++
        }
        return suffix
    }

    /**
     * 将文本切分为重命名差异计算使用的 token 序列。
     *
     * 标识符字符连续合并为一个 token，其他字符按单字符 token 保留，以便精确恢复空白和标点。
     */
    private fun tokenize(text: String): List<Token> {
        if (text.isEmpty()) return emptyList()

        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < text.length) {
            val start = index
            if (text[index].isWordPart()) {
                index++
                while (index < text.length && text[index].isWordPart()) {
                    index++
                }
            } else {
                index++
            }
            tokens += Token(text.substring(start, index), start, index)
        }
        return tokens
    }

    /**
     * 判断字符是否属于标识符风格的单词 token。
     */
    private fun Char.isWordPart(): Boolean = isLetterOrDigit() || this == '_'

    /**
     * 表示差异算法中的一个文本 token。
     */
    private data class Token(
        /**
         * token 的原始文本。
         */
        val text: String,

        /**
         * token 在原文本中的起始 offset。
         */
        val start: Int,

        /**
         * token 在原文本中的结束 offset。
         */
        val end: Int,
    )
}
