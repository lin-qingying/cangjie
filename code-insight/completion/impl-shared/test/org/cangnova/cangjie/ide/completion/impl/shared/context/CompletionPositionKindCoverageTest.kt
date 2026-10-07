package org.cangnova.cangjie.ide.completion.impl.shared.context

import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 位置种类**可产出性**的守门测试。
 *
 * 本变更里同一类缺陷出现了三次，每次的形态都一模一样：枚举里声明了某个位置种类，
 * 但 `classify` 没有判据产出它——于是依赖它的代码全是**死分支**，不报错、
 * 只是永远不执行。已发现的三例：`DOCUMENTATION`（文档注释不是 `CjElement`）、
 * `MACRO_TOKENS`（根本没有判据）、以及 `positionElement` 只接进一个区段导致
 * 其余区段在非仓颉位置上一律走 `EXPRESSION`。
 *
 * 因此这里把「每个种类是否可产出」变成**必须显式回答**的问题：
 * 新增一个枚举取值而不在下表登记，本测试直接失败。
 */
class CompletionPositionKindCoverageTest : CjParsingTestCase(
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
     * 每个位置种类都必须在下面某一张表里登记过。
     *
     * 这条是守门条款：新加一个 `CompletionPositionKind` 而不同时决定它怎么产出，
     * 就会在这里失败——比让它悄悄变成死分支好得多。
     */
    @Test
    fun everyKindHasADecidedProducibility() {
        val undecided = CompletionPositionKind.entries
            .filter {
                it !in PRODUCIBLE && it !in COVERED_ELSEWHERE &&
                    it !in KNOWN_UNPRODUCIBLE && it !in UNDETERMINED
            }
            .map { it.name }

        assertTrue(
            undecided.isEmpty(),
            "新增的位置种类必须显式决定可产出性（放进四张表之一）：$undecided",
        )
    }

    /**
     * 登记为「可产出」的种类，必须真的能被 `classify` 产出。
     *
     * 只登记不验证等于没登记——这一条把「写下来的承诺」和「跑得出来的事实」绑在一起。
     */
    @Test
    fun producibleKindsAreActuallyProduced() {
        for ((kind, snippet) in PRODUCIBLE) {
            val file = createPsiFile("coverage", snippet.text) as CjFile
            val position = snippet.locate(file)
            assertEquals(
                kind,
                CompletionPositionContexts.classify(position).kind,
                "${kind.name} 被登记为可产出，但该片段没有产出它（标记 `${snippet.marker}`）",
            )
        }
    }

    /**
     * 登记为「无判据产出」的种类，必须**确实产不出来**。
     *
     * 反方向同样要验：某天有人补了判据却忘了改表，这条会失败并提示把登记改掉。
     */
    @Test
    fun knownUnproducibleKindsAreNotProducedByTheTableSnippets() {
        for ((kind, reason) in KNOWN_UNPRODUCIBLE) {
            assertTrue(reason.isNotBlank(), "${kind.name} 的未产出原因不得为空")
            for ((other, snippet) in PRODUCIBLE) {
                val file = createPsiFile("coverage", snippet.text) as CjFile
                assertTrue(
                    CompletionPositionContexts.classify(snippet.locate(file)).kind != kind,
                    "${other.name} 的片段产出了被登记为「无判据」的 ${kind.name}——请更新登记表",
                )
            }
        }
    }

    /**
     * 一段用于产出某位置种类的源码片段。
     *
     * @param marker 夹具里标出光标处的文本标记，同时用于失败信息。
     * @param text 源码。
     */
    private class Snippet(val marker: String, val text: String) {
        /** 取标记之后的那个叶子元素作为待分类位置。 */
        fun locate(file: CjFile): com.intellij.psi.PsiElement {
            val offset = file.text.indexOf(marker)
            require(offset >= 0) { "片段必须含标记 `$marker`" }
            val caret = offset + marker.length
            return file.findElementAt(caret) ?: file
        }
    }

    private companion object {
        /** 已验证能被 `classify` 产出的种类。 */
        val PRODUCIBLE: Map<CompletionPositionKind, Snippet> = mapOf(
            CompletionPositionKind.TYPE to Snippet(
                marker = "value: ",
                text = "func f(value: Int64): Int64 { value }\n",
            ),
            CompletionPositionKind.IMPORT to Snippet(
                marker = "import ",
                text = "package p\n\nimport std.collection.ArrayList\n",
            ),
            CompletionPositionKind.NAMED_ARGUMENT to Snippet(
                marker = "alpha: ",
                text = "func host(alpha!: Int64): Int64 { alpha }\n\nfunc site(): Int64 { host(alpha: 1) }\n",
            ),
            CompletionPositionKind.EXPRESSION to Snippet(
                marker = "= ",
                text = "package p\n\nvar x = 1\n",
            ),
            CompletionPositionKind.FUNCTION_BODY to Snippet(
                marker = "{ ",
                text = "func f(): Int64 { 0 }\n",
            ),
            CompletionPositionKind.DOCUMENTATION to Snippet(
                marker = "* @pa",
                text = "/**\n * @pa\n */\nfunc f(): Int64 { 0 }\n",
            ),
            CompletionPositionKind.MACRO_TOKENS to Snippet(
                marker = "@Foo[1",
                text = "macro Foo(a: Tokens, i: Tokens): Tokens { a }\n\nvar v: Int64 = @Foo[1+](2+3)\n",
            ),
            // 判据是「在二元表达式内」且不在块内——所以放在顶层初始化里，
            // 放进函数体会先被 FUNCTION_BODY 截走。
            CompletionPositionKind.OPERATOR to Snippet(
                marker = "1 + ",
                text = "package p\n\nvar x = 1 + 2\n",
            ),
        )

        /**
         * 由**其它测试**验证可产出的种类。
         *
         * 它们的位置定位方式与本表不同，塞进来只会写出一段跑不起来的片段：
         * - 代码片段要经 `CjPsiFactory` 构造，宿主不是普通 `CjFile`；
         * - 声明名要传**声明元素**而不是标识符叶子（判据是 `isOnDeclarationIdentifier`）；
         * - 成员访问要取选择子所在的表达式而不是方括号内的叶子。
         *
         * 这三条正是本表「用叶子定位」的边界——写清楚边界，比硬凑一个片段诚实。
         */
        val COVERED_ELSEWHERE: Map<CompletionPositionKind, String> = mapOf(
            CompletionPositionKind.MEMBER_ACCESS to
                "CompletionPositionContextTest.classifiesMemberAccess",
            CompletionPositionKind.DECLARATION_NAME to
                "CfirKeywordCompletionSectionTest.keywordCandidatesAtDeclarationName",
            CompletionPositionKind.CODE_FRAGMENT_EXPRESSION to
                "CompletionPositionContextTest.classifiesExpressionCodeFragment",
            CompletionPositionKind.CODE_FRAGMENT_BLOCK to
                "CompletionPositionContextTest.classifiesBlockCodeFragment",
        )

        /**
         * 已确认**没有**判据产出的种类。
         *
         * 登记它们不是为了心安，而是让「这是一处缺口」变成可读的事实；
         * 将来补上判据时，本测试会提醒把登记改掉。
         */
        val KNOWN_UNPRODUCIBLE: Map<CompletionPositionKind, String> = mapOf(
            CompletionPositionKind.PACKAGE to
                "classify 里没有产出 PACKAGE 的判据（全文只有 IMPORT 等分支），" +
                    "包名位置目前落到 EXPRESSION",
        )

        /**
         * 尚未确定能否产出的种类。
         *
         * 与上表的区别是**诚实**：能产出的写进 [PRODUCIBLE] 并真跑一遍，
         * 确认不能产出的写进 [KNOWN_UNPRODUCIBLE]；说不清的才留在本表。
         */
        val UNDETERMINED: Map<CompletionPositionKind, String> = emptyMap()
    }
}
