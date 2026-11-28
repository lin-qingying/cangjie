package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 换行续接的 PSI 解析回归：对齐官方 `ParserImpl::SeeingExprOperator`
 * 与 if-let 条件里的 `let` 表达式语义。
 */
class NewlineContinuationParsingTest : CjParsingTestCase(
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

    /** `&&` 后换行、下一行以 `let` 开头的 if-let 链仍属于同一个条件。 */
    @Test
    fun testIfLetChainContinuesAcrossNewlineBeforeLet() {
        val file = createPsiFile("ifLetNewlineBeforeLet", IF_LET_SOURCE) as CjFile

        assertNoParseErrors(file)
        assertEquals(
            4,
            PsiTreeUtil.findChildrenOfType(file, CjLetExpression::class.java).size,
            "condition should contain 4 let-pattern expressions",
        )
        assertEquals(2, PsiTreeUtil.findChildrenOfType(file, CjIfExpression::class.java).size)
    }

    /** 行首 `+` 继续上一行的字符串拼接。 */
    @Test
    fun testStringConcatenationContinuesAcrossNewlineBeforePlus() {
        val file = createPsiFile("concatLeadingPlus", CONCAT_SOURCE) as CjFile

        assertNoParseErrors(file)
        val variable = PsiTreeUtil.findChildOfType(file, CjVariable::class.java)
        assertIs<CjBinaryExpression>(variable?.initializer)
        assertEquals(2, PsiTreeUtil.findChildrenOfType(file, CjBinaryExpression::class.java).size)
        assertEquals(
            listOf("+", "+"),
            PsiTreeUtil.findChildrenOfType(file, CjOperationReferenceExpression::class.java).map { it.text },
        )
    }

    /** 官方规则里换行后的 `-` 不续接二元表达式，而是开启新的 unary 表达式语句。 */
    @Test
    fun testNewlineBeforeMinusStartsNewStatement() {
        val file = createPsiFile("minusNewline", MINUS_SOURCE) as CjFile

        assertNoParseErrors(file)
        val variable = PsiTreeUtil.findChildOfType(file, CjVariable::class.java)
        assertEquals("a", variable?.initializer?.text)

        val function = requireNotNull(PsiTreeUtil.findChildOfType(file, CjFunction::class.java))
        val statements = function.bodyBlockExpression?.statements.orEmpty()
        assertEquals(3, statements.size)
        assertEquals("-3", statements[1].text)
    }

    private fun assertNoParseErrors(file: CjFile) {
        val errors = PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java)
            .map { "${it.errorDescription} @${it.textOffset}" }
        assertTrue(errors.isEmpty(), "unexpected PSI errors: $errors")
    }

    private companion object {
        private val IF_LET_SOURCE = """
            enum PatternSignType {
                POS | NEG
            }

            func update(signType: PatternSignType, prefix: Option<String>, suffix: Option<String>) {
                var posPrefix: Option<String> = None
                var posSuffix: Option<String> = None
                for (i in 0..1) {
                    if (signType == PatternSignType.POS) {
                        posPrefix = prefix
                        posSuffix = suffix
                    } else if (let Some(pfx) <- prefix && let Some(ppfx) <- posPrefix && pfx == ppfx && let Some(sfx) <- suffix &&
                        let Some(psfx) <- posSuffix && sfx == psfx) {
                        continue
                    }
                }
            }
        """.trimIndent()

        private val CONCAT_SOURCE = """
            let description = "one: "
                + "many: "
                + "other: "
        """.trimIndent()

        private val MINUS_SOURCE = """
            func f(a: Int64): Int64 {
                let x = a
                -3
                x
            }
        """.trimIndent()
    }
}
