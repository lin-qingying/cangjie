package org.cangnova.cangjie.ide.completion.impl.shared.dummy

import com.intellij.openapi.command.WriteCommandAction
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

// 注意：`kotlin.test.assertEquals` 在期望值与被测值**都是字符串**时会选中 message-first
// 重载，位置参数会把断言消息当成被测值。本文件的字符串断言一律用命名参数。
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * dummy 占位修正回归。
 *
 * 这套逻辑此前**没有任何用例**：它唯一的入口依赖平台的 `CompletionParameters`，
 * 在测试里几乎不可得。而它恰恰是「候选为空但不报错」这类最难定位的失败的源头——
 * 裸 dummy 在仓颉语法里通常不构成合法表达式，解析或作用域解析一失败，
 * 结果就是一个空候选列表，没有任何异常。
 *
 * 因此新增了不依赖平台参数的 [CompletionDummyIdentifierProvider.provideFor]，
 * 本类只经它验证修正结论。
 */
class CompletionDummyIdentifierProviderTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /**
     * 表达式位置的裸 dummy 被换成**语法合法**的占位调用。
     *
     * 不换的话 `let x = IntellijIdeaRulezzz` 里的裸标识符会让后续分析得到空候选。
     */
    @Test
    fun expressionPositionIsReplacedWithCallablePlaceholder() {
        val file = fileWith("func f(): Int64 {\n    IntellijIdeaRulezzz\n}")
        val context = requireNotNull(dummyAt(file)) { "该位置必须允许补全" }

        assertEquals(
            expected = "cangjieCompletionPlaceholder()",
            actual = context.position.text,
            message = "裸 dummy 必须被换成占位调用",
        )
        assertEquals(file, context.dummyFile, "修正发生在传入的文件上")
    }

    /**
     * 类型位置不替换：那里裸标识符本来就能解析成类型引用。
     *
     * 额外替换成调用形态反而会把候选从类型变成值。
     */
    @Test
    fun typePositionKeepsTheBareIdentifier() {
        val file = fileWith("func f(x: IntellijIdeaRulezzz): Int64 {\n    return 0\n}")
        val context = requireNotNull(dummyAt(file)) { "该位置必须允许补全" }

        assertTrue(
            context.position.text.contains(DUMMY_MARKER),
            "类型位置不得被替换成占位调用，实际 ${context.position.text}",
        )
    }

    /**
     * 泛型限定名后的位置同样替换。
     *
     * `holder<Int64>.裸dummy` 里的裸标识符不是合法表达式；不替换就得到空候选。
     */
    @Test
    fun positionAfterGenericQualifierIsReplaced() {
        val file = fileWith("func f(): Int64 {\n    holder<Int64>.IntellijIdeaRulezzz\n}")
        val context = requireNotNull(dummyAt(file)) { "该位置必须允许补全" }

        assertTrue(
            context.position.text.contains("cangjieCompletionPlaceholder"),
            "泛型限定名后的裸 dummy 必须被替换，实际 ${context.position.text}",
        )
    }

    /**
     * 字符串插值内的位置替换；与原始字符串的区别是**这里会求值**。
     */
    @Test
    fun positionInsideStringInterpolationIsReplaced() {
        val file = fileWith("func f(): String {\n    let s = \"\${IntellijIdeaRulezzz}\"\n    return s\n}")
        val context = requireNotNull(dummyAt(file)) { "插值内必须允许补全" }

        assertTrue(
            context.position.text.contains("cangjieCompletionPlaceholder"),
            "插值内的裸 dummy 必须被替换，实际 ${context.position.text}",
        )
    }

    /**
     * 原始字符串内的位置直接排除：那里不插值，补出来的代码永远不会被求值。
     */
    @Test
    fun rawStringPositionIsRejected() {
        val file = fileWith("func f(): Int64 {\n    let s = #\"IntellijIdeaRulezzz\"#\n    return 0\n}")

        assertNull(dummyAt(file), "原始字符串内不允许补全")
    }

    /**
     * 前缀去掉 dummy 文本与限定名。
     *
     * 前缀删错会让平台按错误前缀把候选全部过滤掉——同样是「没有候选」的假象。
     */
    @Test
    fun prefixStripsDummyTextAndQualifier() {
        val file = fileWith("func f(): Int64 {\n    holder.preIntellijIdeaRulezzz\n}")
        val context = requireNotNull(dummyAt(file)) { "该位置必须允许补全" }

        assertEquals(
            expected = "pre",
            actual = context.prefix,
            message = "前缀必须只保留用户真实键入的部分",
        )
    }

    /**
     * 光标前没有任何标识符时前缀为空，而不是整段文本。
     */
    @Test
    fun emptyPrefixWhenNothingTypedBeforeDummy() {
        val file = fileWith("func f(): Int64 {\n    holder.IntellijIdeaRulezzz\n}")
        val context = requireNotNull(dummyAt(file)) { "该位置必须允许补全" }

        assertEquals(
            expected = "",
            actual = context.prefix,
            message = "限定名后的空前缀不得退化成限定名本身",
        )
    }

    /**
     * 不含 dummy 的位置不应被当成补全点：`provideFor` 找不到元素时返回 `null`。
     */
    @Test
    fun missingElementReturnsNull() {
        val file = fileWith("func f(): Int64 {\n    return 0\n}")
        assertNull(CompletionDummyIdentifierProvider.provideFor(file, file.textLength + 10))
    }

    /**
     * 建一个只含一段代码的仓颉文件。
     */
    private fun fileWith(text: String): CjFile = createPsiFile("dummy", text) as CjFile

    /**
     * 在 dummy 标记处执行修正。
     *
     * 占位替换是一次 PSI 写，必须在命令内执行——平台补全入口本身处于补全命令内，
     * 测试要自己开一条，否则平台直接拒绝（`Must not change PSI outside command`）。
     * 这条前置条件原先被实现里的 `runCatching {}` 静默吞掉，表现为「位置留着裸 dummy」，
     * 与「该位置不需要替换」无法区分。
     */
    private fun dummyAt(file: CjFile): CompletionDummyContext? {
        val offset = file.text.indexOf(DUMMY_MARKER)
        require(offset >= 0) { "夹具必须含 dummy 标记" }
        var outcome: CompletionDummyContext? = null
        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project) {
                outcome = CompletionDummyIdentifierProvider.provideFor(file, offset)
            }
        }
        return outcome
    }

    companion object {
        /** 平台插入的 dummy 标识符。 */
        private const val DUMMY_MARKER = "IntellijIdeaRulezzz"
    }
}
