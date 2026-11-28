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
 * 验证注解实参名提示的**候选判定与实参重建**。
 *
 * 重建（`(1, 2)` → 两个表达式）与偏移换算都在 PSI 层完成，形参名解析不参与，
 * 因此可以在纯解析测试里固化「哪些实参会提示、提示插在哪」。
 */
class CjAnnotationArgumentHintsTest : CjParsingTestCase(
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
     * 两个位置实参各自成为一个候选实参，偏移指向实参起点。
     */
    @Test
    fun testPositionalArgumentsAreRebuilt() {
        val source = """
            @Ann(10, 20)
            func demo() {}
        """.trimIndent()

        val candidate = candidates(source).single()

        assertEquals(listOf("10", "20"), candidate.arguments.map { it.text })
        assertEquals(
            listOf(source.indexOf("10"), source.indexOf("20")),
            candidate.arguments.map { it.offset },
        )
    }

    /**
     * 命名实参不产出提示：形参名已经写在源码里。
     */
    @Test
    fun testNamedArgumentsProduceNoCandidate() {
        val source = """
            @Ann(x: 10, y: 20)
            func demo() {}
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 没有实参则不产出候选。
     */
    @Test
    fun testAnnotationWithoutArgumentsProducesNoCandidate() {
        val source = """
            @Plain
            func demo() {}
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 括号内的嵌套结构不参与逗号切分。
     */
    @Test
    fun testNestedCommaIsNotSplit() {
        val source = """
            @Ann([1, 2], 30)
            func demo() {}
        """.trimIndent()

        val candidate = candidates(source).single()

        assertEquals(2, candidate.arguments.size)
        assertEquals(listOf("[1, 2]", "30"), candidate.arguments.map { it.text })
    }

    /**
     * 请求范围之外的注解不产出候选。
     */
    @Test
    fun testRangeFiltersOutCandidate() {
        val source = """
            @First(1)
            func a() {}

            @Second(2)
            func b() {}
        """.trimIndent()

        assertTrue(candidates(source, range = TextRange(0, source.indexOf("@Second"))).size == 1)

        val secondOnly = candidates(source, range = TextRange(source.indexOf("@Second"), source.length))
        assertEquals(listOf("2"), secondOnly.single().arguments.map { it.text })
    }

    /**
     * 解析源码并挑选候选。
     */
    private fun candidates(
        text: String,
        range: TextRange? = null,
    ): List<CjAnnotationArgumentHintCandidate> {
        val file = createPsiFile("hints", text) as CjFile
        return collectAnnotationCandidates(file, range)
    }
}
