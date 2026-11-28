package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * 宏调用 `@Name(...)` 的 Tokens 输入必须覆盖全部运算符与字面量 token，
 * 否则输入会在 `<<`、字节字面量等位置提前结束，产生 "Expecting ')'" 与后续语句误解析。
 */
class MacroInputParsingTest : CjParsingTestCase(
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

    /** 移位运算符 `<<` 属于宏输入，不能被当作表达式边界。 */
    @Test
    fun testShiftOperatorStaysInsideMacroInput() {
        val file = createPsiFile("macroShift", SHIFT_SOURCE) as CjFile

        assertNoParseErrors(file)
        val macro = requireNotNull(PsiTreeUtil.findChildOfType(file, CjMacroExpression::class.java))
        assertTrue(
            macro.input?.text.orEmpty().contains("<< 1"),
            "macro input must keep the shift operator, actual: ${macro.input?.text}",
        )
    }

    /** 字节字面量 `b'0'` 属于宏输入，不能截断 `@assert(...)`。 */
    @Test
    fun testCharacterByteLiteralStaysInsideMacroInput() {
        val file = createPsiFile("macroByteLiteral", BYTE_LITERAL_SOURCE) as CjFile

        assertNoParseErrors(file)
        val macro = requireNotNull(PsiTreeUtil.findChildOfType(file, CjMacroExpression::class.java))
        assertTrue(
            macro.input?.text.orEmpty().contains("b'0'"),
            "macro input must keep the byte literal, actual: ${macro.input?.text}",
        )
    }

    /** 完整的 `@NoFormat(...)` 多行表达式。 */
    @Test
    fun testMultiLineNoFormatExpressionParses() {
        val file = createPsiFile("macroNoFormat", NO_FORMAT_SOURCE) as CjFile

        assertNoParseErrors(file)
        val macro = requireNotNull(PsiTreeUtil.findChildOfType(file, CjMacroExpression::class.java))
        assertTrue(macro.input?.text.orEmpty().contains("zone.hashCode() << 11"))
    }

    /** 完整的 `@assert(...)` 条件表达式。 */
    @Test
    fun testAssertWithByteRangesParses() {
        val file = createPsiFile("macroAssert", ASSERT_SOURCE) as CjFile

        assertNoParseErrors(file)
        val macro = requireNotNull(PsiTreeUtil.findChildOfType(file, CjMacroExpression::class.java))
        assertTrue(macro.input?.text.orEmpty().contains("replacement[0] >= b'0'"))
    }

    private fun assertNoParseErrors(file: CjFile) {
        val errors = PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java)
            .map { "${it.errorDescription} @${it.textOffset}" }
        assertTrue(errors.isEmpty(), "unexpected PSI errors: $errors")
    }

    private companion object {
        private val SHIFT_SOURCE = """
            func f(x: Int64): Int64 {
                return @NoFormat(x << 1)
            }
        """.trimIndent()

        private val BYTE_LITERAL_SOURCE = """
            func f(b: UInt8) {
                @assert(b >= b'0' && b <= b'9')
            }
        """.trimIndent()

        private val NO_FORMAT_SOURCE = """
            func hashCodeProbe(lenient: Bool, firstDayOfWeek: Int64, minimalDaysInFirstWeek: Int64,
                repeatedWallTime: Bool, skippedWallTime: Bool, zone: String): Int64 {
                return @NoFormat(
                    if (lenient) {1} else {0} |
                    (Int64(firstDayOfWeek) << 1) |
                    (Int64(minimalDaysInFirstWeek) << 4) |
                    (Int64(repeatedWallTime) << 7) |
                    (Int64(skippedWallTime) << 9) |
                    (zone.hashCode() << 11))
            }
        """.trimIndent()

        private val ASSERT_SOURCE = """
            func assertProbe(replacement: String) {
                @assert(replacement.length() != 4 || (replacement[0] >= b'0' && replacement[0] <= b'9'))
            }
        """.trimIndent()
    }
}
