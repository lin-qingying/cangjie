package org.cangnova.cangjie.psi

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** 名称依赖真实声明所有者；非法嵌套的 PSI 恢复不能赋予局部声明包级身份。 */
class NamedDeclarationFqNameTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun nominalMembersIncludeTheirActualOwnerInQualifiedNames() {
        for (kind in listOf("class", "struct", "interface", "enum")) {
            val enumCase = if (kind == "enum") "Case" else ""
            val function = if (kind == "interface") "func member(): Unit" else "func member(): Unit {}"
            val file = parse(
                "nominalMember$kind",
                """
                package fqnames
                $kind Owner {
                    $enumCase
                    $function
                }
                """.trimIndent(),
            )
            assertEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics())
            val owner = file.declarations.filterIsInstance<CjTypeStatement>().single()
            val body = kotlin.test.assertNotNull(owner.body)
            val member = body.declarations.filterIsInstance<CjNamedFunction>().single()
            assertSame(body, member.parent)
            assertQualifiedName(owner, "fqnames", "fqnames.Owner")
            assertQualifiedName(member, "fqnames.Owner", "fqnames.Owner.member")
        }
    }

    /** 复用 NestedNominalParsingTest 的官方 scope 矩阵：保留错误及实际类型层级。 */
    @Test
    fun rejectedNestedNominalsRetainTheirRealOwnerForPsiNames() {
        val cases = listOf("class", "struct", "interface", "enum").map { it to "class" } +
            listOf("struct", "interface", "enum").map { "class" to it }
        for ((ownerKind, nestedKind) in cases) {
            val ownerCase = if (ownerKind == "enum") "OuterCase" else ""
            val nestedCase = if (nestedKind == "enum") "InnerCase" else ""
            val member = if (nestedKind == "interface") "func member(): Unit" else "func member(): Unit {}"
            val text = """
                package fqnames
                $ownerKind Owner {
                    $ownerCase
                    $nestedKind Nested {
                        $nestedCase
                        $member
                    }
                }
            """.trimIndent()
            val file = parse("nested${ownerKind}${nestedKind}", text)
            assertScopeError(file, text, "$nestedKind Nested", nestedKind, ownerKind)
            val owner = file.declarations.filterIsInstance<CjTypeStatement>().single()
            val ownerBody = kotlin.test.assertNotNull(owner.body)
            val nested = ownerBody.declarations.filterIsInstance<CjTypeStatement>().single()
            val nestedBody = kotlin.test.assertNotNull(nested.body)
            val function = nestedBody.declarations.filterIsInstance<CjNamedFunction>().single()
            assertSame(ownerBody, nested.parent)
            assertSame(nestedBody, function.parent)
            assertQualifiedName(nested, "fqnames.Owner", "fqnames.Owner.Nested")
            assertQualifiedName(function, "fqnames.Owner.Nested", "fqnames.Owner.Nested.member")
        }
    }

    /** authority-function-local-primary 已确认局部 class 的 scope 错误；其成员也不能逃逸为全限定名。 */
    @Test
    fun localFunctionsAndRecoveredLocalTypesHaveNoQualifiedName() {
        val text = """
            package fqnames
            func host(): Unit {
                func local(): Unit {}
                class Local {
                    Local() {}
                    func member(): Unit {}
                }
            }
        """.trimIndent()
        val file = parse("localDeclarations", text)
        assertScopeError(file, text, "class Local", "class", "func")
        val host = file.declarations.filterIsInstance<CjNamedFunction>().single()
        assertQualifiedName(host, "fqnames", "fqnames.host")
        val functions = PsiTreeUtil.findChildrenOfType(host, CjNamedFunction::class.java)
        assertEquals(setOf("local", "member"), functions.map { it.name }.toSet())
        val local = PsiTreeUtil.findChildrenOfType(host, CjClass::class.java).single()
        assertEquals("Local", local.name)
        for (declaration in listOf<CjNamedDeclaration>(local) + functions) {
            assertNull(CjNamedDeclarationUtil.getParentFqName(declaration))
            assertNull(CjNamedDeclarationUtil.getFQName(declaration))
            assertNull(declaration.fqName)
        }
    }

    @Test
    fun foreignBodyDoesNotIntroduceAnAdditionalNameScope() {
        val file = parse(
            "foreignDeclarations",
            """
            package fqnames
            foreign {
                func nativeCall(): Unit
            }
            foreign func directNative(): Unit
            func ordinary(): Unit {}
            """.trimIndent(),
        )
        assertEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics())
        val functions = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
        assertEquals(setOf("nativeCall", "directNative", "ordinary"), functions.map { it.name }.toSet())
        val foreign = PsiTreeUtil.getChildrenOfTypeAsList(file, CjForeignDirective::class.java).single()
        val body = kotlin.test.assertNotNull(foreign.body)
        assertSame(body, functions.single { it.name == "nativeCall" }.parent)
        for (function in functions) {
            assertQualifiedName(function, "fqnames", "fqnames.${function.name}")
        }
    }

    /** extend 已有独立 ExtendId；名称所有者沿用包作用域，不把接收者冒充名义声明。 */
    @Test
    fun extendMembersKeepTheExistingPackageOwnerProtocol() {
        val file = parse(
            "extendMember",
            """
            package fqnames
            interface Beta {}
            interface Alpha {}
            class Host {}
            extend Host <: Beta & Alpha {
                func member(): Unit {}
            }
            """.trimIndent(),
        )
        assertEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics())
        val extend = file.declarations.filterIsInstance<CjExtend>().single()
        val body = kotlin.test.assertNotNull(extend.body)
        val function = body.declarations.filterIsInstance<CjNamedFunction>().single()
        assertSame(body, function.parent)
        assertEquals("fqnames:Host<:Alpha&Beta", extend.getExtendId())
        assertQualifiedName(function, "fqnames", "fqnames.member")
    }

    private fun parse(name: String, text: String): CjFile = createPsiFile(name, text) as CjFile

    private fun assertQualifiedName(declaration: CjNamedDeclaration, parent: String, qualified: String) {
        assertEquals(parent, CjNamedDeclarationUtil.getParentFqName(declaration)?.asString())
        assertEquals(qualified, CjNamedDeclarationUtil.getFQName(declaration)?.asString())
        assertEquals(qualified, declaration.fqName?.asString())
    }

    private fun assertScopeError(file: CjFile, text: String, declaration: String, kind: String, scope: String) {
        val error = file.collectParsingDiagnostics().single()
        val offset = text.indexOf(declaration)
        assertEquals(offset, error.startOffset)
        assertEquals(offset + kind.length, error.endOffset)
        assertEquals(
            CangJieParsingBundle.message(
                "parsing.error.invalid.declaration.in.class", kind,
                CangJieParsingBundle.message("parsing.scope.$scope"),
            ),
            error.message,
        )
    }
}
