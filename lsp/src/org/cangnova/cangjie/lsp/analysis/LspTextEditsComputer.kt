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

    private fun Char.isWordPart(): Boolean = isLetterOrDigit() || this == '_'

    private data class Token(
        val text: String,
        val start: Int,
        val end: Int,
    )
}
