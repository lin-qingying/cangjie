package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证 lambda 返回类型提示的**结构候选判定**。
 *
 * 候选只看 PSI（是否写了返回类型、末语句是什么、range 是否覆盖），不依赖 `CaSession`，
 * 因此可以在纯解析测试里把「提示位置」固化下来。
 */
class CjLambdaHintsCollectorCandidatesTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 初始化 PSI 解析测试环境。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 释放 PSI 解析测试环境。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 普通 lambda：提示锚在末表达式之后。
     */
    @Test
    fun testLambdaReturnHintsAfterLastStatement() {
        val source = """
            func demo() {
                let f = { i: Int64 =>
                    i + 10
                    i + 20
                }
            }
        """.trimIndent()

        val candidate = candidates(source).single()

        assertEquals(source.indexOf("i + 20") + "i + 20".length, candidate.offset)
    }

    /**
     * 尾随 lambda（`list.forEach { ... }`）同样产出候选。
     */
    @Test
    fun testTrailingLambdaProducesCandidate() {
        val source = """
            func demo(values: Array<Int64>) {
                values.forEach { value =>
                    value + 1
                }
            }
        """.trimIndent()

        assertEquals(
            source.indexOf("value + 1") + "value + 1".length,
            candidates(source).single().offset,
        )
    }

    /**
     * 末语句是显式 `return` 时不再补类型。
     */
    @Test
    fun testExplicitReturnLastStatementProducesNoCandidate() {
        val source = """
            func demo() {
                let f = { i: Int64 =>
                    return i + 1
                }
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 空函数体没有末表达式，不产出候选。
     */
    @Test
    fun testEmptyBodyProducesNoCandidate() {
        val source = """
            func demo() {
                let f = { i: Int64 => }
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 请求范围之外的 lambda 不产出候选。
     */
    @Test
    fun testRangeFiltersOutCandidate() {
        val source = """
            func demo() {
                let f = { i: Int64 =>
                    i + 1
                }
            }
        """.trimIndent()

        assertTrue(candidates(source, range = TextRange(0, source.indexOf('\n'))).isEmpty())
        assertEquals(
            1,
            candidates(source, range = TextRange(source.indexOf("let f"), source.length)).size,
        )
    }

    /**
     * 没有 lambda 时没有任何候选。
     */
    @Test
    fun testPlainFunctionProducesNoCandidate() {
        val source = """
            func demo(a: Int64): Int64 {
                return a + 1
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 解析源码并挑选候选。
     */
    private fun candidates(
        text: String,
        range: TextRange? = null,
    ): List<CjLambdaReturnHintCandidate> {
        val file = createPsiFile("hints", text) as CjFile
        return CjLambdaHintsCollector.candidates(file, range)
    }
}
