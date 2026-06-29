package org.cangnova.cangjie.references

import com.intellij.psi.PsiReference
import org.cangnova.cangjie.psi.CjElement

/**
 * 对齐 Kotlin `KotlinPsiReferenceProviderContributor` 的仓颉版本。
 *
 * `cj-references` 的职责不是在一个中心 service 里硬编码所有 PSI 分支，
 * 而是为不同 PSI 元素提供可独立注册、可扩展的 reference provider contributor。
 */
interface CangJiePsiReferenceProviderContributor<T : CjElement> {
    /**
     * 根据具体仓颉 PSI 元素创建 reference 列表的函数式接口。
     */
    fun interface ReferenceProvider<in T : CjElement> : (T) -> List<PsiReference>

    /**
     * 当前 contributor 处理的 PSI 元素类型。
     */
    val elementClass: Class<out T>

    /**
     * 为 [elementClass] 及其子类创建 references 的 provider。
     */
    val referenceProvider: ReferenceProvider<T>
}
