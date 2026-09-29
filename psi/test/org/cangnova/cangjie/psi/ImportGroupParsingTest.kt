package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 导入语句、花括号分组和源码导入项必须保留独立的语法归属。 */
class ImportGroupParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun testGroupsKeepSourceItemsAndOrderedFlatView() {
        val file = parse("""
            import a.B
            import {a.C, b.D,}
            import a.{a.B, c.D as E,}
            import org1::a.b.{C, d.E,}
            import org1::{a.B, b.C,}
        """.trimIndent())
        assertTrue(errors(file).isEmpty())
        val directives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java).toList()
        assertEquals(5, directives.size)
        assertEquals(listOf("a.B", "a.C", "b.D", "a.a.B", "a.c.D", "a.b.C", "a.b.d.E", "a.B", "b.C"),
            directives.flatMap { it.importItems }.map { it.importedFqName?.asString() })
        assertEquals(listOf("a.B", "c.D as E"), directives[2].importItems.map { it.text })
        directives.drop(1).forEach { directive ->
            val group = directive.children.single { it.node.elementType == CjNodeTypes.IMPORT_GROUP }
            directive.importItems.forEach { assertSame(group, it.parent) }
        }
        assertSame(directives[0], directives[0].importItems.single().parent)
        directives.takeLast(2).flatMap { it.importItems }.forEach {
            assertEquals(Name.identifier("org1"), it.organizationName)
        }
    }

    @Test
    fun testOrganizationAndPackageReferencesAreSeparate() {
        val file = parse("import org1::a.b.{C}")
        val directive = PsiTreeUtil.findChildOfType(file, CjImportDirective::class.java)!!
        val group = directive.children.single { it.node.elementType == CjNodeTypes.IMPORT_GROUP }
        assertEquals(listOf("org1", "a.b"), group.children.filterIsInstance<CjExpression>().map { it.text })
        assertEquals(FqName("a.b.C"), directive.importItems.single().importedFqName)
        assertEquals("C", directive.importItems.single().importedReference?.text)
    }

    @Test
    fun testContextualKeywordSegmentsAndComments() {
        val file = parse("import internal /* prefix */ .{internal.B, a.internal as internal,}")
        assertTrue(errors(file).isEmpty())
        val directive = PsiTreeUtil.findChildOfType(file, CjImportDirective::class.java)!!
        assertEquals(listOf("internal.internal.B", "internal.a.internal"),
            directive.importItems.map { it.importedFqName?.asString() })
    }

    @Test
    fun testEditingSharedPrefixUpdatesEveryExistingItem() {
        val file = parse("import org1::a.{B,C}")
        val group = PsiTreeUtil.findChildOfType(file, CjImportGroup::class.java)!!
        val items = group.importItems
        assertEquals(listOf("a.B", "a.C"), items.map { it.importedFqName?.asString() })
        val factory = CjPsiFactory(project)
        WriteCommandAction.runWriteCommandAction(project) {
            (group.importedReference as CjSimpleNameExpression).referencedNameElement
                .replace(factory.createNameIdentifier("changed"))
            group.organizationReference!!.referencedNameElement.replace(factory.createNameIdentifier("org2"))
        }
        assertEquals(listOf("changed.B", "changed.C"), items.map { it.importedFqName?.asString() })
        assertEquals(listOf("org2", "org2"), items.map { it.organizationName?.asString() })
        assertEquals(listOf("B", "C"), items.map { it.text })
    }

    @Test
    fun testIllegalGroupsReportErrorsWithoutConsumingNextDirective() {
        listOf(
            "import {}", "import {,}", "import {a.B,,c.D}",
            "import a.{b.{C}}", "import {org1::a.B}",
            "import a.{*}", "import {*} ", "import a.* as B", "import a.{B} as C",
            "import a{B}", "import org1::a{B}",
        ).forEachIndexed { index, source ->
            val file = parse("$source\nimport recovery.K", "invalidImport$index")
            assertTrue(errors(file).isNotEmpty(), source)
            val directives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertEquals(FqName("recovery.K"), directives.last().importItems.single().importedFqName, source)
        }
    }

    private fun parse(source: String, name: String = "importGroups"): CjFile =
        createPsiFile(name, source) as CjFile

    @Test
    fun testMissingBraceDoesNotConsumeModifiedDeclaration() {
        for (declaration in listOf("public class Kept {}", "public open class Kept {}", "public struct Kept {}", "public func Kept(): Unit {}", "public type Kept = Int64")) {
            for (broken in listOf("import a.{B,", "import a.{B.", "import a.{B as")) {
                val file = parse("$broken\n$declaration")
                assertTrue(errors(file).isNotEmpty())
                assertEquals(listOf("Kept"), file.declarations.filterIsInstance<CjNamedDeclaration>().map { it.name }, "$broken\n$declaration")
            }
        }
        val valid = parse("import a.{\ninternal.B,\npublic.C,\n}")
        assertTrue(errors(valid).isEmpty())
        assertEquals(listOf("a.internal.B", "a.public.C"), valid.importDirectives.single().importItems.map { it.importedFqName?.asString() })
    }

    private fun errors(file: CjFile): Collection<PsiErrorElement> =
        PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
}
