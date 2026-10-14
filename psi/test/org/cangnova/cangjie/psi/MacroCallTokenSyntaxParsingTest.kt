package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 宏调用的括号输入和属性输入分别按官方 Tokens 规则解析。
 * 依据仓颉《非属性宏》《属性宏》说明及 external/cangjie_compiler/src/Parse/ParseMacro.cpp。
 */
class MacroCallTokenSyntaxParsingTest : CjParsingTestCase(
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

    /** 普通宏输入可空；只校验小括号，美元符号和方括号本身都是合法 token。 */
    @Test
    fun plainMacroInputAcceptsEmptyDollarAndUnpairedSquareTokens() {
        val file = parseWithoutErrors(
            "plainMacroTokens",
            """
            func sample() {
                let empty = @Empty()
                let dollar = @Dollar($)
                let openSquare = @OpenSquare([)
                let squarePair = @SquarePair([])
                let nested = @Nested((value))
            }
            """.trimIndent(),
        )

        val macros = macroCalls(file)
        assertEquals(5, macros.size)
        assertTrue(macros.all { it.attr == null })
        assertTrue(macros.all { it.input != null && !it.hasDeclarationInput })
    }

    /** 普通宏输入转义括号后，转义括号不参与外层小括号配对。 */
    @Test
    fun plainMacroInputAcceptsEscapedParenthesesAndAtSign() {
        val file = parseWithoutErrors(
            "plainMacroEscapes",
            """
            func sample() {
                let leftParen = @LeftParen(\()
                let rightParen = @RightParen(\))
                let atSign = @AtSign(\@)
            }
            """.trimIndent(),
        )

        assertEquals(3, macroCalls(file).size)
    }

    /** 全局词法识别的宏转义在普通 Cangjie 表达式中仍不是合法语法。 */
    @Test
    fun macroEscapesAreRejectedOutsideMacroInput() {
        assertParseErrors(
            "macroEscapesOutsideMacroInput",
            """
            func sample() {
                let escapedParen = \(value)
                let escapedAt = \@name
            }
            """.trimIndent(),
        )
    }

    /** BAD_CHARACTER 不能因为宏 token 集扩展而被普通 quote 参数静默跳过。 */
    @Test
    fun quoteParametersRejectBadCharacters() {
        assertParseErrors(
            "quoteBadCharacter",
            "func sample() { let value = quote(§) }",
        )
    }

    /** 保留官方解析器用例中的 quote 括号转义语法。 */
    @Test
    fun quoteParametersAcceptEscapedParentheses() {
        parseWithoutErrors(
            "quoteEscapedParentheses",
            """
            func sample() {
                quote(1 + 2 + 3 \( \))
            }
            """.trimIndent(),
        )
    }

    /** 属性输入只要求中括号配对，允许空属性、括号 token 及对应转义。 */
    @Test
    fun attributeMacroInputAcceptsEmptyTokensBalancedBracketsAndLegalEscapes() {
        val file = parseWithoutErrors(
            "attributeMacroTokens",
            """
            func sample() {
                let empty = @Empty[]()
                let parens = @Parens[()]()
                let nested = @Nested[([value])]()
                let leftBracket = @LeftBracket[\[]()
                let atSign = @AtSign[\@]()
            }
            """.trimIndent(),
        )

        val macros = macroCalls(file)
        assertEquals(5, macros.size)
        assertTrue(macros.all { it.attr != null })
        assertTrue(macros.all { it.input != null && !it.hasDeclarationInput })
    }

    /** 普通输入的不配对小括号、裸非法 token 和不适用的方括号转义都应报错。 */
    @Test
    fun plainMacroInputRejectsUnbalancedParenthesesAndIllegalTokens() {
        assertParseErrors(
            "unbalancedPlainMacroInput",
            "func sample() { let value = @M((value) }",
        )
        assertParseErrors(
            "illegalPlainMacroTokens",
            """
            func sample() {
                let hash = @Hash(#)
                let backtick = @Backtick(`)
                let backslash = @Backslash(\ )
                let unescapedAt = @At(@)
                let squareEscape = @SquareEscape(\[)
            }
            """.trimIndent(),
        )
    }

    /** 属性输入的不配对中括号、非法 token 和不适用的小括号转义都应报错。 */
    @Test
    fun attributeMacroInputRejectsUnbalancedBracketsAndIllegalTokens() {
        assertParseErrors(
            "unbalancedAttributeMacroInput",
            "func sample() { let value = @M[[]() }",
        )
        assertParseErrors(
            "illegalAttributeMacroTokens",
            """
            func sample() {
                let hash = @Hash[#]()
                let backtick = @Backtick[`]()
                let atSign = @AtSign[@]()
                let backslash = @Backslash[\ ]()
                let parenEscape = @ParenEscape[\(]()
            }
            """.trimIndent(),
        )
    }

    /** 宏是否有一或两个 Tokens 形参不能由 PSI parser fixture 判断。 */
    @Test
    fun parserPreservesAttributeShapeWithoutClaimingMacroDeclarationArity() {
        val file = parseWithoutErrors(
            "macroAttributeShape",
            """
            func sample() {
                let plain = @Plain(input)
                let attributed = @Attributed[attribute](input)
            }
            """.trimIndent(),
        )

        val macros = macroCalls(file).associateBy { it.shortName?.asString() }
        val plain = macros.getValue("Plain")
        val attributed = macros.getValue("Attributed")
        assertFalse(plain.hasDeclarationInput)
        assertNotNull(plain.input)
        assertNull(plain.attr)
        assertFalse(attributed.hasDeclarationInput)
        assertNotNull(attributed.input)
        assertNotNull(attributed.attr)
    }

    private fun parseWithoutErrors(name: String, source: String): CjFile {
        val file = createPsiFile(name, source) as CjFile
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), errors.joinToString { it.errorDescription })
        return file
    }

    private fun assertParseErrors(name: String, source: String) {
        val file = createPsiFile(name, source) as CjFile
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isNotEmpty(), "Expected a parser error for:\n$source")
    }

    private fun macroCalls(file: CjFile): List<CjMacroExpression> =
        PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java).toList()
}
