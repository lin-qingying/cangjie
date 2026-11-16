package org.cangnova.cangjie.lsp.state

import kotlin.random.Random
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 文档坐标与增量编辑的性质测试（固定种子）。
 *
 * 断言不拿实现和自己比：这里先用「行列表 + 分隔符列表」造出文档模型，再由模型独立算出
 * 偏移与文本，最后与生产实现的结果比对。固定种子让失败可复现——随机测试不能给出
 * 「偶发失败」这种无法归因的结论。
 */
class LspTextDocumentPropertyTest {
    /**
     * 位置与偏移必须互逆（对所有合法位置）。
     */
    @Test
    fun `positions and offsets are inverse for every valid position`() {
        val random = Random(SEED)
        repeat(CASES) { case ->
            val model = randomDocument(random)
            val document = model.toDocument()
            model.positions().forEach { (position, offset) ->
                assertEquals(
                    offset,
                    document.offsetAt(position),
                    "case=$case 位置 $position 的偏移不符（文本=${model.describe()}）",
                )
                assertEquals(
                    position,
                    document.positionAt(offset),
                    "case=$case 偏移 $offset 的位置不符（文本=${model.describe()}）",
                )
            }
        }
    }

    /**
     * 越界位置只断言**可验证的不变量**，不固化实现细节。
     *
     * LSP 规定位置必须落在文档内，越界行为未定义。因此这里要求：
     * 1. 不抛错；
     * 2. 结果落在 `[0, 长度]`；
     * 3. 夹取是幂等的（把结果转回位置再取一次偏移不变）。
     *
     * 具体的「回落到最后一行」是当前实现的约定，不写进断言——否则测试只是在抄实现，
     * 实现一改测试就红，而真正的契约（不越界、不抛错）反而没被守住。
     */
    @Test
    fun `out of range positions stay inside the document and clamp idempotently`() {
        val random = Random(SEED + 1)
        repeat(CASES) { case ->
            val model = randomDocument(random)
            val document = model.toDocument()
            val length = document.text.length

            val positions = buildList {
                model.lines.indices.forEach { line ->
                    add(Position(line, model.lines[line].length + 7))
                    add(Position(line, Int.MAX_VALUE))
                }
                add(Position(model.lines.size + 5, 3))
                add(Position(model.lines.size + 5, Int.MAX_VALUE))
                add(Position(Int.MAX_VALUE, Int.MAX_VALUE))
            }

            positions.forEach { position ->
                val offset = document.offsetAt(position)
                assertTrue(
                    offset in 0..length,
                    "case=$case 位置 $position 的偏移 $offset 越出 [0, $length]（${model.describe()}）",
                )
                val roundTrip = document.offsetAt(document.positionAt(offset))
                assertEquals(
                    offset,
                    roundTrip,
                    "case=$case 夹取必须幂等：$position -> $offset -> $roundTrip",
                )
            }
        }
    }

    /**
     * 范围替换的结果必须与模型独立算出的文本完全一致。
     */
    @Test
    fun `range changes match the model computed text`() {
        val random = Random(SEED + 2)
        repeat(CASES) { case ->
            val model = randomDocument(random)
            val first = model.randomPosition(random)
            val second = model.randomPosition(random)
            val (start, end) = listOf(first, second).sortedWith { left, right -> compare(left, right) }
            val replacement = randomReplacement(random)

            val actual = LspTextDocument.applyRangeChange(
                text = model.toDocument().text,
                range = Range(start, end),
                replacement = replacement,
            )

            assertEquals(
                model.replaceOffsets(model.offsetOf(start), model.offsetOf(end), replacement),
                actual,
                "case=$case 替换结果不符（文本=${model.describe()}，替换=$replacement）",
            )
        }
    }

    /**
     * 分析文本只做换行规范化：CRLF/CR 变 LF，其余字符逐一保留。
     */
    @Test
    fun `analysis text only normalizes line separators`() {
        val random = Random(SEED + 3)
        repeat(CASES) { case ->
            val model = randomDocument(random)
            val document = model.toDocument()

            assertEquals(
                model.lines.joinToString("\n"),
                document.analysisText,
                "case=$case 规范化结果不符（文本=${model.describe()}）",
            )
            assertTrue('\r' !in document.analysisText)
        }
    }

    /**
     * 规范化对不含 CR 的文本必须返回同一实例（热路径不容许多余拷贝）。
     */
    @Test
    fun `normalization returns the same text when no carriage return exists`() {
        val text = "func main() {\n    return\n}\n"
        assertTrue(LspTextDocument.normalizeText(text) === text, "无 CR 文本不应产生新对象")
    }

    /**
     * 位置比较必须满足「行优先、行内按字符」的单调性：偏移递增则位置不减。
     */
    @Test
    fun `positions are monotonic in offset`() {
        val random = Random(SEED + 4)
        repeat(CASES) { case ->
            val document = randomDocument(random).toDocument()
            var previous = Position(0, 0)
            (0..document.text.length).forEach { offset ->
                val current = document.positionAt(offset)
                assertTrue(
                    compare(current, previous) >= 0,
                    "case=$case 偏移 $offset 的位置 $current 小于前一个 $previous",
                )
                previous = current
            }
        }
    }

