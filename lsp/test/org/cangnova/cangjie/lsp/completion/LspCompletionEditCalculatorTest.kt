package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextEdit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LSP 侧「接受候选」编辑计算回归。
 *
 * 断言方式是**把算出的编辑应用到原文**再看结果文本：只断言编辑条数或区间
 * 会让「删除前缀与插入位置错开一格」这类错误照样通过，而它的后果是插入 `bubuildUser`
 * 或把用户代码删掉一个字符——都只能在最终文本上才看得出来。
 */
class LspCompletionEditCalculatorTest {
    /**
     * 普通调用形态候选：替换前缀并补一对括号。
     */
    @Test
    fun replacesPrefixAndAddsParenthesesForCallableCandidate() {
        val text = "func f() {\n    buil\n}"
        val start = text.indexOf("buil")

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = start,
            replaceEnd = start + "buil".length,
            snapshot = snapshot(
                lookupString = "buildUser",
                kind = CompletionItemKind.TOP_LEVEL,
                tailText = "(): User",
                insertParenthesis = true,
            ),
        )

        assertEquals(
            "func f() {\n    buildUser()\n}",
            apply(text, edits),
            "接受候选后应得到「替换前缀 + 补括号」的文本，实际编辑 $edits",
        )
    }

    /**
     * 用户已经手写左括号并填好实参时不得再补一对：那会写出 `foo(()` 或 `foo()(x)`。
     */
    @Test
    fun doesNotAddParenthesesWhenCallAlreadyWritten() {
        val text = "func f() {\n    buil(x)\n}"
        val start = text.indexOf("buil")

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = start,
            replaceEnd = start + "buil".length,
            snapshot = snapshot(
                lookupString = "buildUser",
                kind = CompletionItemKind.TOP_LEVEL,
                tailText = "(x: Int64): User",
                insertParenthesis = true,
            ),
        )

        assertEquals("func f() {\n    buildUser(x)\n}", apply(text, edits), "已写括号时只替换前缀")
    }

    /**
     * 命名实参候选插入的是**整个** `name: `，不是「名字 + 前缀」。
     *
     * 后者会写出 `betabeta: `——这是命名实参插入最容易踩的一格。
     */
    @Test
    fun namedArgumentCandidateInsertsWholeNamePrefix() {
        val text = "func f() {\n    g(bet)\n}"
        val start = text.indexOf("bet")

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = start,
            replaceEnd = start + "bet".length,
            snapshot = snapshot(
                lookupString = "beta",
                kind = CompletionItemKind.NAMED_ARGUMENT,
                namedArgumentPrefix = "beta: ",
            ),
        )

        assertEquals("func f() {\n    g(beta: )\n}", apply(text, edits), "命名实参插入必须是 `name: `")
    }

    /**
     * 没有已键入前缀时不该产生删除编辑。
     */
    @Test
    fun emptyPrefixProducesOnlyInsertion() {
        val text = "func f() {\n    \n}"
        val caret = text.indexOf("    ") + 4

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = caret,
            replaceEnd = caret,
            snapshot = snapshot(lookupString = "value", kind = CompletionItemKind.LOCAL),
        )

        assertEquals("func f() {\n    value\n}", apply(text, edits))
        assertTrue(edits.none { it.newText.isEmpty() }, "空前缀不该产生删除编辑，实际 $edits")
    }

    /**
     * CRLF 文本的行列换算：`\r` 不计入列号。
     *
     * 换算出错的表现是编辑整体右移一列，把用户写好的字符吃掉。
     */
    @Test
    fun crlfTextKeepsColumnNumbersExact() {
        val text = "func f() {\r\n    bu\r\n}\r\n"
        val start = text.indexOf("bu")

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = start,
            replaceEnd = start + 2,
            snapshot = snapshot(lookupString = "buildUser", kind = CompletionItemKind.LOCAL),
        )

        val deletion = edits.first { it.newText.isEmpty() }
        assertEquals(1, deletion.range.start.line, "第二行（0 基）")
        assertEquals(4, deletion.range.start.character, "缩进 4 列，`\\r` 不计入")
        assertEquals(6, deletion.range.end.character, "前缀 2 个字符")
        assertEquals("func f() {\r\n    buildUser\r\n}\r\n", apply(text, edits))
    }

    /**
     * 替换区间越界说明文档已变：必须报错让调用方判定会话过期，不得硬插。
     */
    @Test
    fun outOfRangeReplacementIsRejected() {
        val text = "func f() {}"

        assertThrows(IllegalArgumentException::class.java) {
            LspCompletionEditCalculator.candidateEdits(
                text = text,
                replaceStart = 10,
                replaceEnd = text.length + 5,
                snapshot = snapshot(lookupString = "value", kind = CompletionItemKind.LOCAL),
            )
        }
    }

    /**
     * 泛型类型候选把类型参数列表一起写进编辑。
     *
     * 两端共用共享的插入实现：IDE 侧补出 `Box<T>`，LSP 侧必须**逐字一致**——
     * 否则同一个候选在两个端点上落盘成不同文本，而这是用户直接看得见的差异。
     */
    @Test
    fun genericTypeCandidateInsertsTypeParameterList() {
        val text = "func f() {\n    let x: Box\n}"
        val start = text.indexOf("Box")

        val edits = LspCompletionEditCalculator.candidateEdits(
            text = text,
            replaceStart = start,
            replaceEnd = start + "Box".length,
            snapshot = snapshot(
                lookupString = "Box",
                kind = CompletionItemKind.TOP_LEVEL,
                tailText = "<T>",
                typeParameterList = "<T>",
            ),
        )

        assertEquals(
            "func f() {\n    let x: Box<T>\n}",
            apply(text, edits),
            "泛型候选必须把类型参数列表写进编辑，实际 $edits",
        )
    }

    /**
     * 把编辑应用到原文，得到最终文本。
     *
     * 编辑全部相对**原文**，因此按偏移升序一次走完：复制区间之间的原文、替换被编辑的区间。
     * 不用「按偏移降序就地 replace」——那要求每步偏移都在同一坐标系里，写错一格就会
     * 变成越界或吃掉字符，而这类错误与「编辑本身算错」难以区分。
     */
    private fun apply(text: String, edits: List<TextEdit>): String {
        val sorted = edits.sortedBy { offsetOf(text, it.range.start) }
        val out = StringBuilder()
        var cursor = 0
        for (edit in sorted) {
            val start = offsetOf(text, edit.range.start)
            val end = offsetOf(text, edit.range.end)
            require(start >= cursor) { "编辑区间重叠：[$start, $end) 落在已处理到的 $cursor 之前" }
            out.append(text, cursor, start)
            out.append(edit.newText)
            cursor = end
        }
        out.append(text, cursor, text.length)
        return out.toString()
    }

    /**
     * 位置 → 偏移（测试文本均为 `\n` 或 `\r\n`，按行首累加）。
     */
    private fun offsetOf(text: String, position: Position): Int {
        var line = 0
        var offset = 0
        while (line < position.line && offset < text.length) {
            val newline = text.indexOf('\n', offset)
            if (newline < 0) break
            offset = newline + 1
            line++
        }
        return offset + position.character
    }

    /**
     * 测试候选。
     */
    private fun snapshot(
        lookupString: String,
        kind: CompletionItemKind,
        tailText: String = "",
        insertParenthesis: Boolean = false,
        namedArgumentPrefix: String = "",
        typeParameterList: String = "",
    ): CompletionSnapshot = completionSnapshot(
        lookupString = lookupString,
        kind = kind,
        origin = CompletionOrigin.TOP_LEVEL,
        tailText = tailText,
        insertParenthesis = insertParenthesis,
        namedArgumentPrefix = namedArgumentPrefix,
        typeParameterList = typeParameterList,
    ).build()
}
