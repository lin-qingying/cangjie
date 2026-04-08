package org.cangnova.cangjie.idea.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.idea.references.CangJieReferenceSearchSupport
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjFile

/**
 * 仓颉 `ReferencesSearch` 执行器。
 *
 * Kotlin 在 IDE 中主要依赖索引驱动的 usages 搜索；仓颉当前 headless/standalone 主线
 * 还没有完整复刻 IntelliJ 索引服务，因此这里明确采用一条与 analysis session 对齐的
 * source-backed 搜索链：
 *
 * `useScope` -> 候选文件裁剪 -> reference 名称裁剪 -> `isReferenceTo` / resolve 复核
 *
 * 关键点不是“遍历 PSI”本身，而是 usages 搜索与 `cj-references` 的统一协议共用：
 * 1. `resolvesByNames`
 * 2. `isReferenceTo`
 * 3. original/navigation element identity
 * 4. import alias 扩展搜索名
 */
class CangJieReferencesSearchExecutor :
    QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true),
    DumbAware {

    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val target = queryParameters.elementToSearch
        val baseSearchNames = CangJieReferenceSearchSupport.baseSearchNames(target)
        if (baseSearchNames.isEmpty()) {
            return
        }

        val effectiveScope = effectiveScope(queryParameters, target)
        val psiManager = PsiManager.getInstance(target.project)

        when (effectiveScope) {
            is LocalSearchScope -> {
                val roots = LinkedHashSet(effectiveScope.scope.asList())
                for (root in roots) {
                    val searchNames = localSearchNames(root, target, baseSearchNames)
                    if (!processElementTree(root, target, searchNames, consumer)) {
                        return
                    }
                }
            }

            is GlobalSearchScope -> {
                val sourceItems = target.project.getService(CaProjectStructureProvider::class.java)
                    ?.snapshot
                    ?.allSourceFiles
                    .orEmpty()

                for (item in sourceItems) {
                    if (!processSourceItem(item, psiManager, effectiveScope, baseSearchNames, target, consumer)) {
                        return
                    }
                }
            }

            else -> return
        }
    }

    private fun effectiveScope(
        queryParameters: ReferencesSearch.SearchParameters,
        target: PsiElement,
    ): SearchScope {
        val userScope = queryParameters.scopeDeterminedByUser
        return if (queryParameters.isIgnoreAccessScope) {
            userScope
        } else {
            userScope.intersectWith(target.useScope)
        }
    }

    private fun localSearchNames(
        root: PsiElement,
        target: PsiElement,
        baseSearchNames: Set<String>,
    ): Set<String> {
        val file = root.containingFile as? CjFile ?: return baseSearchNames
        return CangJieReferenceSearchSupport.searchNamesForFile(file, target, baseSearchNames)
    }

    private fun processSourceItem(
        item: PsiFileSystemItem,
        psiManager: PsiManager,
        scope: GlobalSearchScope,
        baseSearchNames: Set<String>,
        target: PsiElement,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        return when (item) {
            is CjFile -> {
                val searchNames = CangJieReferenceSearchSupport.searchNamesForFile(item, target, baseSearchNames)
                val virtualFile = item.virtualFile
                if ((virtualFile != null && !scope.contains(virtualFile)) || !item.mayContainAnyName(searchNames)) {
                    true
                } else {
                    processElementTree(item, target, searchNames, consumer)
                }
            }

            else -> {
                val root = item.virtualFile ?: return true
                var shouldContinue = true
                VfsUtilCore.iterateChildrenRecursively(root, null) { child ->
                    if (!shouldContinue) {
                        return@iterateChildrenRecursively false
                    }
                    if (child.isDirectory) {
                        return@iterateChildrenRecursively true
                    }
                    if (child.fileType != CangJieFileType.INSTANCE || !scope.contains(child)) {
                        return@iterateChildrenRecursively true
                    }

                    val psiFile = psiManager.findFile(child) as? CjFile ?: return@iterateChildrenRecursively true
                    val searchNames = CangJieReferenceSearchSupport.searchNamesForFile(psiFile, target, baseSearchNames)
                    if (!psiFile.mayContainAnyName(searchNames)) {
                        return@iterateChildrenRecursively true
                    }

                    shouldContinue = processElementTree(psiFile, target, searchNames, consumer)
                    shouldContinue
                }
                shouldContinue
            }
        }
    }

    private fun processElementTree(
        element: PsiElement,
        target: PsiElement,
        searchNames: Set<String>,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        for (reference in element.references) {
            if (!CangJieReferenceSearchSupport.mayResolveByName(reference, searchNames)) {
                continue
            }

            if (CangJieReferenceSearchSupport.matchesTarget(reference, target) && !consumer.process(reference)) {
                return false
            }
        }

        for (child in element.children) {
            if (!processElementTree(child, target, searchNames, consumer)) {
                return false
            }
        }

        return true
    }

    private fun CjFile.mayContainAnyName(names: Set<String>): Boolean {
        return names.any(text::contains)
    }
}
