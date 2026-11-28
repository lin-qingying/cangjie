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
 * 验证区间开闭性提示的**结构候选判定**。
 *
 * 开闭性完全来自 PSI（端点位置 + `..`/`..=`），不依赖 `CaSession`，因此可以在纯解析
 * 测试里把「端点位置 + 关系符文本」全部固化下来。
 */
class CjRangeValueHintsCollectorCandidatesTest : CjParsingTestCase(
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
     * `..` 是左闭右开：起点标 `≤`，终点标 `<`。
     */
    @Test
    fun testHalfOpenRangeMarksExclusiveEnd() {
        val source = """
            func demo(): Int64 {
                for (i in 0..10) {
                    return i
                }
                return 0
            }
        """.trimIndent()

        val markers = candidates(source).single().markers

        assertEquals(
            listOf(" ≤ ", " < "),
            markers.map { it.text },
        )
        assertEquals(
            listOf(source.indexOf("0..") + 1, source.indexOf("..10") + 2),
            markers.map { it.offset },
        )
    }

    /**
     * `..=` 包含终点：两端都是 `≤`。
     */
    @Test
    fun testInclusiveRangeMarksClosedEnd() {
        val source = """
            func demo(): Int64 {
                for (i in 0..=10) {
                    return i
                }
                return 0
            }
        """.trimIndent()

        assertEquals(
            listOf(" ≤ ", " ≤ "),
            candidates(source).single().markers.map { it.text },
        )
    }

    /**
     * 缺起点的切片只提示存在的那一端。
     */
    @Test
    fun testSliceWithoutStartMarksStopOnly() {
        val source = """
            func demo(values: Array<Int64>): Array<Int64> {
                return values[..10]
            }
        """.trimIndent()

        val markers = candidates(source).single().markers

        assertEquals(listOf(" < "), markers.map { it.text })
        assertEquals(source.indexOf("10"), markers.single().offset)
    }

    /**
     * 带步长的范围只标两端，步长本身不参与。
     */
    @Test
    fun testRangeWithStepMarksEndpointsOnly() {
        val source = """
            func demo(): Int64 {
                for (i in 0..10:2) {
                    return i
                }
                return 0
            }
        """.trimIndent()

        val markers = candidates(source).single().markers

        assertEquals(
            listOf(source.indexOf("0..") + 1, source.indexOf("..10") + 2),
            markers.map { it.offset },
        )
    }

    /**
     * 请求范围之外的端点不产出标记。
     */
    @Test
    fun testRangeFiltersOutMarkers() {
        val source = """
            func demo(): Int64 {
                for (i in 0..10) {
                    return i
                }
                return 0
            }
        """.trimIndent()

        // 只请求第一行（函数签名）：两个端点都在其后。
        assertTrue(candidates(source, range = TextRange(0, source.indexOf('\n'))).isEmpty())

        val bodyStart = source.indexOf("for (")
        assertEquals(
            2,
            candidates(source, range = TextRange(bodyStart, source.length)).single().markers.size,
        )
    }

    /**
     * 没有范围表达式时没有任何候选。
     */
    @Test
    fun testPlainExpressionProducesNoCandidate() {
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
    ): List<CjRangeValueHintCandidate> {
        val file = createPsiFile("hints", text) as CjFile
        return CjRangeValueHintsCollector.candidates(file, range)
    }
}
