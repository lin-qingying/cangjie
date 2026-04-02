package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

data class LspTextDocument(
    val uri: String,
    val languageId: String?,
    val version: Int,
    val text: String,
) {
    fun withText(
        newText: String,
        newVersion: Int,
    ): LspTextDocument = copy(text = newText, version = newVersion)

    fun offsetAt(position: Position): Int = offsetAt(text, position)

    fun positionAt(offset: Int): Position = positionAt(text, offset)

    // 把编译器侧的半开区间 offset 范围换算回 LSP 使用的 UTF-16 行列坐标。
    fun rangeOf(startOffset: Int, endOffset: Int): Range {
        val normalizedStart = startOffset.coerceAtLeast(0)
        val normalizedEnd = endOffset.coerceAtLeast(normalizedStart)
        return Range(
            positionAt(normalizedStart),
            positionAt(normalizedEnd),
        )
    }

    companion object {
        internal fun offsetAt(
            text: String,
            position: Position,
        ): Int {
            val requestedLine = position.line.coerceAtLeast(0)
            val requestedCharacter = position.character.coerceAtLeast(0)
            var line = 0
            var offset = 0

            while (line < requestedLine && offset < text.length) {
                val nextLineBreak = text.indexOf('\n', offset)
                offset = if (nextLineBreak >= 0) nextLineBreak + 1 else text.length
                line++
            }

            val lineEnd = text.indexOf('\n', offset).let { if (it >= 0) it else text.length }
            val availableCharacters = (lineEnd - offset).coerceAtLeast(0)
            return offset + requestedCharacter.coerceAtMost(availableCharacters)
        }

        internal fun positionAt(
            text: String,
            offset: Int,
        ): Position {
            val targetOffset = offset.coerceIn(0, text.length)
            var currentOffset = 0
            var line = 0

            // 逐行推进到目标 offset 所在行，再用剩余偏移量计算该行内字符位置。
            while (currentOffset < targetOffset) {
                val nextLineBreak = text.indexOf('\n', currentOffset)
                if (nextLineBreak < 0 || nextLineBreak >= targetOffset) break
                currentOffset = nextLineBreak + 1
                line++
            }

            return Position(line, targetOffset - currentOffset)
        }

        internal fun applyRangeChange(
            text: String,
            range: Range,
            replacement: String,
        ): String {
            val startOffset = offsetAt(text, range.start)
            val endOffset = offsetAt(text, range.end).coerceAtLeast(startOffset)
            return buildString(text.length - (endOffset - startOffset) + replacement.length) {
                append(text, 0, startOffset)
                append(replacement)
                append(text, endOffset, text.length)
            }
        }
    }
}
