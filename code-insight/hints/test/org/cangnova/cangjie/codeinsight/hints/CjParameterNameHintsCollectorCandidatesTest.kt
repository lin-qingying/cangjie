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
 * 验证参数名提示的**结构候选判定**。
 *
 * 与类型提示同构：候选只依赖 PSI 结构（是不是位置实参、是否有实参表达式、是否在 range 内），
 * 因此不需要 `CaSession`。形参名如何绑定由 LSP 集成测试在真实 `resolveToCall` 链上覆盖。
 */
class CjParameterNameHintsCollectorCandidatesTest : CjParsingTestCase(
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
     * 两个位置实参各产出一个提示位置，锚点落在实参表达式起点。
     */
    @Test
    fun testPositionalArgumentsProduceOnePositionEach() {
        val source = """
            func use(): Int64 {
                return greet(1, 2)
            }
        """.trimIndent()

        val candidate = candidates(source).single()

        assertEquals(
            listOf(source.indexOf("1"), source.indexOf("2")),
            candidate.positions.map { it.offset },
        )
    }

    /**
     * 命名实参已经在源码里写明了形参名，不重复提示。
     */
    @Test
    fun testNamedArgumentProducesNoPosition() {
        val source = """
            func use(): Int64 {
                return greet(value: 1)
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 混合实参只为位置实参产出提示；全部命名时整个调用不出候选。
     */
    @Test
    fun testOnlyPositionalArgumentsAreHintedInMixedCall() {
        val mixed = """
            func use(): Int64 {
                return greet(1, times: 2)
            }
        """.trimIndent()

        val candidate = candidates(mixed).single()
        assertEquals(listOf(mixed.indexOf("1")), candidate.positions.map { it.offset })
    }

    /**
     * 无实参调用不产出候选。
     */
    @Test
    fun testCallWithoutArgumentsProducesNoCandidate() {
        val source = """
            func use(): Int64 {
                return greet()
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 每个调用各出一个候选：嵌套调用分别提示自己的实参。
     */
    @Test
    fun testNestedCallsProduceOneCandidateEach() {
        val source = """
            func use(): Int64 {
                return greet(1, wrap(2))
            }
        """.trimIndent()

        val candidates = candidates(source)

        // 外层 `greet(1, wrap(2))` 有 2 个位置实参，内层 `wrap(2)` 有 1 个：共 3 个位置。
        assertEquals(2, candidates.size)
        assertEquals(3, candidates.sumOf { it.positions.size })
    }

    /**
     * range 之外的实参位置被裁掉；全部越界时整个调用不出候选。
     */
    @Test
    fun testRangeFiltersOutPositions() {
        val source = """
            func use(): Int64 {
                return greet(1, 2)
            }
        """.trimIndent()

        val firstLineEnd = source.indexOf('\n')
        assertTrue(candidates(source, range = TextRange(0, firstLineEnd)).isEmpty())

        val secondLineStart = firstLineEnd + 1
        assertEquals(
            2,
            candidates(source, range = TextRange(secondLineStart, source.length)).single().positions.size,
        )
    }

    /**
     * 解析源码并挑选候选。
     */
    private fun candidates(
        text: String,
        range: TextRange? = null,
    ): List<CjParameterNameHintCandidate> {
        val file = createPsiFile("hints", text) as CjFile
        return CjParameterNameHintsCollector.candidates(file, range)
    }
}
