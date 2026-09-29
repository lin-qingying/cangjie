package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportGroup
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.isImportDirectiveExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 从真实分组 PSI 验证共享前缀引用、叶项 CFIR 映射和重命名后的组合路径。 */
class AnalysisApiImportGroupReferenceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/importGroupReferences",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun groupedPaths(mainFile: CjFile) {
        val directive = mainFile.importDirectives.single()
        val group = PsiTreeUtil.findChildOfType(directive, CjImportGroup::class.java)!!
        val items = directive.importItems
        assertEquals(listOf("sample.lib.Box", "sample.lib.make"), items.map { it.importedFqName?.asString() })
        val prefixNames = PsiTreeUtil.findChildrenOfType(group.importedReference, CjSimpleNameExpression::class.java)
            .sortedBy { it.textOffset }
        assertEquals(listOf("sample", "lib"), prefixNames.map { it.referencedName })
        prefixNames.forEachIndexed { index, name ->
            assertTrue(name.isImportDirectiveExpression())
            val packages = analyzeForTest(name) {
                name.resolveToSymbols().filterIsInstance<CaPackageSymbol>().map { it.fqName.asString() }
            }
            assertEquals(listOf(listOf("sample", "sample.lib")[index]), packages)
        }
        items.forEachIndexed { index, item ->
            val reference = item.importedReference as CjSimpleNameExpression
            val declaration = reference.mainReference.resolve() as CjNamedDeclaration
            assertEquals(listOf("Box", "make")[index], declaration.name)
            analyzeForTest(item) {
                val facade = (this as CaCfirSession).resolutionFacade
                val cfir = item.getOrBuildCfir(facade) as CfirImport
                assertSame(item, cfir.psi)
                assertEquals(item.importedFqName, cfir.importedFqName)
                assertSame(cfir, reference.getOrBuildCfir(facade))
                assertTrue(group.getOrBuildCfir(facade) is CfirFile)
                assertTrue(directive.getOrBuildCfir(facade) is CfirFile)
            }
        }
        WriteCommandAction.runWriteCommandAction(mainFile.project) {
            prefixNames.last().mainReference.handleElementRename("renamed")
        }
        assertEquals(listOf("sample.renamed.Box", "sample.renamed.make"), items.map { it.importedFqName?.asString() })
        assertEquals(listOf("Box", "make"), items.map { it.importedReference?.text })
    }

    @Test
    fun organizationQualifiedPaths(mainFile: CjFile) {
        val items = mainFile.importDirectives.flatMap { it.importItems }
        items.forEachIndexed { index, item ->
            val reference = item.importedReference as CjSimpleNameExpression
            val resolved = reference.mainReference.multiResolve(false).mapNotNull { it.element as? CjNamedDeclaration }
            val expectedOrganization = listOf("org1", "org1", "org2", "org2")[index]
            assertEquals(1, resolved.size) {
                val candidates = analyzeForTest(reference) {
                    val session = (this as CaCfirSession).cfirSession
                    val fqName = requireNotNull(item.importedFqName)
                    val provider = session.symbolProvider
                    val classes = provider.getClassLikeSymbolsByClassId(ClassId(fqName.parent(), fqName.shortName()))
                    val functions = provider.getTopLevelCallableSymbols(fqName.parent(), Name.identifier(reference.referencedName))
                    "classes=" + classes.map { symbol ->
                        symbol.classId.toString() to symbol.cfir.moduleData.session.cfirProvider
                            .getCfirClassifierContainerFileIfAny(symbol)?.packageDirective?.organizationName
                    } + "; functions=" + functions.map { symbol ->
                        symbol.callableId.toString() to symbol.cfir.moduleData.session.cfirProvider
                            .getCfirCallableContainerFile(symbol)?.packageDirective?.organizationName
                    }
                }
                "Import #$index ${item.text}, organization=$expectedOrganization, fullPath=${item.importedFqName}: $candidates"
            }
            assertEquals(expectedOrganization, resolved.single().containingCjFile.packageDirective?.organizationName?.asString())
            val organization = item.importGroup!!.organizationReference!!
            assertFalse(organization.isImportDirectiveExpression())
            assertTrue(analyzeForTest(organization) { organization.resolveToSymbols().isEmpty() })
        }
    }

    /** 星号导入和包别名的使用点必须继续保留绑定阶段的组织限定。 */
    @Test
    fun organizationQualifiedScopes(mainFile: CjFile) {
        val calls = PsiTreeUtil.findChildrenOfType(mainFile.declarations.single(), CjSimpleNameExpression::class.java)
            .filter { it.referencedName == "same" }.sortedBy { it.textOffset }
        assertEquals(2, calls.size)
        calls.forEachIndexed { index, reference ->
            val targets = reference.mainReference.multiResolve(false).mapNotNull { it.element as? CjNamedDeclaration }
            assertEquals(1, targets.size)
            assertEquals(listOf("org1", "org2")[index], targets.single().containingCjFile.packageDirective?.organizationName?.asString())
        }
    }

    /** 即使两个显式导入引入同名冲突，优化也不能通过丢失组织身份将其错误合并。 */
    @Test
    fun organizationAwarePlanning(mainFile: CjFile) {
        val originalItems = mainFile.importDirectives.flatMap { it.importItems }
        assertEquals(listOf("shared.lib.same", "shared.lib.same"), originalItems.map { it.importedFqName?.asString() })
        analyzeForTest(mainFile) {
            val optimization = mainFile.collectImportOptimizationPlan()
            assertTrue(optimization.duplicateImports.isEmpty(), "不同组织的相同 FQName 不构成重复导入")
            assertTrue(optimization.unusedImports.isEmpty(), "同名冲突不能通过优化任意删除其中一个组织的导入")
            assertEquals(listOf("org1", "org2"), optimization.retainedImports.map { it.organizationName?.asString() })
            assertEquals(originalItems, optimization.retainedImports)
            assertTrue(optimization.missingImports.isEmpty())

            // 此文件唯一的限定表达式都属于分组前缀，不能生成代码引用缩短操作。
            val shortening = mainFile.collectReferenceShorteningPlan()
            assertTrue(shortening.operations.isEmpty(), "导入前缀不得进入普通代码引用缩短计划")
        }
    }
}
