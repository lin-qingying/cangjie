package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/** 验证组织名限定符的词法、package/import 语法和相对路径语义。 */
class DoubleColonParsingTest : CjParsingTestCase(
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
    fun testDoubleColonIsOneLexerToken() {
        val lexer = CangJieLexer()
        lexer.start("::")

        assertEquals(CjTokens.DOUBLE_COLON, lexer.tokenType)
        assertEquals("::", lexer.tokenText)
        lexer.advance()
        assertEquals(null, lexer.tokenType)
    }

    @Test
    fun testPackageAndImportKeepOrganizationSeparateFromPackageName() {
        val file = createPsiFile(
            "organizationQualifiedPath",
            """
            package org1::a.b
            import org1::a.b.A
            import org1::a.b.{B, C}
            import org1::{D, E}
            """.trimIndent(),
        ) as CjFile

        val packageDirective = file.packageDirective ?: error("package directive is missing")
        assertEquals("a.b", packageDirective.qualifiedName)
        assertEquals(Name.identifier("org1"), packageDirective.organizationName)

        val imports = file.importDirectivesItem
        assertEquals(5, imports.size)
        assertEquals(FqName("a.b.A"), imports[0].importedFqName)
        assertEquals(FqName("a.b.B"), imports[1].importedFqName)
        assertEquals(FqName("a.b.C"), imports[2].importedFqName)
        assertEquals(FqName("D"), imports[3].importedFqName)
        assertEquals(FqName("E"), imports[4].importedFqName)
        assertEquals(Name.identifier("org1"), (imports[0] as CjImportItem).organizationName)
        assertEquals(Name.identifier("org1"), (imports[3] as CjImportItem).organizationName)
    }

    @Test
    fun testOrganizationSeparatorCannotAppearAfterPackagePath() {
        listOf("import a.b::c", "import a::*").forEachIndexed { index, source ->
            val file = createPsiFile("invalidOrganizationQualifiedPath$index", source) as CjFile
            assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isNotEmpty())
        }
    }
}
