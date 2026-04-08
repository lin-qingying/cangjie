package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.visibilityChecker

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.isExtendMemberDeclaration
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiVisibilityTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedSymbolVisibility
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedVisibilityExplicit
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedVisibleInSession
import org.cangnova.cangjie.analysis.api.impl.base.test.targetNameText
import org.cangnova.cangjie.analysis.api.impl.base.test.visibilityTargetKind
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * visibility checker generated 测试。
 *
 * 这组测试把“声明自身的可见性元数据”和“当前 use-site session 下的可见性结果”拆成两段校验：
 * 1. 在目标声明所属文件的 session 中校验 `visibility` / `isVisibilityExplicit`；
 * 2. 在主文件的 use-site session 中校验 `isVisible()`。
 *
 * 这样既能覆盖 source/local/extend 的基础映射，也能表达跨模块 internal 在主 session 中不可见的情况，
 * 避免把 declaration metadata 与 use-site visibility 混成同一个语义层次。
 */
abstract class AbstractVisibilityCheckerTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiVisibilityTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetDeclaration = findTargetDeclaration(
            allFiles = testServices.cjTestModuleStructure.allCjFiles,
            targetKind = directives.visibilityTargetKind,
            targetName = directives.targetNameText,
        )
        val targetFile = targetDeclaration.containingFile as? CjFile
            ?: error("Visibility target must belong to a CjFile: ${targetDeclaration::class.simpleName}")

        analyzeForTest(targetFile) {
            val symbol = resolveDeclarationSymbol(targetDeclaration)
            assertNotNull(symbol, "目标声明应能恢复为公开 declaration symbol")
            assertEquals(directives.expectedSymbolVisibility, symbol!!.visibility)
            assertEquals(directives.expectedVisibilityExplicit, symbol.isVisibilityExplicit)
        }

        analyzeForTest(mainFile) {
            val symbol = tryResolveDeclarationSymbol(targetDeclaration)
            val actualVisible = symbol?.isVisible() ?: false
            assertEquals(directives.expectedVisibleInSession, actualVisible)
        }
    }

    private fun findTargetDeclaration(
        allFiles: List<CjFile>,
        targetKind: String,
        targetName: String,
    ): PsiElement {
        val candidates = when (targetKind) {
            "TOP_LEVEL_FUNCTION" -> allFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
                    .filter { function -> function.name == targetName && !function.isExtendMemberDeclaration() }
            }

            "TOP_LEVEL_PROPERTY" -> allFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                    .filter { property -> property.name == targetName }
            }

            "CLASS" -> allFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, CjTypeStatement::class.java)
                    .filter { declaration -> declaration.name == targetName }
            }

            "EXTEND_MEMBER" -> allFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
                    .filter { function -> function.name == targetName && function.isExtendMemberDeclaration() }
            }

            "BINDING_PATTERN" -> allFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, CjBindingPattern::class.java)
                    .filter { binding -> binding.name == targetName }
            }

            else -> error("Unsupported visibility target kind: $targetKind")
        }

        return candidates.singleOrNull()
            ?: error("Cannot uniquely locate visibility target `$targetName` of kind `$targetKind`.")
    }

    private fun CaSession.resolveDeclarationSymbol(target: PsiElement): CaDeclarationSymbol? {
        return when (target) {
            is CjNamedFunction -> target.symbol
            is CjProperty -> target.symbol
            is CjTypeStatement -> target.classSymbol
            is CjBindingPattern -> target.symbol
            else -> error("Unsupported declaration PSI for visibility test: ${target::class.simpleName}")
        }
    }

    private fun CaSession.tryResolveDeclarationSymbol(target: PsiElement): CaDeclarationSymbol? {
        return runCatching { resolveDeclarationSymbol(target) }.getOrNull()
    }
}
