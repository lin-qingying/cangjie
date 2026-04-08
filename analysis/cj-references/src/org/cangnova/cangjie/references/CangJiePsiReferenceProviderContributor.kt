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
    fun interface ReferenceProvider<in T : CjElement> : (T) -> List<PsiReference>

    val elementClass: Class<out T>

    val referenceProvider: ReferenceProvider<T>
}
