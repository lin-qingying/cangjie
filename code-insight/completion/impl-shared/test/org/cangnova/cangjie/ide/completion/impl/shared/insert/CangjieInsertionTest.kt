package org.cangnova.cangjie.ide.completion.impl.shared.insert

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaretPolicy
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionInsertionData
import org.cangnova.cangjie.ide.completion.api.lookup.InsertionValidity
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.namedArgumentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 插入执行回归。
 *
 * 固定的是**写入决策**，不是平台 `InsertionContext` 的接线：
 * 插入发生在写事务里，任何一次错误写入都会破坏用户代码，因此
 * 「什么时候不写」「括号补不补」「caret 落在哪」必须各自可断言。
 *
 * 覆盖的四种触发差异：
 * - 失效候选一个字符都不写；
 * - 已存在左括号时不重复补（Tab 补全落在 `foo(|)` 的最常见情形）；
 * - 已有实参与闭合括号时 caret 停在右括号之前；
 * - 命名实参只写仓颉的 `name: `，绝不写 Kotlin 的 `name = `。
 */
class CangjieInsertionTest {
    /**
     * 待写入的文本。
     *
     * 用纯文本 sink 而不是平台 `Document`：文档写入受 EDT 与写访问检查约束，
     * 在轻量夹具里只能改用 EDT 包装，而那样验证的是平台 API 而不是插入策略。
     */
    private lateinit var target: FakeTextSink

    /**
     * 构造一份初始文本。
     */
    private fun text(initial: String): FakeTextSink = FakeTextSink(initial).also { target = it }

    /**
     * 失效候选不得写文档：文本与 caret 都不变。
     */
    @Test
    fun staleCandidateWritesNothing() {
        text("alpha")
        val before = target.text()
        var caret = 5
        val result = performCangjieInsertion(
            snapshot = callableSnapshot(insertParenthesis = true),
            sink = target,
            validity = InsertionValidity.StaleDocument(expectedStamp = 1L, actualStamp = 2L),
            caretOffset = 5,
        ) { caret = it }
        assertFalse(result.wroteText, "失效候选不得写文档")
        assertEquals(before, target.text(), "失效候选不得改动任何字符")
        assertEquals(5, caret, "失效候选不得移动 caret：原地不动作业比移动到猜出来的位置安全")
    }

    /**
     * 区间越界的候选同样不得写文档。
     */
    @Test
    fun outOfRangeCandidateWritesNothing() {
        text("ab")
        val result = performCangjieInsertion(
            snapshot = callableSnapshot(insertParenthesis = true),
            sink = target,
            validity = InsertionValidity.OutOfRange(10, 20, 2),
            caretOffset = 5,
        ) {}
        assertFalse(result.wroteText, "越界候选不得写文档")
        assertEquals("ab", target.text(), "越界候选不得改动任何字符")
    }

    /**
     * 有效候选在光标后没有括号时补一对括号，并把 caret 放到括号内。
     */
    @Test
    fun appendsParenthesisAndMovesCaretInside() {
        text("alpha beta")
        var caret = 5
        val result = performCangjieInsertion(
            snapshot = callableSnapshot(insertParenthesis = true),
            sink = target,
            validity = InsertionValidity.Valid,
            caretOffset = 5,
        ) { caret = it }
        assertTrue(result.wroteText, "有效候选必须写括号")
        assertEquals("alpha() beta", target.text(), "括号必须插在 caret 处，不额外补空格")
        assertEquals(6, caret, "caret 必须落在新写入的左括号之后")
    }

    /**
     * 插入点之后已有左括号时不再补括号。
     *
     * 这是 Tab 补全最常见的落点：`foo(|)` 里补 `foo` 的实参，
     * 重复补会得到 `foo((|))`。
     */
    @Test
    fun doesNotDuplicateExistingParenthesis() {
        text("alpha() beta")
        var caret = 5
        val result = performCangjieInsertion(
            snapshot = callableSnapshot(insertParenthesis = true),
            sink = target,
            validity = InsertionValidity.Valid,
            caretOffset = 5,
        ) { caret = it }
        assertFalse(result.wroteText, "已有左括号时不得再补括号")
        assertEquals("alpha() beta", target.text(), "文档不得被改动")
        assertEquals(5, caret, "已有括号时 caret 停在插入点")
    }

