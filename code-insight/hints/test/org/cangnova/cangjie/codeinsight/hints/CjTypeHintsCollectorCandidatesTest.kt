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
 * 验证类型提示的**结构候选判定**。
 *
 * 这一层刻意不涉及任何语义：候选挑选只依赖 PSI 结构（有没有显式类型、有没有初始化器、
 * 名字是否可锚定、是否落在 range 内），因此不需要 `CaSession`。
 * 类型解析与渲染的正确性由 LSP 集成测试在真实 `analyzeSnapshot` 链上覆盖。
 */
class CjTypeHintsCollectorCandidatesTest : CjParsingTestCase(
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
     * 无类型标注的局部变量是候选，锚点落在变量名末尾。
     */
    @Test
    fun testLocalVariableWithoutTypeIsCandidate() {
        val source = """
            func demo(): Int64 {
                let answer = 42
                return answer
            }
        """.trimIndent()

        val candidates = candidates(source)

        val candidate = candidates.single()
        assertEquals(source.indexOf("answer") + "answer".length, candidate.offset)
    }

    /**
     * 已显式标注类型的变量不产出候选。
     */
    @Test
    fun testExplicitTypeIsNotCandidate() {
        val source = """
            func demo(): Int64 {
                let answer: Int64 = 42
                return answer
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 解构模式没有单一名字节点，不产出候选。
     *
     * 解构的每个绑定各有自己的类型，而绑定自身的符号在 dangling 会话里取不到，
     * 与其给出整条声明的 typle 类型，不如不提示。
     */
    @Test
    fun testDestructuringPatternIsNotCandidate() {
        val source = """
            func demo(): Int64 {
                let (left, right) = (1, 2)
                return left + right
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 简单绑定（非解构）不会被当成解构绑定重复产出候选。
     */
    @Test
    fun testSimpleBindingIsNotDuplicatedAsDestructuring() {
        val source = """
            func demo(): Int64 {
                let answer = 42
                return answer
            }
        """.trimIndent()

        val candidates = candidates(source)

        assertEquals(1, candidates.size)
        assertEquals(CangJieInlayHintTarget.LOCAL_VARIABLE_TYPE, candidates.single().target)
    }

    /**
     * 顶层变量与成员属性分别落到 `VARIABLE_TYPE` / `PROPERTY_TYPE`。
     */
    @Test
    fun testTopLevelAndPropertyTargetsAreDistinguished() {
        val source = """
            let topLevel = 1

            class Holder {
                let field = 2
            }
        """.trimIndent()

        val targets = candidates(source).map { it.target }.toSet()

        assertEquals(
            setOf(CangJieInlayHintTarget.VARIABLE_TYPE, CangJieInlayHintTarget.PROPERTY_TYPE),
            targets,
        )
    }

    /**
     * 未标注返回类型的函数是候选，锚点落在参数列表右括号之后。
     */
    @Test
    fun testFunctionWithoutReturnTypeIsCandidate() {
        val source = """
            func inferred(a: Int64) {
                return a
            }
        """.trimIndent()

        val candidate = candidates(source).single()
        assertEquals(source.indexOf(")") + 1, candidate.offset)
        assertEquals(CangJieInlayHintTarget.FUNCTION_RETURN_TYPE, candidate.target)
    }

    /**
     * 已标注返回类型的函数不产出候选。
     */
    @Test
    fun testFunctionWithReturnTypeIsNotCandidate() {
        val source = """
            func typed(a: Int64): Int64 {
                return a
            }
        """.trimIndent()

        assertTrue(candidates(source).isEmpty())
    }

    /**
     * 三个开关全部关闭时不产出任何候选。
     */
    @Test
    fun testDisabledOptionsProduceNoCandidate() {
        val source = """
            func inferred(a: Int64) {
                let answer = a
                let annotated: Int64 = a
                return answer
            }
        """.trimIndent()

        val options = CangJieInlayHintOptions(
            showPropertyTypes = false,
            showLocalVariableTypes = false,
            showFunctionReturnTypes = false,
            showParameterNames = false,
        )

        assertTrue(candidates(source, options = options).isEmpty())
        // 同一份源码在默认开关下必须产出候选，否则上面的空结果说明不了开关确实生效。
        assertEquals(2, candidates(source).size)
    }

    /**
     * 只关闭局部变量开关时，仅剩函数返回类型候选。
     */
    @Test
    fun testLocalVariableSwitchOnlyGatesLocalVariables() {
        val source = """
            func inferred(a: Int64) {
                let answer = a
                return answer
            }
        """.trimIndent()

        val candidates = candidates(source, options = CangJieInlayHintOptions(showLocalVariableTypes = false))

        assertEquals(1, candidates.size)
        assertTrue(candidates.single() is CjTypeHintCandidate.Function)
    }

    /**
     * range 之外的候选被裁掉。
     */
    @Test
    fun testRangeFiltersOutCandidatesOutside() {
        val source = """
            func inferred(a: Int64) {
                let first = a
                let second = a
                return first + second
            }
        """.trimIndent()

        val firstLineEnd = source.indexOf('\n')
        val range = TextRange(0, firstLineEnd)

        val candidates = candidates(source, range = range)

        // 首行只含函数声明：`let` 行都在 range 之外。
        assertEquals(1, candidates.size)
        assertTrue(candidates.single() is CjTypeHintCandidate.Function)
    }

    /**
     * 解析源码并挑选候选。
     */
    private fun candidates(
        text: String,
        range: TextRange? = null,
        options: CangJieInlayHintOptions = CangJieInlayHintOptions(),
    ): List<CjTypeHintCandidate> {
        val file = createPsiFile("hints", text) as CjFile
        return CjTypeHintsCollector.candidates(file, range, options)
    }
}