    /**
     * 一行文档模型：行内容 + 行间分隔符。
     *
     * 位置到偏移的换算由模型独立完成，因此不会和被测实现一起错。
     */
    private data class DocumentModel(
        val lines: List<String>,
        val separators: List<String>,
    ) {
        /** 分隔符数量必须等于行数减一。 */
        init {
            require(lines.isNotEmpty())
            require(separators.size == lines.size - 1)
            require(separators.all { it in BREAKS })
        }

        /** 模型算出的完整文本。 */
        val text: String = buildString {
            lines.forEachIndexed { index, line ->
                append(line)
                if (index < separators.size) append(separators[index])
            }
        }

        /** 全部合法位置及其偏移。 */
        fun positions(): List<Pair<Position, Int>> = buildList {
            lines.forEachIndexed { line, content ->
                val lineStart = offsetOfLineStart(line)
                (0..content.length).forEach { character ->
                    add(Position(line, character) to lineStart + character)
                }
            }
        }

        /** 第 [line] 行行首偏移。 */
        fun offsetOfLineStart(line: Int): Int {
            var offset = 0
            for (index in 0 until line) {
                offset += lines[index].length + separators[index].length
            }
            return offset
        }

        /** 第 [line] 行行尾的偏移（不含分隔符）。 */
        fun offsetOfLineEnd(line: Int): Int = offsetOfLineStart(line) + lines[line].length

        /** 位置对应的模型偏移。 */
        fun offsetOf(position: Position): Int {
            val line = position.line.coerceAtMost(lines.size - 1)
            return offsetOfLineStart(line) + position.character.coerceAtMost(lines[line].length)
        }

        /** 随机合法位置。 */
        fun randomPosition(random: Random): Position {
            val line = random.nextInt(lines.size)
            return Position(line, random.nextInt(lines[line].length + 1))
        }

        /** 按偏移替换范围。 */
        fun replaceOffsets(startOffset: Int, endOffset: Int, replacement: String): String =
            text.substring(0, startOffset) + replacement + text.substring(endOffset)

        /** 便于失败诊断的紧凑描述。 */
        fun describe(): String =
            "lines=${lines.map { it.replace("\r", "\\r").replace("\n", "\\n") }} " +
                "seps=${separators.map { it.replace("\r", "\\r").replace("\n", "\\n") }}"


        companion object {
            /** 从原始文本切分出模型（分隔符只认 LF / CRLF / CR）。 */
            fun parse(raw: String): DocumentModel {
                val lines = mutableListOf<String>()
                val separators = mutableListOf<String>()
                var cursor = 0
                BREAK_PATTERN.findAll(raw).forEach { match ->
                    lines += raw.substring(cursor, match.range.first)
                    separators += match.value
                    cursor = match.range.last + 1
                }
                lines += raw.substring(cursor)
                return DocumentModel(lines, separators)
            }
        }

        /** 转成被测对象。 */
        fun toDocument(): LspTextDocument = LspTextDocument(
            uri = "file:///property/property.cj",
            languageId = "cangjie",
            version = 1,
            text = text,
        )
    }

    /**
     * 随机生成文档模型。
     *
     * 先拼原始文本再**按换行符切分**出模型：如果直接分别随机「行内容」和「行间分隔符」，
     * 相邻的 `\r` 与 `\n` 会拼成 CRLF，而实现把它当**一个**换行——模型与文本就会分叉，
     * 这种不一致是测试造出来的，不是实现的问题。
     */
    private fun randomDocument(random: Random): DocumentModel {
        val lineCount = random.nextInt(1, 5)
        val text = buildString {
            repeat(lineCount) { index ->
                if (index > 0) append(BREAKS[random.nextInt(BREAKS.size)])
                repeat(random.nextInt(0, 6)) { append(randomCharacter(random)) }
            }
        }
        return DocumentModel.parse(text)
    }

    /**
     * 随机字符：覆盖 ASCII、CJK、非 BMP 代理对与制表符。
     */
    private fun randomCharacter(random: Random): String = CHARACTERS[random.nextInt(CHARACTERS.size)]

    /**
     * 随机替换文本。
     */
    private fun randomReplacement(random: Random): String = buildString {
        repeat(random.nextInt(0, 4)) { append(randomCharacter(random)) }
    }

    /**
     * 位置比较：行优先，再比字符。
     */
    private fun compare(left: Position, right: Position): Int = when {
        left.line != right.line -> left.line - right.line
        else -> left.character - right.character
    }

    companion object {
        /** 固定种子：失败可复现。 */
        private const val SEED: Int = 20261111

        /** 每个性质的随机用例数（够覆盖边界，又不至于拖慢 CI）。 */
        private const val CASES: Int = 60

        /** 随机字符表。 */
        private val CHARACTERS = listOf("a", "B", "0", " ", "\t", "中", "文", "😀", "𝄞", "(", "}")

        /** 换行候选。 */
        private val BREAKS = listOf("\n", "\r\n", "\r")

        /** 从文本里识别换行；顺序保证 CRLF 优先于 CR。 */
        private val BREAK_PATTERN = Regex("\r\n|\r|\n")
    }
}
