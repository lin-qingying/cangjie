package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

/**
 * LSP 文档快照。
 *
 * 这里显式区分两套文本语义：
 * 1. `text` 保留客户端原始文本，用于增量编辑、版本流转与协议层 offset 计算；
 * 2. `analysisText` 规范化为 `\n`，只供 PSI / Analysis API 快照消费。
 *
 * 这样既不会破坏 LSP 的 CRLF 文本模型，也不会让 Analysis API 再和客户端换行风格耦合。
 */
data class LspTextDocument(
    /**
     * 文档在 LSP 协议中的 URI。
     */
    val uri: String,

    /**
     * 客户端上报的语言标识。
     */
    val languageId: String?,

    /**
     * 客户端维护的文档版本号。
     */
    val version: Int,

    /**
     * 客户端原始文本内容，保留原始换行风格。
     */
    val text: String,
) {
    /**
     * 供 Analysis API 和 PSI 使用的规范化文本。
     *
     * 该属性懒加载并只把 CRLF/CR 换行规范化为 LF，不回写到协议层原始文本。
     */
    val analysisText: String by lazy(LazyThreadSafetyMode.NONE) {
        normalizeText(text)
    }

    /**
     * 基于新文本和新版本创建文档快照。
     *
     * URI 与语言标识保持不变，确保增量更新只改变内容和版本。
     */
    fun withText(
        newText: String,
        newVersion: Int,
    ): LspTextDocument = copy(
        text = newText,
        version = newVersion,
    )

    /**
     * 基于客户端原始文本执行 `Position -> offset`。
     *
     * 这条链只服务于协议层增量编辑与原始文本定位。
     */
    fun offsetAt(position: Position): Int = offsetAt(text, position)

    /**
     * 基于客户端原始文本执行 `offset -> Position`。
     */
    fun positionAt(offset: Int): Position = positionAt(text, offset)

    /**
     * 基于 Analysis 规范化文本执行 `Position -> offset`。
     */
    fun analysisOffsetAt(position: Position): Int = offsetAt(analysisText, position)

    /**
     * 基于 Analysis 规范化文本执行 `offset -> Position`。
     */
    fun analysisPositionAt(offset: Int): Position = positionAt(analysisText, offset)

    /**
     * 把原始文本 offset 范围转换成 LSP 行列坐标。
     */
    fun rangeOf(startOffset: Int, endOffset: Int): Range {
        val normalizedStart = startOffset.coerceAtLeast(0)
        val normalizedEnd = endOffset.coerceAtLeast(normalizedStart)
        return Range(
            positionAt(normalizedStart),
            positionAt(normalizedEnd),
        )
    }

    /**
     * 把 Analysis 规范化文本 offset 范围转换成 LSP 行列坐标。
     */
    fun analysisRangeOf(startOffset: Int, endOffset: Int): Range {
        val normalizedStart = startOffset.coerceAtLeast(0)
        val normalizedEnd = endOffset.coerceAtLeast(normalizedStart)
        return Range(
            analysisPositionAt(normalizedStart),
            analysisPositionAt(normalizedEnd),
        )
    }

    companion object {
        /**
         * 把客户端原始文本规范化成 Analysis API / PSI 使用的 `\n` 文本。
         *
         * 文档存储不再直接回写规范化结果；规范化只发生在进入 Analysis 快照时。
         */
        fun normalizeText(text: String): String {
            if (text.indexOf('\r') < 0) return text

            val normalized = StringBuilder(text.length)
            var index = 0
            while (index < text.length) {
                val current = text[index]
                if (current == '\r') {
                    normalized.append('\n')
                    if (index + 1 < text.length && text[index + 1] == '\n') {
                        index += 2
                    } else {
                        index += 1
                    }
                } else {
                    normalized.append(current)
                    index += 1
                }
            }
            return normalized.toString()
        }

        internal fun offsetAt(
            text: String,
            position: Position,
        ): Int {
            val requestedLine = position.line.coerceAtLeast(0)
            val requestedCharacter = position.character.coerceAtLeast(0)

            var line = 0
            var lineStartOffset = 0
            var index = 0

            while (index < text.length && line < requestedLine) {
                val current = text[index]
                when {
                    current == '\r' && index + 1 < text.length && text[index + 1] == '\n' -> {
                        index += 2
                        line++
                        lineStartOffset = index
                    }

                    current == '\r' || current == '\n' -> {
                        index += 1
                        line++
                        lineStartOffset = index
                    }

                    else -> {
                        index++
                    }
                }
            }

            var lineEndOffset = text.length
            index = lineStartOffset
            while (index < text.length) {
                val current = text[index]
                if (current == '\r' || current == '\n') {
                    lineEndOffset = index
                    break
                }
                index++
            }

            val availableCharacters = (lineEndOffset - lineStartOffset).coerceAtLeast(0)
            return lineStartOffset + requestedCharacter.coerceAtMost(availableCharacters)
        }

        internal fun positionAt(
            text: String,
            offset: Int,
        ): Position {
            val targetOffset = offset.coerceIn(0, text.length)
            var line = 0
            var lineStartOffset = 0
            var index = 0

            while (index < targetOffset) {
                val current = text[index]
                when {
                    current == '\r' && index + 1 < targetOffset && text[index + 1] == '\n' -> {
                        index += 2
                        line++
                        lineStartOffset = index
                    }

                    current == '\r' || current == '\n' -> {
                        index += 1
                        line++
                        lineStartOffset = index
                    }

                    else -> {
                        index++
                    }
                }
            }

            return Position(line, targetOffset - lineStartOffset)
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
