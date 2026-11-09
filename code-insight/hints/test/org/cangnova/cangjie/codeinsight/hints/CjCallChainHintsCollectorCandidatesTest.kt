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
 * 验证链式调用提示的**结构候选判定**。
 *
 * 链的拆分、换行判定与 range 过滤都在 PSI 层完成，因此可以在纯解析测试里固化
 * 「哪些环节会被提示、提示插在哪」。
 */
class CjCallChainHintsCollectorCandidatesTest : CjParsingTestCase(
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
     * 多行链上每个调用环节各一个候选，位置在该环节末尾。
     */
    @Test
    fun testMultilineChainMarksEveryCallLink() {
        val source = """
            func demo(box: Box) {
                box
                    .first()
                    .second()
            }
        """.trimIndent()

        val candidates = candidates(source)

        assertEquals(2, candidates.size)
        assertEquals(
            listOf(
                source.indexOf(".first()") + ".first()".length,
                source.indexOf(".second()") + ".second()".length,
            ),
            candidates.map { it.offset },
        )
    }

    /**
     * 单行链不产出候选：一眼能读完，提示只是噪声。
     */
    @Test
    fun testSingleLineChainProducesNoCandidate() {
        val source = """
            func demo(box: Box) {
                box.first().second()
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 纯属性访问链不产出候选：提示只针对调用环节。
     */
    @Test
    fun testPropertyOnlyChainProducesNoCandidate() {
        val source = """
            func demo(box: Box) {
                box
                    .name
                    .length
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 只有一格（`a.b()`）不算链。
     */
    @Test
    fun testSingleLinkProducesNoCandidate() {
        val source = """
            func demo(box: Box) {
                box
                    .first()
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 请求范围之外的环节不产出候选。
     */
    @Test
    fun testRangeFiltersOutCandidates() {
        val source = """
            func demo(box: Box) {
                box
                    .first()
                    .second()
            }
        """.trimIndent()

        assertTrue(candidates(source, range = TextRange(0, source.indexOf('\n'))).isEmpty())
        assertEquals(
            1,
            candidates(source, range = TextRange(source.indexOf(".second()"), source.length)).size,
        )
    }

    /**
     * 没有调用链时没有任何候选。
     */
    @Test
    fun testPlainCallProducesNoCandidate() {
        val source = """
            func demo(box: Box) {
                let value = box.first()
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
    ): List<CjCallChainHintCandidate> {
        val file = createPsiFile("hints", text) as CjFile
        return CjCallChainHintsCollector.candidates(file, range)
    }
}
