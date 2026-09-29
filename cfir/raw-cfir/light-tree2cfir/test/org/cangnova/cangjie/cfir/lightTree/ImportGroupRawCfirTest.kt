package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.isReexportingSourceImport
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** PSI 与 LightTree 独立验证导入路径组合以及 item 源码范围。 */
@RunWith(JUnit3RunnerWithInners::class)
class ImportGroupRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testInternalGroupVisibilityInBothBuilders() {
        for (source in listOf("internal import {std.collection.ArrayList}", "internal import {std.collection.ArrayList as b}")) {
            for (file in listOf(buildPsi(source), buildLightTree(source))) {
                assertEquals(org.cangnova.cangjie.descriptors.Visibilities.Internal, file.imports.single().visibility, source)
            }
        }
    }

    fun testPsiImportGroups() = assertImports(buildPsi(SOURCE))

    fun testLightTreeImportGroups() = assertImports(buildLightTree(SOURCE))

    fun testConditionalVisibilityBelongsToStatementInBothBuilders() {
        val source = "@When[os == \"Windows\"] public import a.{B, C};"
        for (file in listOf(buildPsi(source), buildLightTree(source))) {
            assertEquals(listOf("a.B", "a.C"), file.imports.map { it.importedFqName?.asString() })
            kotlin.test.assertTrue(file.imports.all { it.condition != null })
            kotlin.test.assertTrue(file.imports.all { it.isReexportingSourceImport() })
        }
    }

    fun testPsiEmptyGroupDoesNotImportPrefix() {
        assertEquals(listOf("recovery.K"), buildPsi("import a.{}\nimport recovery.K").imports.map { it.importedFqName?.asString() })
    }

    fun testLightTreeEmptyGroupDoesNotImportPrefix() {
        assertEquals(listOf("recovery.K"), buildLightTree("import a.{}\nimport recovery.K").imports.map { it.importedFqName?.asString() })
    }

    fun testInvalidGroupsAgreeAcrossBuilders() {
        listOf("import a.{*}", "import a.{B,,C}", "import a.{b.{C}}", "import {org1::a.B}").forEach { invalid ->
            val source = "$invalid\nimport recovery.K"
            val psi = buildPsi(source).imports.map { it.importedFqName?.asString() }
            val lightTree = buildLightTree(source).imports.map { it.importedFqName?.asString() }
            assertEquals(psi, lightTree, invalid)
            kotlin.test.assertEquals("recovery.K", lightTree.last(), invalid)
            check("a" !in lightTree) { "无效 item 不能退化成共享前缀导入：$invalid" }
        }
    }

    private fun assertImports(file: CfirFile) {
        assertEquals(listOf("a.B", "a.C", "b.D", "a.a.B", "a.c.D", "a.b.C", "a.b.d.E", "a.B", "b", "internal.internal.B"),
            file.imports.map { it.importedFqName?.asString() })
        assertEquals(listOf(null, null, null, null, null, "org1", "org1", "org1", "org1", null),
            file.imports.map { it.organizationName?.asString() })
        assertEquals(listOf(null, null, null, null, "E", null, null, null, null, null),
            file.imports.map { it.aliasName?.asString() })
        assertEquals(listOf(false, false, false, false, false, false, false, false, true, false),
            file.imports.map { it.isAllUnder })
        assertEquals(listOf("a.B", "a.C", "b.D", "a.B", "c.D as E", "C", "d.E", "a.B", "b.*", "internal.B"),
            file.imports.map { import ->
                val source = checkNotNull(import.source)
                SOURCE.substring(source.startOffset, source.endOffset)
            })
    }

    private fun buildPsi(source: String): CfirFile =
        PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
            .buildCfirFile(createCjFile("importGroups", source))

    private fun buildLightTree(source: String): CfirFile {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile("importGroups.cj", null, source)
        return LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(parseLightTree(source), sourceFile, source.toSourceLinesMapping()).first
    }

    companion object {
        private val SOURCE = """
            import a.B
            import {a.C, b.D,}
            import a.{a.B, c.D as E,}
            import org1::a.b.{C, d.E,}
            import org1::{a.B, b.*,}
            import internal /* prefix */ .{internal.B,}
        """.trimIndent()
    }
}
