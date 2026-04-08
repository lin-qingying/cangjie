package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchSession
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.idea.search.CangJieReferencesSearchExecutor
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定仓颉 Find Usages 主链的正式能力。
 *
 * 这里不再只验证“能搜到一个 top-level 函数”这样的最小闭环，而是同时覆盖：
 * 1. top-level declaration usages
 * 2. extend member usages
 * 3. import alias usages
 * 4. pattern binding usages
 *
 * 这样 `useScope`、alias 扩展搜索名、引用解析与 references-search executor
 * 才能被当作一个统一子系统回归。
 */
class AnalysisApiFindUsagesTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/findUsages",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun topLevelFunctionUsages(mainFile: CjFile) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "greet" }

        val references = findUsages(declaration)
        assertEquals(3, references.size, "Top-level 函数的 usages 数量不正确")
        assertTrue(
            references.all { it.element.containingFile == mainFile },
            "Top-level 函数回归夹具中的 usages 应全部落在主文件内",
        )
    }

    @Test
    fun extendMemberUsages(mainFile: CjFile) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "prettyPrint" && it.parent is CjExtend }

        val references = findUsages(declaration)
        assertEquals(2, references.size, "extend 成员 usages 数量不正确")
        assertTrue(
            references.all { it.element.containingFile == mainFile },
            "extend 成员回归夹具中的 usages 应全部落在声明所在文件内",
        )
    }

    @Test
    fun importAliasUsages(mainFile: CjFile) {
        val alias = PsiTreeUtil.findChildrenOfType(mainFile, CjImportAlias::class.java)
            .single { it.name == "welcome" }

        val aliasReferences = findUsages(alias)
        assertEquals(2, aliasReferences.size, "import alias usages 数量不正确")
        assertReferencesStayInsideLocalScope(alias, aliasReferences)

        val providerFile = siblingFiles(mainFile).single { it.name == "provider.cj" }
        val originalDeclaration = PsiTreeUtil.findChildrenOfType(providerFile, CjNamedFunction::class.java)
            .single { it.name == "greet" }

        val originalReferences = findUsages(originalDeclaration)
        assertEquals(3, originalReferences.size, "原始声明应同时覆盖 alias import 与 alias 调用位")
        assertTrue(
            originalReferences.count { it.element.containingFile == mainFile } == 3,
            "通过 alias 进入的 usages 应全部出现在 consumer 文件中",
        )
    }

    @Test
    fun patternBindingUsages(mainFile: CjFile) {
        val bindingPattern = PsiTreeUtil.findChildrenOfType(mainFile, CjBindingPattern::class.java)
            .single { it.name == "value" }

        val references = findUsages(bindingPattern)
        assertEquals(2, references.size, "pattern binding usages 数量不正确")
        assertReferencesStayInsideLocalScope(bindingPattern, references)
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

    private fun siblingFiles(mainFile: CjFile): List<CjFile> {
        return mainFile.containingDirectory?.files
            ?.filterIsInstance<CjFile>()
            ?.toList()
            .orEmpty()
    }
}
