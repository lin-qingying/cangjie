package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 原生 UI fixture 中 ContextInner 的输入是类字段，AST/stub 和非局部宿主必须保留同一角色。 */
class MacroDeclarationSourceOwnershipTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun testContextInnerInputsRemainFieldsWithOnePhysicalClassOwner() {
        val file = createPsiFile("contextInputs", SOURCE) as CjFile
        assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
        val klass = file.declarations.filterIsInstance<CjClass>().single()
        val fields = assertPresent(klass.body).declarations.filterIsInstance<CjFieldVariable>()
        assertEquals(listOf("firstCount", "secondCount"), fields.map { it.name })
        fields.forEach { field ->
            assertSame(klass, field.containingTypeStatement)
            val input = assertPresent(field.parent as? CjMacroInput)
            assertSame(field, (input.parent as CjMacroExpression).unwrappedDeclaration)
        }
        val patterns = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
        assertEquals(1, patterns.size, "只有函数体内 let value 使用模式声明")
        assertNull(patterns.single().containingTypeStatement)
        assertEquals(2, assertPresent(klass.body).declarations.size)
    }

    @Test
    fun testStubMacroInputsKeepFieldRolesAndCanonicalContainer() {
        val file = createPsiFile("contextInputStubs", SOURCE) as CjFile
        val root = CjFileStubBuilder().buildStubTree(file)
        val outer = root.childrenStubs.map { it.psi }.filterIsInstance<CjMacroExpression>().single()
        val klass = assertPresent(outer.unwrappedDeclaration) as CjClass
        val fields = assertPresent(klass.body).declarations.filterIsInstance<CjFieldVariable>()
        assertEquals(listOf("firstCount", "secondCount"), fields.map { it.name })
        fields.forEach { field ->
            assertSame(klass, field.containingTypeStatement)
            assertTrue(field.parent is CjMacroExpression, "stub 宏输入不创建 MACRO_INPUT 层")
            assertSame(field, (field.parent as CjMacroExpression).unwrappedDeclaration)
        }
    }

    @Test
    fun testNestedDeclarationWrappersDoNotLiftLocalOrTokenInputDeclarations() {
        val file = createPsiFile("nestedInputOwners", """
            package sourceowners
            class Owner {
                @Outer @Inner var member: Int64 = 42
                func local(): Unit {
                    @Local let value = 1
                    @Tokens(var tokenPayload = 0)
                }
            }
        """.trimIndent()) as CjFile
        val klass = file.declarations.single() as CjClass
        val member = assertPresent(klass.body).declarations.filterIsInstance<CjFieldVariable>().single()
        assertSame(klass, member.containingTypeStatement)
        val local = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java).single()
        assertNull(local.containingTypeStatement)
        val tokens = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java)
            .single { it.shortName?.asString() == "Tokens" }
        assertTrue(!tokens.hasDeclarationInput)
        assertNull(tokens.unwrappedDeclaration)
    }

    private companion object {
        /** 与用户 native fixture 保持相同字段与包裹关系，未改 fixture 内容或预期角色。 */
        val SOURCE = """
            package untitled89
            @ContextOuter
            class ContextMacroProbe {
                @ContextInner
                var firstCount: Int64 = 40
                @ContextInner
                var secondCount: Int64 = 2
            }
            func generatedContextProbe(): Int64 {
                let value = ContextMacroProbe()
                return value.generatedCount()
            }
        """.trimIndent()
    }
}
