package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.visibilityChecker

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.createUseSiteVisibilityChecker
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
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
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * visibility checker generated 测试。
 *
 * 这组测试把“声明元数据”和“use-site 可见性”拆开校验：
 * 1. 先在目标声明所在文件的 session 中校验 `visibility` / `isVisibilityExplicit`；
 * 2. 再在主文件 use-site session 中通过 `createUseSiteVisibilityChecker(...)` 校验可见性。
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
            val useSiteElement = runCatching { findUsageSimpleName(mainFile, directives.targetNameText) }.getOrNull()
            val receiverExpression = useSiteElement?.getStrictParentOfType<CjDotQualifiedExpression>()
            val actualVisible = symbol?.let { declarationSymbol ->
                checkVisibility(
                    declarationSymbol = declarationSymbol,
                    useSiteElement = useSiteElement ?: mainFile,
                    receiverExpression = receiverExpression?.receiverExpression,
                    useSiteFileSymbol = mainFile.symbol,
                )
            } ?: false
            assertEquals(directives.expectedVisibleInSession, actualVisible)
        }
    }

    private fun CaSession.checkVisibility(
        declarationSymbol: CaDeclarationSymbol,
        useSiteElement: PsiElement,
        receiverExpression: CjExpression?,
        useSiteFileSymbol: org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol,
    ): Boolean {
        val visibleByUseSiteChecker = createUseSiteVisibilityChecker(
            useSiteFile = useSiteFileSymbol,
            receiverExpression = receiverExpression,
            position = useSiteElement,
        ).isVisible(declarationSymbol)

        @Suppress("DEPRECATION")
        val visibleByDeprecatedEntry = isVisible(
            candidateSymbol = declarationSymbol,
            useSiteFile = useSiteFileSymbol,
            receiverExpression = receiverExpression,
            position = useSiteElement,
        )

        assertEquals(
            visibleByDeprecatedEntry,
            visibleByUseSiteChecker,
            "deprecated isVisible(...) 与 createUseSiteVisibilityChecker(...).isVisible(...) 结果不一致。",
        )

        return visibleByUseSiteChecker
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
