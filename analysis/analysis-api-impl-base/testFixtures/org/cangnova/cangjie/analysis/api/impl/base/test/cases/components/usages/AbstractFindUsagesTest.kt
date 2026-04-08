package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.usages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchSession
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiUsageTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedUsageCount
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedUsageScopeKind
import org.cangnova.cangjie.analysis.api.impl.base.test.usageTargetKind
import org.cangnova.cangjie.analysis.api.impl.base.test.usageTargetName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.idea.search.CangJieReferencesSearchExecutor
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * generated find usages 抽象测试。
 *
 * 该测试直接对位 Kotlin analysis 中“references search -> usages”主链，
 * 统一验证：
 * 1. top-level declaration usages
 * 2. extend member usages
 * 3. import alias usages
 * 4. pattern binding usages
 */
abstract class AbstractFindUsagesTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiUsageTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val target = findTarget(mainFile, directives.usageTargetKind, directives.usageTargetName)
        val references = findUsages(target)

        assertEquals(directives.expectedUsageCount, references.size)
        if (directives.expectedUsageScopeKind == "LOCAL") {
            assertReferencesStayInsideLocalScope(target, references)
        }
    }

    private fun findTarget(
        mainFile: CjFile,
        targetKind: String,
        targetName: String,
    ): PsiElement = when (targetKind) {
        "TOP_LEVEL_FUNCTION" -> PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == targetName && it.parent !is CjExtend }

        "EXTEND_MEMBER" -> PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == targetName && it.parent is CjExtend }

        "IMPORT_ALIAS" -> PsiTreeUtil.findChildrenOfType(mainFile, CjImportAlias::class.java)
            .single { it.name == targetName }

        "BINDING_PATTERN" -> PsiTreeUtil.findChildrenOfType(mainFile, CjBindingPattern::class.java)
            .single { it.name == targetName }

        else -> error("Unsupported usages target kind: $targetKind")
    }

    private fun findUsages(target: PsiElement): List<PsiReference> {
        val references = mutableListOf<PsiReference>()
        val executor = CangJieReferencesSearchExecutor()
        val parameters = ReferencesSearch.SearchParameters(
            target,
            target.useScope,
            false,
            SearchRequestCollector(SearchSession(target)),
        )
        executor.execute(
            parameters,
            Processor { reference ->
                references += reference
                true
            },
        )
        return references
    }

    private fun assertReferencesStayInsideLocalScope(
        target: PsiElement,
        references: List<PsiReference>,
    ) {
        val localScope = target.useScope as? LocalSearchScope
            ?: error("Expected LocalSearchScope for ${target::class.simpleName}")

        assertTrue(
            references.all { reference ->
                localScope.scope.any { root ->
                    root == reference.element || PsiTreeUtil.isAncestor(root, reference.element, false)
                }
            },
            "局部声明的 usages 不应逃逸出其 LocalSearchScope",
        )
    }
}
