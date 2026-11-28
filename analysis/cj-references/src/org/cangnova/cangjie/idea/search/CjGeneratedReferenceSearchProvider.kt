package org.cangnova.cangjie.idea.search

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor
import org.cangnova.cangjie.extensions.ProjectExtensionDescriptor
import org.cangnova.cangjie.psi.CangJieResolveResult
import org.cangnova.cangjie.idea.references.CjReference
import org.cangnova.cangjie.idea.references.CjReferenceRole

/**
 * 一个由生成语义模型发现的引用，以及它应在编辑器中显示的位置。
 *
 * `reference` 保留生成代码中的真实解析目标；`navigationElement` 指向可编辑的宿主源码位置。
 */
data class CjGeneratedReference(
    val reference: PsiReference,
    val navigationElement: PsiElement,
    /** 最终成功符号的规范声明与导航位置，不能用共同宏来源代替目标。 */
    val resolveResult: CangJieResolveResult,
) {
    val referenceRole: CjReferenceRole
        get() = (reference as? CjReference)?.referenceRole ?: CjReferenceRole.OTHER
}

/**
 * 为仓颉 Find Usages 提供由生成语义模型发现的引用。
 *
 * Provider 返回的引用由统一的 `CangJieReferencesSearchExecutor` 按名称、目标身份和用户搜索范围复核，
 * 并把最终结果包装到 [CjGeneratedReference.navigationElement] 上，保证搜索结果能回到可编辑源码。
 */
interface CjGeneratedReferenceSearchProvider {
    companion object : ProjectExtensionDescriptor<CjGeneratedReferenceSearchProvider>(
        "org.cangnova.cangjie.generatedReferenceSearchProvider",
        CjGeneratedReferenceSearchProvider::class.java,
    )

    /**
     * 向 [consumer] 提交 [scope] 内的生成引用；返回 `false` 表示 consumer 已停止搜索。
     */
    fun processGeneratedReferences(
        project: Project,
        scope: SearchScope,
        consumer: Processor<in CjGeneratedReference>,
    ): Boolean
}
