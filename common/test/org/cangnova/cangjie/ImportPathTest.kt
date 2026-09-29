package org.cangnova.cangjie

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/** 组织限定和分组组合必须保留导入身份及全部源码路径段。 */
class ImportPathTest {
    @Test
    fun organizationSurvivesRenderingParsingAndIdentityComparison() {
        val first = ImportPath(FqName("a.b"), true, organizationName = Name.identifier("org1"))
        val second = first.copy(organizationName = Name.identifier("org2"))
        assertEquals("org1::a.b.*", first.pathStr)
        assertEquals(first, ImportPath.fromString(first.pathStr))
        assertNotEquals(first, second)
        assertEquals(2, linkedSetOf(first, second).size)
        assertEquals("org1::a.b as B", first.copy(isAllUnder = false, alias = Name.identifier("B")).toString())
        val aliased = first.copy(isAllUnder = false, alias = Name.identifier("B"))
        assertEquals(aliased, ImportPath.fromString(aliased.toString()))
    }

    @Test
    fun repeatedLocalSegmentsAreNotMistakenForAnAlreadyQualifiedPath() {
        val prefix = ImportPathPrefix(Name.identifier("org1"), FqName("a"))
        assertEquals(ImportPathPrefix(Name.identifier("org1"), FqName("a.a.B")), prefix.resolveImportPath(FqName("a.B")))
    }

    @Test
    fun organizationOnlyPrefixAndAbsentPrefixRemainDifferent() {
        val prefix = ImportPathPrefix(Name.identifier("org1"), FqName.ROOT)
        assertEquals(ImportPathPrefix(Name.identifier("org1"), FqName("D")), prefix.resolveImportPath(FqName("D")))
        assertEquals(ImportPathPrefix(null, FqName("D")), null.resolveImportPath(FqName("D")))
        assertEquals(prefix, null.resolveImportPath(FqName.ROOT, Name.identifier("org1")))
    }

    @Test
    fun emptyRelativePathDoesNotAlterItsNamespacePrefix() {
        val prefix = ImportPathPrefix(null, FqName("a.b"))
        assertEquals(prefix, prefix.resolveImportPath(FqName.ROOT))
    }

    @Test
    fun nestedOrganizationQualifierCannotBeCombined() {
        assertFailsWith<IllegalArgumentException> {
            ImportPathPrefix(null, FqName("a")).resolveImportPath(FqName("B"), Name.identifier("org2"))
        }
        assertFailsWith<IllegalArgumentException> { ImportPath.fromString("org1::org2::a.B") }
    }

    @Test
    fun incompletePathsAndMalformedAliasesAreRejected() {
        listOf("", "org::", "::a.B", "bad:org::a.B", "a..B", "a.B as", "a.B as B as C", "a.* as B").forEach { source ->
            assertFailsWith<IllegalArgumentException>(source) { ImportPath.fromString(source) }
        }
        assertEquals(ImportPath(FqName("internal.B"), false, Name.identifier("Alias")), ImportPath.fromString("internal.B as Alias"))
    }

    @Test
    fun escapedNamesRoundTripWithoutKeepingSourceQuotesInTheirIdentity() {
        val path = ImportPath(FqName("internal.class"), false, Name.identifier("a as b"), Name.identifier("org-one"))
        assertEquals(path, ImportPath.fromString(path.toString()))
        assertEquals(path.toString(), ImportPath.fromString(path.toString()).toString())
        assertEquals(ImportPath(FqName("a.B"), false, Name.identifier("class"), Name.identifier("org")),
            ImportPath.fromString("`org`::a.B as `class`"))
    }
}