    /**
     * 已有实参与闭合括号时，caret 落在右括号之前而不是插入文本末尾。
     *
     * 停在末尾会把用户写好的实参挤到括号外。
     */
    @Test
    fun caretStopsBeforeExistingClosingParenthesis() {
        // caret 落在括号内、闭合括号之前，且中间隔着空格：
        // 这正是「实参已写好、闭括号已存在」的真实形态。
        text("alpha( ) beta")
        var caret = 6
        performCangjieInsertion(
            snapshot = callableSnapshot(
                insertParenthesis = false,
                caretPolicy = CaretPolicy.BEFORE_EXISTING_CLOSING_PARENTHESIS,
            ),
            sink = target,
            validity = InsertionValidity.Valid,
            caretOffset = 6,
        ) { caret = it }
        assertEquals(7, caret, "caret 必须跨过空格停在右括号之前")
        assertEquals("alpha( ) beta", target.text(), "已有括号与实参不得被破坏")
    }

    /**
     * 不需要括号的候选（类型、关键字、包名）不得补括号。
     */
    @Test
    fun typeCandidateDoesNotAppendParenthesis() {
        text("alpha beta")
        val result = performCangjieInsertion(
            snapshot = typeSnapshot(),
            sink = target,
            validity = InsertionValidity.Valid,
            caretOffset = 5,
        ) {}
        assertFalse(result.wroteText, "类型候选不得补括号")
        assertEquals("alpha beta", target.text(), "文档不得被改动")
    }

    /**
     * 命名实参写入仓颉的 `name: ` 形式，caret 落在冒号之后。
     */
    @Test
    fun namedArgumentUsesCangjieColonForm() {
        text("alpha()")
        var caret = 5
        val result = performNamedArgumentInsertion(
            snapshot = namedArgumentSnapshot("size").build(),
            sink = target,
            validity = InsertionValidity.Valid,
            caretOffset = 5,
        ) { caret = it }
        assertTrue(result.wroteText, "命名实参必须写入前缀")
        assertEquals("alphasize: ()", target.text(), "必须写仓颉的冒号形式")
        assertEquals("alphasize: ", target.text().substring(0, caret), "caret 必须落在冒号之后")
        assertFalse(target.text().contains("="), "不得插入 Kotlin 的等号形式")
    }

    /**
     * 失效的命名实参候选同样不写文档。
     */
    @Test
    fun staleNamedArgumentWritesNothing() {
        text("alpha()")
        val result = performNamedArgumentInsertion(
            snapshot = namedArgumentSnapshot("size").build(),
            sink = target,
            validity = InsertionValidity.StaleDocument(1L, 2L),
            caretOffset = 5,
        ) {}
        assertFalse(result.wroteText, "失效的命名实参候选不得写文档")
        assertEquals("alpha()", target.text(), "文档不得被改动")
    }

    /**
     * 构造一个需要括号的调用候选。
     */
    private fun callableSnapshot(
        insertParenthesis: Boolean,
        caretPolicy: CaretPolicy = CaretPolicy.INSIDE_NEW_PARENTHESIS,
    ): CompletionSnapshot = completionSnapshot(
        lookupString = "alpha",
        kind = CompletionItemKind.MEMBER,
        origin = CompletionOrigin.MEMBER,
        insertParenthesis = insertParenthesis,
    ).build().copy(insertion = insertionOf(insertParenthesis, caretPolicy))

    /**
     * 构造一个不需要括号的类型候选。
     */
    private fun typeSnapshot(): CompletionSnapshot = completionSnapshot(
        lookupString = "Alpha",
        kind = CompletionItemKind.TOP_LEVEL,
        origin = CompletionOrigin.TOP_LEVEL,
        identityKind = CompletionIdentityKind.TYPE,
    ).build()

    /**
     * 覆盖插入数据，使 caret 策略与括号需求可分别指定。
     */
    private fun insertionOf(
        insertParenthesis: Boolean,
        caretPolicy: CaretPolicy,
    ) = CompletionInsertionData(
        insertParenthesis = insertParenthesis,
        namedArgumentPrefix = "",
        caretPolicy = caretPolicy,
        documentStamp = 0L,
    )
}

/**
 * 纯文本 sink：以 `StringBuilder` 为后端，可在无平台环境的单元测试里使用。
 */
private class FakeTextSink(initial: String) : CompletionTextSink {
    private val buffer = StringBuilder(initial)

    override val length: Int
        get() = buffer.length

    override fun charAt(offset: Int): Char =
        if (offset in 0 until buffer.length) buffer[offset] else ' '

    override fun insert(offset: Int, text: String) {
        buffer.insert(offset, text)
    }

    /** 当前全文。 */
    fun text(): String = buffer.toString()

    /** 从 [start] 起的子串。 */
    fun substring(start: Int): String = buffer.substring(start)
}
