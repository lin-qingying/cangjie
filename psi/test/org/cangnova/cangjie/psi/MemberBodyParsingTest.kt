package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 成员缺体判定的 parser 行为回归。
 *
 * 期望值来自官方 cjc 1.0.5 的 JSON 诊断（`parse_missing_body` /
 * `parse_abstract_func_must_have_return_type` / `parse_expected_character`），
 * 判定本身在 `CjMemberBodyPolicy`，本类只验证适配器执行结果。
 */
class MemberBodyParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = CangJieFileType.EXTENSION,
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

    private fun parse(source: String): CjFile = createPsiFile("memberBody", source) as CjFile

    private fun errors(source: String): List<String> =
        PsiTreeUtil.findChildrenOfType(parse(source), PsiErrorElement::class.java).map { it.errorDescription }

    private fun assertNoErrorContaining(source: String, fragment: String) {
        val errors = errors(source)
        assertTrue(
            errors.none { it.contains(fragment, ignoreCase = true) },
            "不应出现含 `$fragment` 的 parse 错误，实际：$errors\n--- source ---\n$source",
        )
    }

    private fun assertReports(source: String, fragment: String) {
        val errors = errors(source)
        assertTrue(
            errors.any { it.contains(fragment, ignoreCase = true) },
            "必须报含 `$fragment` 的 parse 错误，实际：$errors\n--- source ---\n$source",
        )
    }

    /** `@Intrinsic` 顶层无体函数：官方 isMember 显式排除 INTRINSIC，不报缺体。 */
    @Test
    fun testIntrinsicTopLevelFunctionDoesNotReportMissingBody() {
        val errors = errors("@Intrinsic\nfunc f(): Int64")
        assertTrue(errors.isEmpty(), "Intrinsic 顶层函数不应产生缺体 parse 错误，实际：$errors")
    }

    /** `@Intrinsic` class 成员无返回类型：既不报缺体，也不报 abstract 缺返回类型。 */
    @Test
    fun testIntrinsicMemberFunctionDoesNotReportAbstractReturnType() {
        val errors = errors(
            """
            class C {
                @Intrinsic
                func f()
            }
            """.trimIndent()
        )
        assertTrue(errors.isEmpty(), "Intrinsic 成员不应产生缺体相关 parse 错误，实际：$errors")
    }

    /** 缺参数列表的函数：官方 HAS_BROKEN 只门诊断，不叠报缺体（class 仍置 ABSTRACT）。 */
    @Test
    fun testBrokenClassFunctionSignatureDoesNotReportMissingBody() {
        val source = "class C {\n    func f : Int64\n}"
        assertReports(source, "(")
        assertNoErrorContaining(source, "body")
        assertNoErrorContaining(source, "return type")
    }

    /** 缺参数列表的 struct 函数：HAS_BROKEN 抑制 DiagMissingBody。 */
    @Test
    fun testBrokenStructFunctionSignatureDoesNotReportMissingBody() {
        val source = "struct S {\n    func f : Int64\n}"
        assertReports(source, "(")
        assertNoErrorContaining(source, "body")
    }

    /** 缺冒号的属性：官方只报类型/冒号诊断，不再叠报缺体。 */
    @Test
    fun testBrokenPropertySignatureDoesNotReportMissingBody() {
        val source = "struct S {\n    prop p Int64\n}"
        assertReports(source, ":")
        assertNoErrorContaining(source, "body")
    }

    /** 缺类型名的属性：同样不再叠报缺体。 */
    @Test
    fun testPropertyWithoutTypeDoesNotReportMissingBody() {
        val source = "struct S {\n    prop p: \n}"
        assertNoErrorContaining(source, "body")
    }

    /** 合法无体 struct 属性仍必须报缺体，证明抑制没有越界。 */
    @Test
    fun testValidStructPropertyStillReportsMissingBody() {
        assertReports("struct S {\n    prop p: Int64\n}", "body")
    }
}
