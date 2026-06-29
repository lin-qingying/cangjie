package org.cangnova.cangjie.psi

import kotlin.test.Test
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
}
