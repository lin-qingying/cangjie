package org.cangnova.cangjie.lsp.completion

import kotlin.random.Random
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.lsp.testkit.LspEditApplicator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 「接受候选」编辑的性质测试（固定种子）。
 *
 * 断言只看最终文本：编辑必须做到「删掉用户已键入的前缀、插入候选、其余文本原样保留」，
 * 且区间互不重叠。只看编辑条数或起点会让「插入点偏一格」这类错误逃过去——
 * 它在真实客户端上的后果是把用户代码吃掉一个字符。
 */
class LspCompletionEditPropertyTest {
    /**
     * 任意合法替换区间：应用编辑后的文本必须保留前后文、并以候选文本开头。
     */
    @Test
    fun `applying candidate edits preserves surrounding text for random documents`() {
        val random = Random(SEED)
        repeat(CASES) { case ->
            val text = randomDocument(random)
            val line = random.nextInt(text.lineSequence().count())
            val lineStart = lineStartOffset(text, line)
            val lineLength = lineLength(text, line)
            val replaceEnd = lineStart + random.nextInt(lineLength + 1)
            val replaceStart = lineStart + random.nextInt(0, replaceEnd - lineStart + 1)
            val lookupString = CANDIDATES[random.nextInt(CANDIDATES.size)]

            val edits = LspCompletionEditCalculator.candidateEdits(
                text = text,
                replaceStart = replaceStart,
                replaceEnd = replaceEnd,
                snapshot = snapshot(lookupString, callable = random.nextBoolean()),
            )
            val applied = LspEditApplicator.apply(text, edits)

            assertTrue(
                applied.startsWith(text.substring(0, replaceStart)),
                "case=$case 前缀被破坏：$applied",
            )
            assertTrue(
                applied.endsWith(text.substring(replaceEnd)),
                "case=$case 后缀被破坏：$applied",
            )
            val inserted = applied.substring(replaceStart, applied.length - (text.length - replaceEnd))
            assertTrue(
                inserted.startsWith(lookupString),
                "case=$case 插入文本必须以候选名开头：$inserted",
            )
        }
    }

    /**
     * 非法替换区间必须显式拒绝：越界先被 require 拦住，而不是算出错位编辑。
     */
    @Test
    fun `invalid replacement ranges are rejected`() {
        val text = "func f() {\n    buil\n}"
        val snapshot = snapshot("buildUser", callable = true)

        assertThrows(IllegalArgumentException::class.java) {
            LspCompletionEditCalculator.candidateEdits(text, 5, 4, snapshot)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LspCompletionEditCalculator.candidateEdits(text, 0, text.length + 1, snapshot)
        }
    }

    /**
     * CRLF 文档：编辑位置必须按原始文本坐标给出，应用后行尾风格不变。
     */
    @Test
    fun `crlf documents keep line endings and suffix intact`() {
        val text = "func f() {\r\n    buil\r\n}\r\n"
        val replaceStart = text.indexOf("buil")
        val replaceEnd = replaceStart + "buil".length

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = replaceStart,
            replaceEnd = replaceEnd,
            snapshot = snapshot("buildUser", callable = true),
        )
        val applied = LspEditApplicator.apply(text, edits)

        assertEquals(3, applied.count { it == '\r' }, "CRLF 行尾数量不得变化")
        assertTrue(applied.endsWith("}\r\n"))
        assertTrue(applied.contains("buildUser"))
    }

    /**
     * 命名实参候选插入的是完整 `name: `，不是 `name: ` 前面再带一遍已键入前缀。
     */
    @Test
    fun `named argument candidates insert the whole prefix once`() {
        val text = "func f() {\n    be\n}"
        val replaceStart = text.indexOf("be")
        val replaceEnd = replaceStart + 2

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = replaceStart,
            replaceEnd = replaceEnd,
            snapshot = snapshot("beta", callable = false, namedArgumentPrefix = "beta: "),
        )
        val applied = LspEditApplicator.apply(text, edits)

        assertTrue(applied.contains("beta: "), "命名实参必须插入完整前缀：$applied")
        assertTrue(!applied.contains("betabeta"), "前缀只能插入一次：$applied")
    }

    /**
     * 随机文档：多行文本，混用 LF 与 CRLF，行内带可供补全的前缀。
     */
    private fun randomDocument(random: Random): String = buildString {
        val lineCount = random.nextInt(1, 5)
        repeat(lineCount) { index ->
            append(PREFIXES[random.nextInt(PREFIXES.size)])
            if (index < lineCount - 1) append(if (random.nextBoolean()) "\n" else "\r\n")
        }
    }

    /**
     * 行首偏移。
     */
    private fun lineStartOffset(text: String, line: Int): Int {
        var offset = 0
        var current = 0
        while (current < line) {
            val newline = text.indexOf('\n', offset)
            if (newline < 0) return text.length
            offset = newline + 1
            current++
        }
        return offset
    }

    /**
     * 行内容长度（不含行尾的 `\r`）。
     */
    private fun lineLength(text: String, line: Int): Int {
        val start = lineStartOffset(text, line)
        val newline = text.indexOf('\n', start).let { if (it < 0) text.length else it }
        var end = newline
        if (end > start && text[end - 1] == '\r') end--
        return end - start
    }

    /**
     * 测试候选。
     */
    private fun snapshot(
        lookupString: String,
        callable: Boolean,
        namedArgumentPrefix: String = "",
    ) = completionSnapshot(
        lookupString = lookupString,
        kind = if (callable) CompletionItemKind.TOP_LEVEL else CompletionItemKind.LOCAL,
        origin = CompletionOrigin.TOP_LEVEL,
        tailText = if (callable) "()" else "",
        insertParenthesis = callable,
        namedArgumentPrefix = namedArgumentPrefix,
        typeParameterList = "",
    ).build()

    companion object {
        /** 固定种子。 */
        private const val SEED: Int = 20261111

        /** 随机用例数。 */
        private const val CASES: Int = 40

        /** 随机行内容：都带一个可替换的标识符前缀。 */
        private val PREFIXES = listOf("    buil", "    be", "val x = gr", "    us", "fun f() { ret")

        /** 候选名。 */
        private val CANDIDATES = listOf("buildUser", "beta", "greet", "user", "return")
    }
}
