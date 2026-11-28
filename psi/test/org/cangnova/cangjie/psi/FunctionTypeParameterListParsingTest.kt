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
 * 元组类型 / 函数类型形参表的解析回归。
 *
 * 对齐官方 `ParserImpl::ParseTypeWithParen`（`external/cangjie_compiler/src/Parse/ParseType.cpp:186-225`）
 * 与 `ParseTypeParameterInTupleType`（`:154-184`）：
 * - 形参标签 `name:` 只是语法元数据，元组 / 函数类型节点照常构造；
 * - 全具名或全无名，否则 `parse_all_parameters_must_be_named`，但类型节点仍然保留；
 * - 具名不得重名（反引号转义写法与普通写法同名），否则 `parse_duplicate_type_parameter_name`；
 * - 单元素括号类型不允许类型形参名（`parse_only_tuple_and_func_type_allow_type_parameter_name`）；
 * - 缺右括号时整段类型退化为 `InvalidType`，不再退化成括号类型；
 * - 尾逗号合法（`ParseZeroOrMoreSepTrailing`），空形参表没有 `->` 按损坏函数类型处理。
 */
class FunctionTypeParameterListParsingTest : CjParsingTestCase(
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

    /** 合法具名元组类型：零诊断，标签不进入类型节点，两个元素类型都在。 */
    @Test
    fun testNamedTupleTypeIsAcceptedWithoutLabelsInTypeNode() {
        val file = parse("namedTuple", "type NamedTuple = (x: Int64, y: String)")

        assertErrorDescriptions(file, emptyList())
        val tupleType = assertIs<CjTupleType>(aliasTypeElement(file))
        assertEquals(2, tupleType.typeArgumentsAsTypes.size)
        assertEquals(listOf("Int64", "String"), tupleType.typeArgumentsAsTypes.map { it.text })
    }

    /** 具名与匿名混写：只报一条官方诊断，元组类型仍按两个元素构造。 */
    @Test
    fun testMixedNamedAndUnnamedTupleTypeReportsDiagnosticAndKeepsTupleType() {
        val file = parse("mixedTuple", "type MixedTuple = (Int64, x: String)")

        assertErrorDescriptions(file, listOf(MIXED_NAMED_PARAMETERS))
        val tupleType = assertIs<CjTupleType>(aliasTypeElement(file))
        assertEquals(listOf("Int64", "String"), tupleType.typeArgumentsAsTypes.map { it.text })
    }

    /** 函数类型的形参表混写：诊断不破坏形参表节点，函数类型仍保留两个形参（官方不丢 arity）。 */
    @Test
    fun testMixedNamedAndUnnamedFunctionTypeKeepsParameters() {
        val file = parse(
            "mixedFuncType",
            "type F = (Int64, x: String) -> Bool",
        )

        assertErrorDescriptions(file, listOf(MIXED_NAMED_PARAMETERS))
        val functionType = assertIs<CjFunctionType>(aliasTypeElement(file))
        assertEquals(2, functionType.parameters.size)
        assertEquals("Bool", functionType.returnTypeReference?.text)
    }

    /** 单元素括号类型的类型形参名：`parse_only_tuple_and_func_type_allow_type_parameter_name`。 */
    @Test
    fun testTypeParameterNameInParenTypeIsRejected() {
        val file = parse("parenNamed", "type P = (x: Int64)")

        assertErrorDescriptions(file, listOf(TYPE_PARAMETER_NAME_IN_PAREN_TYPE))
        assertIs<CjParenthesizedType>(aliasTypeElement(file))
    }

    /** 具名重复：普通写法与反引号转义写法是同一个类型形参名。 */
    @Test
    fun testDuplicateTypeParameterNameIsRejected() {
        val plain = parse("dupNamed", "type D = (x: Int64, x: String)")
        assertErrorDescriptions(plain, listOf("Duplicated type parameter name: 'x'"))

        val escaped = parse("dupRawNamed", "type D = (x: Int64, `x`: String)")
        assertErrorDescriptions(escaped, listOf("Duplicated type parameter name: 'x'"))
    }

    /** 尾逗号属于官方 `ParseZeroOrMoreSepTrailing` 的合法写法。 */
    @Test
    fun testTrailingCommaInTypeParameterListIsAccepted() {
        val file = parse("trailingCommaTuple", "type T = (Int64,)")

        assertErrorDescriptions(file, emptyList())
        assertIs<CjParenthesizedType>(aliasTypeElement(file))
    }

    /**
     * 类型形参表缺右括号：整段类型是官方 `InvalidType`。
     *
     * 参数类型引用存在但没有类型元素，raw CFIR 侧据此构造 error type ref
     * （`cfir/raw-cfir/raw-cfir-common` 的 `RawCfirTypeDiagnostics.INVALID_TYPE`）。
     */
    @Test
    fun testUnclosedTypeParameterListProducesInvalidType() {
        val file = parse(
            "funcTypeDefault",
            """
            func withDefault(g: (x: Int64 = 1) -> Bool): Bool {
                return g(1)
            }
            """.trimIndent(),
        )

        // junit.framework.Assert 的静态 assertNotNull 会遮蔽 kotlin.test 的同名函数，这里用 checkNotNull
        val parameter = checkNotNull(
            PsiTreeUtil.findChildrenOfType(file, CjParameter::class.java).firstOrNull { it.name == "g" },
        )
        val typeReference = checkNotNull(parameter.typeReference)
        assertTrue(typeReference.typeElement == null, "缺右括号的类型必须是官方 InvalidType")
        assertTrue(
            errorDescriptions(file).contains(UNCLOSED_DELIMITER),
            "unexpected diagnostics: ${errorDescriptions(file)}",
        )
    }

    /** 空形参表没有 `->`：官方按损坏的函数类型处理（`parse_expected_arrow_in_func_type`）。 */
    @Test
    fun testEmptyParenthesesTypeIsBrokenFunctionType() {
        val file = parse("emptyParen", "type E = ()")

        assertIs<CjFunctionType>(aliasTypeElement(file))
        assertTrue(
            errorDescriptions(file).contains(EXPECTED_ARROW_IN_FUNCTION_TYPE),
            "unexpected diagnostics: ${errorDescriptions(file)}",
        )
    }

    private fun parse(name: String, source: String): CjFile = createPsiFile(name, source) as CjFile

    private fun aliasTypeElement(file: CjFile): CjTypeElement? {
        val typeAlias = checkNotNull(PsiTreeUtil.findChildOfType(file, CjTypeAlias::class.java))
        val typeReference = checkNotNull(typeAlias.getTypeReference())
        return typeReference.typeElement
    }

    private fun errorDescriptions(file: CjFile): List<String> =
        PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java).map { it.errorDescription }

    private fun assertErrorDescriptions(file: CjFile, expected: List<String>) {
        assertEquals(expected, errorDescriptions(file))
    }

    private companion object {
        private const val MIXED_NAMED_PARAMETERS =
            "In a parameter type list, either all parameters must be named, or none of them; mixed is not allowed"
        private const val TYPE_PARAMETER_NAME_IN_PAREN_TYPE =
            "Type parameter names are only allowed in tuple and function types"
        private const val UNCLOSED_DELIMITER = "Unclosed delimiter: '('"
        private const val EXPECTED_ARROW_IN_FUNCTION_TYPE = "Expected '->' in function type"
    }
}
