package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.ImportPathPrefix
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 工厂生成的真实导入 PSI 必须保留组织、别名和星号的语义身份。 */
class ImportPathFactoryTest : CjParsingTestCase(
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
    fun testOrganizationQualifiedFactoryRoundTrip() {
        val paths = listOf(
            ImportPath(FqName("a.B"), false, Name.identifier("Renamed"), Name.identifier("org1")),
            ImportPath(FqName("a"), true, organizationName = Name.identifier("org2")),
            ImportPath(FqName("a.B"), false),
        )
        paths.forEach { path ->
            val directive = CjPsiFactory(project).createImportDirective(path)
            assertTrue(PsiTreeUtil.findChildrenOfType(directive, PsiErrorElement::class.java).isEmpty())
            assertEquals(path, directive.importItems.single().importPath)
        }
    }

    @Test
    fun testGroupFactoryPreservesLocalAndCombinedPaths() {
        val items = listOf(ImportPath(FqName("B"), false, Name.identifier("Alias")), ImportPath(FqName("c"), true))
        val cases = listOf(
            null to listOf("B", "c"),
            ImportPathPrefix(null, FqName.ROOT) to listOf("B", "c"),
            ImportPathPrefix(Name.identifier("org1"), FqName.ROOT) to listOf("B", "c"),
            ImportPathPrefix(Name.identifier("org1"), FqName("a")) to listOf("a.B", "a.c"),
        )
        cases.forEach { (prefix, expected) ->
            val directive = CjPsiFactory(project).createImportDirective(prefix, items)
            val group = directive.importItems.first().importGroup!!
            assertEquals(listOf("B", "c"), group.importItems.map { it.localFqName?.asString() })
            assertEquals(expected, directive.importItems.map { it.importedFqName?.asString() })
            assertEquals(List(2) { prefix?.organizationName }, directive.importItems.map { it.organizationName })
            assertEquals("Alias", group.importItems.first().aliasName)
            assertTrue(group.importItems.last().isAllUnder)
        }
        assertFailsWith<IllegalArgumentException> { CjPsiFactory(project).createImportDirective(null, emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            CjPsiFactory(project).createImportDirective(null, listOf(items.first().copy(organizationName = Name.identifier("other"))))
        }
    }

    @Test
    fun testFactoriesPreserveEscapedOrganizationPackageAndAliasNames() {
        val local = ImportPath(FqName("internal.B"), false, Name.identifier("class"))
        val organization = Name.identifier("org-one")
        val singlePath = local.copy(organizationName = organization)
        val single = CjPsiFactory(project).createImportDirective(singlePath).importItems.single()
        assertEquals(singlePath, single.importPath)
        assertEquals("class", single.alias?.name)
        val group = CjPsiFactory(project).createImportDirective(ImportPathPrefix(organization, FqName("class")), listOf(local))
        val grouped = group.importItems.single()
        assertEquals(singlePath.copy(fqName = FqName("class.internal.B")), grouped.importPath)
        assertEquals("internal.B", grouped.localFqName?.asString())
        assertEquals("class", grouped.alias?.name)
        assertTrue(PsiTreeUtil.findChildrenOfType(group, PsiErrorElement::class.java).isEmpty())
    }

    @Test
    fun testDeletingGroupItemsPreservesRemainingCommentsAndValidSeparators() {
        listOf(0, 1, 2).forEach { removedIndex ->
            val file = createPsiFile("remove$removedIndex", "import a.{B, /* keep middle */ C, D, /* keep end */}\nimport other.E") as CjFile
            val directive = file.importDirectives.first()
            WriteCommandAction.runWriteCommandAction(project) { directive.importItems[removedIndex].delete() }
            assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
            assertEquals(listOf("a.B", "a.C", "a.D").filterIndexed { index, _ -> index != removedIndex },
                directive.importItems.map { it.importedFqName?.asString() })
            assertTrue(file.text.contains("/* keep middle */"))
            assertTrue(file.text.contains("/* keep end */"))
            val reparsed = createPsiFile("reparsed$removedIndex", file.text)
            assertTrue(PsiTreeUtil.findChildrenOfType(reparsed, PsiErrorElement::class.java).isEmpty())
        }
    }

    @Test
    fun testDeletingLastItemRemovesItsStatement() {
        listOf("import a.B", "import a.{B}").forEachIndexed { index, source ->
            val file = createPsiFile("last$index", "$source\n// keep following\nimport other.E") as CjFile
            WriteCommandAction.runWriteCommandAction(project) { file.importDirectives.first().importItems.single().delete() }
            assertEquals(listOf("other.E"), file.importDirectives.flatMap { it.importItems }.map { it.importedFqName?.asString() })
            assertTrue(file.text.contains("// keep following"))
        }
    }

    @Test
    fun testDeletingFinalGroupItemWithoutTrailingCommaPreservesComment() {
        val file = createPsiFile("finalItem", "import a.{B, /* keep */ C}") as CjFile
        WriteCommandAction.runWriteCommandAction(project) { file.importDirectives.single().importItems.last().delete() }
        assertTrue(file.text.contains("/* keep */"))
        val reparsed = createPsiFile("finalItemReparsed", file.text) as CjFile
        assertTrue(PsiTreeUtil.findChildrenOfType(reparsed, PsiErrorElement::class.java).isEmpty())
        assertEquals("a.B", reparsed.importDirectives.single().importItems.single().importedFqName?.asString())
    }

    @Test
    fun testMovingItemBetweenGroupsUsesItsNewPrefix() {
        val file = createPsiFile("moveItem", "import org1::a.{C as Alias, /* retain source */ D}\nimport org2::b.{Placeholder, /* retain target */ E}") as CjFile
        val sourceDirective = file.importDirectives[0]
        val targetDirective = file.importDirectives[1]
        val original = sourceDirective.importItems.first()
        assertEquals("a.C", original.importedFqName?.asString())
        val localText = original.text
        lateinit var moved: CjImportItem
        WriteCommandAction.runWriteCommandAction(project) {
            moved = targetDirective.importItems.first().replace(original.copy()) as CjImportItem
            original.delete()
        }
        assertEquals(localText, moved.text)
        assertEquals("C", moved.localFqName?.asString())
        assertEquals("b.C", moved.importedFqName?.asString())
        assertEquals("org2", moved.organizationName?.asString())
        assertEquals("Alias", moved.aliasName)
        assertEquals(listOf("a.D"), sourceDirective.importItems.map { it.importedFqName?.asString() })
        assertEquals(listOf("b.C", "b.E"), targetDirective.importItems.map { it.importedFqName?.asString() })
        assertTrue(file.text.contains("/* retain source */"))
        assertTrue(file.text.contains("/* retain target */"))
        val reparsed = createPsiFile("moveItemReparsed", file.text)
        assertTrue(PsiTreeUtil.findChildrenOfType(reparsed, PsiErrorElement::class.java).isEmpty())
    }

    @Test
    fun testUnitVisitorReachesGroupsAndAllLeafItemsInSourceOrder() {
        val file = createPsiFile("importVisitor", "import direct.A\nimport org1::a.{B, c.D}\nimport {e.F, g.H}") as CjFile
        val visits = mutableListOf<String>()
        file.accept(object : CjTreeVisitorUnit() {
            override fun visitImportGroup(importGroup: CjImportGroup) {
                visits += "group"
                super.visitImportGroup(importGroup)
            }

            override fun visitImportItem(importItem: CjImportItem) {
                visits += "item:${importItem.importedFqName?.asString()}"
                super.visitImportItem(importItem)
            }
        })
        assertEquals(listOf("item:direct.A", "group", "item:a.B", "item:a.c.D", "group", "item:e.F", "item:g.H"), visits)
    }
}
