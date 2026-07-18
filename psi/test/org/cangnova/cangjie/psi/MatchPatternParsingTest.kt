package org.cangnova.cangjie.psi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 表示 `MatchPatternParsingTest`，承载PSI 测试中的语法节点、索引桩或辅助模型。
 */
class MatchPatternParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {

    /**
     * 提供 `setUpFixture` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 提供 `tearDownFixture` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 提供 `testBareIdentifierPatternsStayDeferred` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @Test
    fun testBareIdentifierPatternsStayDeferred() {
        val file = createPsiFile(
            "matchVarOrEnumPattern",
            """
            enum Option<T> {
                Some(T)
                None
            }

            func sample(value: Option<Int64>): Int64 {
                return match (value) {
                    case None => 0
                    case Some(x) => x
                    case fallback => 1
                }
            }
            """.trimIndent(),
        ) as CjFile

        val matchExpression = PsiTreeUtil.findChildOfType(file, CjMatchExpression::class.java)
            ?: error("match expression not found")
        val entries = matchExpression.entries.toList()

        assertIs<CjVarOrEnumPattern>(entries[0].conditions.single())
        assertIs<CjEnumPattern>(entries[1].conditions.single())
        assertIs<CjVarOrEnumPattern>(entries[2].conditions.single())
    }

    /**
     * 提供 `testNegativeLiteralPatternStaysConstantPattern` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @Test
    fun testNegativeLiteralPatternStaysConstantPattern() {
        val file = createPsiFile(
            "matchNegativeConstPattern",
            """
            func sample(value: Int64): Int64 {
                return match (value) {
                    case -1 => 0
                    case 1 => 1
                }
            }
            """.trimIndent(),
        ) as CjFile

        val matchExpression = PsiTreeUtil.findChildOfType(file, CjMatchExpression::class.java)
            ?: error("match expression not found")
        val entries = matchExpression.entries.toList()

        assertIs<CjConstantPattern>(entries[0].conditions.single())
        assertIs<CjConstantPattern>(entries[1].conditions.single())
    }

    /**
     * 类型模式的绑定名和冒号后的基本类型必须分离；`Float64` 不能被误当成绑定引用或表达式。
     */
    @Test
    fun testTypePatternKeepsBindingAndBasicTypeReferenceSeparate() {
        val file = createPsiFile(
            "matchTypePattern",
            """
            func sample(value: Any): Int64 {
                return match (value) {
                    case j: Float64 => 1
                    case _ => 0
                }
            }
            """.trimIndent(),
        ) as CjFile

        val matchExpression = PsiTreeUtil.findChildOfType(file, CjMatchExpression::class.java)
            ?: error("match expression not found")
        val typePattern = assertIs<CjTypePattern>(matchExpression.entries.first().conditions.single())

        assertEquals("j", typePattern.name)
        assertEquals("Float64", typePattern.typeReference?.text)
    }

    /**
     * case body 的语句集合只能暴露当前 CASE_BLOCK 的直接语句，不能泄漏嵌套 match 的 pattern。
     */
    @Test
    fun testCaseBlockStatementsDoNotIncludeNestedPatterns() {
        val file = createPsiFile(
            "nestedMatchCaseBlock",
            """
            var a = b()
            let c = true
            var d = e

            enum f {
                e
            }

            class b {
                public let g: Int8 = 5
            }

            func h(i!: Int8 = match {
                    case c => match (d) {
                        case j: Float64 => a
                    }
                    case _ => a
                }.g) {}
            """.trimIndent(),
        ) as CjFile

        val matchExpressions = PsiTreeUtil.findChildrenOfType(file, CjMatchExpression::class.java).toList()
        val outerMatch = matchExpressions.first { it.subjectExpression == null }
        val innerMatch = matchExpressions.first { it.subjectExpression?.text == "d" }

        val outerStatements = outerMatch.entries.first().expression?.statements.orEmpty()
        val innerStatements = innerMatch.entries.first().expression?.statements.orEmpty()

        assertEquals(1, outerStatements.size)
        assertIs<CjMatchExpression>(outerStatements.single())
        assertEquals(1, innerStatements.size)
        assertEquals("a", innerStatements.single().text)
    }
}
