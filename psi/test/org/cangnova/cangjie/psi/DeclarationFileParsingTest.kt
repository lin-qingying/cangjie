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
 * 反向验证：**同样的文本在 `.cj` 下必须报错**。
 *
 * 这是 P1 的关键防线 —— 抑制逻辑一旦"漏到"普通源文件上，本类的用例会立刻失败。
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
            "`.cj` 中不允许无体声明，必须报错，但没有任何 PsiErrorElement：\n$text",
        )
    }

    @Test
    fun testBodylessClassMemberFunctionReportsError() {
        assertReportsError("class C {\n    func f(): Int64\n}")
    }

    @Test
    fun testBodylessMemberPropertyReportsError() {
        assertReportsError("class C {\n    prop p: Int64\n}")
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
}
