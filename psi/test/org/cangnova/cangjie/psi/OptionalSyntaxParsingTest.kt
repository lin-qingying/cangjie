package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class OptionalSyntaxParsingTest : CjParsingTestCase(
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
    fun testOptionTypeParsesAsNestedOptionalType() {
        val file = createPsiFile("optionalType", "func sample(value: ??Int64) {}") as CjFile
        val optionTypes = PsiTreeUtil.findChildrenOfType(file, CjOptionType::class.java)
        assertEquals(2, optionTypes.size)
    }

    @Test
    fun testOptionalChainBuildsOptionalPsiNodes() {
        val file = createPsiFile(
            "optionalChain",
            """
            func sample() {
                x?.b?[1]?()
            }
            """.trimIndent(),
        ) as CjFile

        assertNotNull(PsiTreeUtil.findChildOfType(file, CjOptionalExpression::class.java))
        assertNotNull(PsiTreeUtil.findChildOfType(file, CjOptionalChainExpression::class.java))
    }

    @Test
    fun testInvalidStandaloneQuestReportsError() {
        val file = createPsiFile(
            "invalidQuest",
            """
            func sample() {
                x?b
            }
            """.trimIndent(),
        ) as CjFile

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun testAbstractFunctionWithoutBodyDoesNotProduceParseError() {
        val file = createPsiFile(
            "abstractFunctionWithoutBody",
            """
            class AstNode {
                abstract func dump(indent: UInt16): std.core.String
            }
            """.trimIndent(),
        ) as CjFile

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(
            errors.isEmpty(),
            "abstract function without body should parse without PsiErrorElement, but got: ${
                errors.joinToString { it.errorDescription }
            }",
        )
    }
}
