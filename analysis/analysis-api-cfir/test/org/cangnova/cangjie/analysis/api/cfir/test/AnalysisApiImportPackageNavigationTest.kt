@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportGroup
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.packgae.CangJiePackage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 包前缀必须能返回有真实目录归属的 PSI 导航目标，组织限定不能在 symbol→PSI 时丢失。 */
class AnalysisApiImportPackageNavigationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/importPackageNavigation",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun organizationDirectoriesAndRename(mainFile: CjFile) {
        val groups = mainFile.importDirectives.map { PsiTreeUtil.findChildOfType(it, CjImportGroup::class.java)!! }
        fun terminalReference(group: CjImportGroup): CjSimpleNameExpression =
            PsiTreeUtil.findChildrenOfType(group.importedReference, CjSimpleNameExpression::class.java).maxBy { it.textOffset }
        fun resolvedPackage(group: CjImportGroup): CangJiePackage {
            val reference = terminalReference(group)
            val targets = reference.mainReference.multiResolve(false).mapNotNull { it.element }
            assertEquals(1, targets.size) {
                analyzeForTest(reference) {
                    val modules = CaModuleProvider.getInstance(mainFile.project)
                    val declarations = CangJieDeclarationProviderFactory.getInstance(mainFile.project)
                    val files = modules.allSourceFiles.map { file ->
                        val psi = file as? CjFile
                        "${file.virtualFile?.url} pkg=${psi?.packageFqName} org=${psi?.packageDirective?.organizationName} " +
                            "analysisScope=${file.virtualFile?.let(analysisScope::contains)}"
                    }
                    val packages = modules.allModules.map { module ->
                        val provider = declarations.createDeclarationProvider(analysisScope.intersectWith(module.contentScope), module)
                        "$module: ${provider.getPackageFiles(FqName("shared.lib"), Name.identifier("org1")).map { it.virtualFile?.url }}"
                    }
                    "symbols=${reference.resolveToSymbols().map { symbol -> "${symbol::class.simpleName} psi=${symbol.psi}" }}; " +
                        "main=${mainFile.virtualFile.url}; files=$files; packageFiles=$packages"
                }
            }
            val target = targets.single() as CangJiePackage
            assertTrue(target.manager.areElementsEquivalent(target, reference.mainReference.resolve()))
            return target
        }
        val first = resolvedPackage(groups[0])
        val second = resolvedPackage(groups[1])
        assertEquals("shared.lib", first.qualifiedName)
        assertEquals("shared.lib", second.qualifiedName)
        assertEquals("org1", first.organizationName?.asString())
        assertEquals("org2", second.organizationName?.asString())
        val firstDirectories = first.directories.map { it.virtualFile.path }.toSet()
        val secondDirectories = second.directories.map { it.virtualFile.path }.toSet()
        assertEquals(setOf("org1a", "org1b"), first.directories.map { it.name }.toSet())
        assertEquals(setOf("org2"), second.directories.map { it.name }.toSet())
        assertTrue(first.canNavigate())
        assertTrue(second.canNavigate())
        assertTrue(firstDirectories.intersect(secondDirectories).isEmpty())
        groups.forEachIndexed { index, group ->
            val ancestorReference = PsiTreeUtil.findChildrenOfType(group.importedReference, CjSimpleNameExpression::class.java)
                .minBy { it.textOffset }
            val ancestor = ancestorReference.mainReference.resolve() as CangJiePackage
            assertEquals("shared", ancestor.qualifiedName)
            assertEquals(listOf("org1", "org2")[index], ancestor.organizationName?.asString())
            assertEquals(listOf(setOf("org1a", "org1b", "renamed"), setOf("org2"))[index],
                ancestor.directories.map { it.name }.toSet())
        }
        groups.forEachIndexed { index, group ->
            val reference = terminalReference(group)
            val organization = analyzeForTest(reference) {
                (reference.resolveToSymbols().single() as CaPackageSymbol).organizationName?.asString()
            }
            assertEquals(listOf("org1", "org2")[index], organization)
        }

        // CoreLocalFileSystem 的物理源根只读；编辑使用保留原文件分析上下文的 PSI 副本，
        // 包目标仍必须来自上面已经验证的真实 VFS 目录。
        val editableFile = mainFile.copy() as CjFile
        val editableGroups = editableFile.importDirectives.map { PsiTreeUtil.findChildOfType(it, CjImportGroup::class.java)!! }
        assertEquals("shared.lib", resolvedPackage(editableGroups[0]).qualifiedName)
        WriteCommandAction.runWriteCommandAction(mainFile.project) {
            terminalReference(editableGroups[0]).mainReference.handleElementRename("changed")
        }
        assertEquals("shared.changed.Box", editableGroups[0].importItems.single().importedFqName?.asString())
        val renamed = resolvedPackage(editableGroups[0])
        assertEquals("shared.changed", renamed.qualifiedName)
        assertEquals("org1", renamed.organizationName?.asString())
        assertEquals(setOf("renamed"), renamed.directories.map { it.name }.toSet())
        assertTrue(renamed.directories.none { it.virtualFile.path in firstDirectories })
        assertEquals("shared.lib", resolvedPackage(editableGroups[1]).qualifiedName)
        assertEquals("shared.lib", resolvedPackage(groups[0]).qualifiedName)
    }
}
