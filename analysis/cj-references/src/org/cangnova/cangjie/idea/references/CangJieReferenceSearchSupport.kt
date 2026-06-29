package org.cangnova.cangjie.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration

/**
 * 仓颉 references / find usages / navigation 共享的目标匹配协议。
 *
 * Kotlin `kt-references` 的核心价值不是“有很多 reference 类”，而是：
 * 1. 主引用选择统一；
 * 2. `isReferenceTo()` 语义统一；
 * 3. usages 搜索与引用解析共用同一套 target identity。
 *
 * 仓颉没有 Java PSI / light classes，但依然需要把 source-backed navigation、
 * import alias、original/navigation element 这些语义统一到一个位置，避免
 * search executor、reference 基类、测试各自维护一份“目标相等性”逻辑。
 */
internal object CangJieReferenceSearchSupport {
    /**
     * 计算目标元素在 usages 搜索中应使用的基础名称集合。
     *
     * 同时考虑元素本身、original element 和 navigation element，覆盖 source-backed 与 wrapper-backed 目标。
     */
    fun baseSearchNames(target: PsiElement): Set<String> {
        return linkedSetOf<String>().apply {
            addNamedElement(target)
            addNamedElement(target.originalElement.takeIf { it != target })
            addNamedElement(target.navigationElement.takeIf { it != target })
        }
    }

    /**
     * 对单个文件扩展搜索名集合。
     *
     * 当目标声明被 `import ... as alias` 引入时，`find usages` 搜索原声明也必须能命中
     * alias 调用位，否则“引用可解析、但 usages 搜不到”的链路会断掉。
     */
    fun searchNamesForFile(
        file: CjFile,
        target: PsiElement,
        baseNames: Set<String>,
    ): Set<String> {
        if (target !is CjNamedDeclaration) {
            return baseNames
        }

        val fqName = target.fqName ?: return baseNames
        val aliasName = file.findAliasByFqName(fqName)?.name
        if (aliasName.isNullOrBlank()) {
            return baseNames
        }

        return linkedSetOf<String>().apply {
            addAll(baseNames)
            add(aliasName)
        }
    }

    /**
     * 判断 reference 公开的可能解析名称是否与搜索名集合有交集。
     */
    fun mayResolveByName(
        reference: PsiReference,
        searchNames: Set<String>,
    ): Boolean {
        val resolvesByNames = (reference as? CjReference)
            ?.resolvesByNames
            ?.map { name -> name.asString() }
            ?.filter(String::isNotBlank)
            .orEmpty()

        return resolvesByNames.isEmpty() || resolvesByNames.any(searchNames::contains)
    }

    /**
     * 判断单个 reference 是否匹配目标元素。
     */
    fun matchesTarget(
        reference: PsiReference,
        target: PsiElement,
    ): Boolean {
        if (reference.isReferenceTo(target)) {
            return true
        }

        return matchesResolvedTargets(reference.unwrappedTargets, target)
    }

    /**
     * 判断一组解析目标是否与候选目标等价。
     */
    fun matchesResolvedTargets(
        resolvedTargets: Set<PsiElement>,
        candidateTarget: PsiElement,
    ): Boolean {
        val candidateChain = candidateTarget.referenceIdentityChain()
        return resolvedTargets.any { resolved ->
            val resolvedChain = resolved.referenceIdentityChain()
            resolvedChain.any { resolvedIdentity ->
                candidateChain.any { candidateIdentity ->
                    areEquivalent(resolvedIdentity, candidateIdentity)
                }
            }
        }
    }

    /**
     * 构造用于 reference 目标等价判断的 PSI 身份链。
     */
    private fun PsiElement.referenceIdentityChain(): Set<PsiElement> {
        val identities = linkedSetOf<PsiElement>()

        fun collect(element: PsiElement?) {
            if (element == null || !identities.add(element)) {
                return
            }

            val original = element.originalElement
            if (original != element) {
                collect(original)
            }

            val navigation = element.navigationElement
            if (navigation != element) {
                collect(navigation)
            }
        }

        collect(this)
        return identities
    }

    /**
     * 判断两个 PSI 元素是否可视为同一个引用目标。
     */
    private fun areEquivalent(
        left: PsiElement,
        right: PsiElement,
    ): Boolean {
        if (left == right) {
            return true
        }

        if (left.manager == right.manager && left.manager.areElementsEquivalent(left, right)) {
            return true
        }

        return left.containingFile == right.containingFile &&
            left.textRange == right.textRange &&
            left::class == right::class
    }

    /**
     * 将具名 PSI 元素的名称加入搜索名集合。
     */
    private fun MutableSet<String>.addNamedElement(element: PsiElement?) {
        val name = (element as? PsiNamedElement)?.name
        if (!name.isNullOrBlank()) {
            add(name)
        }
    }
}
