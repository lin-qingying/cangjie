package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * `.cj.d` 声明模式下的解析行为。
 *
 * 覆盖 docs/cjd-declaration-file-support-v3.md 的 P1 验收：
 *  - A 组：**合法的无体声明**在 `.cj.d` 下不得产生任何 `PsiErrorElement`（双向验证的另一半见
 *    [DeclarationModeReverseParsingTest]）。
 *  - B 组守卫：**残缺输入**（`func }`、`init }` 等）在 `.cj.d` 下**仍然报错**，
 *    且报的是修正后的"缺标识符 / 缺参数列表"而非"函数体缺失"。
 *  - 模式载体：`CjFile.sourceKind` 必须是 `DECLARATION`（而不是靠调用点选对入口）。
 */
class DeclarationFileParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = CangJieDeclarationFileType.DECLARATION_EXTENSION,
    fileType = CangJieDeclarationFileType,
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

    private fun parse(text: String): CjFile = createPsiFile("declaration", text) as CjFile

    private fun errorMessages(file: CjFile): List<String> =
        PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).map { it.errorDescription }

    /** 断言 `.cj.d` 文本无任何解析错误。 */
    private fun assertNoParseError(text: String) {
        val file = parse(text)
        val errors = errorMessages(file)
        assertTrue(
            errors.isEmpty(),
            "`.cj.d` 中合法的无体声明不应产生 PsiErrorElement，但得到：$errors\n--- source ---\n$text",
        )
    }

    /** 断言 `.cj.d` 文本**仍然报错**，且错误文案包含 [expectedFragment]。 */
    private fun assertStillReports(text: String, expectedFragment: String) {
        val file = parse(text)
        val errors = errorMessages(file)
        assertTrue(
            errors.any { it.contains(expectedFragment, ignoreCase = true) },
            "残缺输入在 `.cj.d` 下必须仍报 `$expectedFragment`，但得到：$errors\n--- source ---\n$text",
        )
    }

    // ==================== 模式载体 ====================

    @Test
    fun testDeclarationFileIsRecognisedAsDeclarationSourceKind() {
        val file = parse("class C { func f(): Int64 }")
        assertEquals(CjSourceKind.DECLARATION, file.sourceKind)
        assertTrue(file.isDeclarationFile)
        assertEquals(CangJieDeclarationFileType, file.fileType)
    }

    // ==================== A 组：合法无体声明被抑制 ====================

    @Test
    fun testBodylessClassMemberFunctionHasNoError() {
        assertNoParseError(
            """
            class C {
                func f(): Int64
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessInterfaceMemberFunctionHasNoError() {
        assertNoParseError(
            """
            interface I {
                func f(): Int64
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessFunctionWithoutReturnTypeHasNoError() {
        assertNoParseError(
            """
            class C {
                func f()
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessTopLevelFunctionHasNoError() {
        assertNoParseError("func topLevel(): Int64")
    }

    @Test
    fun testBodylessMacroHasNoError() {
        assertNoParseError("macro package sample\npublic macro M(input: Tokens): Tokens")
    }

    @Test
    fun testBodylessMacroWithoutParametersStillReportsError() {
        assertStillReports("macro package sample\npublic macro M: Tokens", "(")
    }

    @Test
    fun testBodylessMainHasNoError() {
        assertNoParseError("main()")
    }

    @Test
    fun testBodylessMemberPropertyHasNoError() {
        assertNoParseError(
            """
            class C {
                prop p: Int64
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessPropertyAccessorsHaveNoError() {
        assertNoParseError(
            """
            class C {
                prop p: Int64 {
                    get()
                    set(v)
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessConstructorHasNoError() {
        assertNoParseError(
            """
            class C {
                init(x: Int64)
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessPrimaryConstructorHasNoError() {
        assertNoParseError(
            """
            class C {
                C(x: Int64)
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testBodylessFinalizerHasNoError() {
        assertNoParseError(
            """
            class C {
                ~init()
            }
            """.trimIndent(),
        )
    }

    // ==================== B 组守卫：残缺输入仍必须报错 ====================

    @Test
    fun testFunctionWithoutNameStillReportsIdentifierError() {
        // `func }`：连函数名都没有。修复前报的是"函数体缺失"，语义错误。
        assertStillReports("class C {\n    func\n}", "identifier")
    }

    @Test
    fun testInitWithoutParameterListStillReportsError() {
        // `init }`：缺参数列表。
        assertStillReports("class C {\n    init\n}", "(")
    }

    @Test
    fun testMainWithoutParameterListStillReportsError() {
        // `main }`：缺参数列表。
        assertStillReports("main", "(")
    }

    @Test
    fun testMacroWithoutNameStillReportsIdentifierError() {
        // `macro }`：缺宏名。
        //
        // 注意输入不能是裸 `macro }`：`macro package <name>` 是合法的文件前导形式，
        // parsePackageDirective 会把**处于文件首位**的 `macro` 当作宏包声明头吃掉
        // （随后报 "Expecting 'package' keyword"），因此到不了 parseMacro。
        // 前面必须有内容（注解 / package）才能进入宏声明分支。
        assertStillReports("@Foo\nmacro }", "identifier")
    }

    @Test
    fun testUnclosedClassStillReportsError() {
        assertTrue(errorMessages(parse("class C")).isNotEmpty(), "`class C`（缺类体）必须报错")
    }

    @Test
    fun testMalformedDeclarationDoesNotSwallowFollowingMembers() {
        // 声明模式不得让解析器"吞掉"后续成员：恢复路径必须停下。
        val file = parse(
            """
            class C {
                func
                func ok(): Int64
            }
            """.trimIndent(),
        )
        val functions = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
        assertTrue(
            functions.any { it.name == "ok" },
            "残缺输入之后的成员仍应被解析出来，实际得到函数：${functions.map { it.name }}",
        )
    }

    @Test
    fun testDeclarationFileKeepsPropertyAccessorStructure() {
        // A 组抑制不得破坏 PSI 结构：无体 getter 仍必须是 CjPropertyAccessor。
        val file = parse(
            """
            class C {
                prop p: Int64 {
                    get()
                }
            }
            """.trimIndent(),
        )
        val accessors = PsiTreeUtil.findChildrenOfType(file, CjPropertyAccessor::class.java)
        assertTrue(accessors.any { it.isGetter }, "无体 getter 应仍被解析为 CjPropertyAccessor")
    }

    @Test
    fun testDeclarationFileKeepsFunctionSignature() {
        val file = parse(
            """
            class C {
                func f(a: Int64, b: String): Int64
            }
            """.trimIndent(),
        )
        val function = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).single()
        assertEquals("f", function.name)
        assertEquals(2, function.valueParameters.size)
        assertNotNull(function.typeReference)
    }
}

/**
 * 反向验证：**同样的文本在 `.cj` 与 `.cj.d` 下行为一致的部分**，以及 `.cj` 仍必须报错的形态。
 *
 * 关键点：class / interface 体内的无体成员在**两种**文件里都不是 parse 错误
 * （官方 `CheckFuncBody` / `ParsePropDecl` 对 CLASS_BODY / INTERFACE_BODY 一律置 `ABSTRACT`，
 * 与 `parseDeclFile` 无关）；"缺体"真正的 parse 错误只在顶层 / struct / enum / extend 出现。
 * 因此本类锁定的是后者 —— 抑制逻辑一旦"漏到"这些作用域，用例会立刻失败。
 */
class DeclarationModeReverseParsingTest : CjParsingTestCase(
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

    private fun assertReportsError(text: String) {
        val file = createPsiFile("source", text) as CjFile
        assertEquals(CjSourceKind.SOURCE, file.sourceKind)
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).map { it.errorDescription }
        assertTrue(
            errors.isNotEmpty(),
            "`.cj` 中该形态必须报语法错误，但没有任何 PsiErrorElement：\n$text",
        )
    }

    /** 断言 `.cj` 文本**没有**任何 parse 错误（错误属于语义阶段，不属于 PSI 层）。 */
    private fun assertNoParseError(text: String) {
        val file = createPsiFile("source", text) as CjFile
        assertEquals(CjSourceKind.SOURCE, file.sourceKind)
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).map { it.errorDescription }
        assertTrue(
            errors.isEmpty(),
            "`.cj` 中该形态不应产生 parse 错误，但得到：$errors\n--- source ---\n$text",
        )
    }

    /** 断言 `.cj` 文本报错且错误文案包含 [expectedFragment]。 */
    private fun assertStillReports(text: String, expectedFragment: String) {
        val file = createPsiFile("source", text) as CjFile
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).map { it.errorDescription }
        assertTrue(
            errors.any { it.contains(expectedFragment, ignoreCase = true) },
            "`.cj` 下必须仍报 `$expectedFragment`，但得到：$errors\n--- source ---\n$text",
        )
    }

    @Test
    fun testBodylessClassMemberFunctionIsImplicitAbstractNotSyntaxError() {
        // 官方 `ParserImpl::CheckFuncBody`（ParseDecl.cpp:1548-1561）：class/interface 体内的无体函数
        // 被置 `ABSTRACT`，**不报缺体**；"function 'f' can not be abstract" 由语义阶段
        // （`sema_missing_func_body`）给出，本仓对应 CFIR 的 MISSING_FUNC_BODY。
        // 因此这里在 `.cj` 下也必须零 parse 错误 —— 语义诊断不属于 PSI 层。
        assertNoParseError("class C {\n    func f(): Int64\n}")
    }

    @Test
    fun testBodylessMemberPropertyIsImplicitAbstractNotSyntaxError() {
        // 官方 `ParserImpl::ParsePropDecl`（Parser.cpp:371-390）：CLASS_BODY / INTERFACE_BODY /
        // UNKNOWN_SCOPE 下无 `{}` 的属性同样被置 `ABSTRACT`，不产生 parse 诊断。
        assertNoParseError("class C {\n    prop p: Int64\n}")
    }

    @Test
    fun testBodylessPropertyAccessorReportsError() {
        assertReportsError("class C {\n    prop p: Int64 {\n        get()\n    }\n}")
    }

    @Test
    fun testBodylessConstructorReportsError() {
        assertReportsError("class C {\n    init(x: Int64)\n}")
    }

    @Test
    fun testBodylessMacroReportsError() {
        assertReportsError("macro package sample\npublic macro M(input: Tokens): Tokens")
    }

    @Test
    fun testBodylessMainReportsError() {
        assertReportsError("main()")
    }

    @Test
    fun testBodylessTopLevelFunctionReportsError() {
        assertReportsError("func topLevel(): Int64")
    }

    @Test
    fun testBodylessStructMemberFunctionReportsError() {
        // struct / enum / extend 体不产生隐式 abstract，官方走 `DiagMissingBody`。
        assertReportsError("struct S {\n    func f(): Int64\n}")
    }

    @Test
    fun testBodylessEnumMemberFunctionReportsError() {
        assertReportsError("enum E {\n    | A\n    func f(): Int64\n}")
    }

    @Test
    fun testBodylessExtendMemberFunctionReportsError() {
        assertReportsError("extend Int64 {\n    func f(): Int64\n}")
    }

    @Test
    fun testBodylessAbstractFunctionWithoutReturnTypeReportsError() {
        // 官方在同分支额外检查 `parse_abstract_func_must_have_return_type`：
        // 隐式 abstract 函数必须有返回类型。
        assertStillReports("class C {\n    func f()\n}", "return type")
    }
}
