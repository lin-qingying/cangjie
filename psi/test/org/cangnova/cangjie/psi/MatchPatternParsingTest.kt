package org.cangnova.cangjie.psi

import kotlin.test.Test
import kotlin.test.assertIs
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class MatchPatternParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

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
