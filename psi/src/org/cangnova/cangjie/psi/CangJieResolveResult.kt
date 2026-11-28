package org.cangnova.cangjie.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult

/**
 * 同一次语义解析的声明身份与导航位置。
 *
 * [canonicalTarget] 是真实声明；[getElement] 保持平台 resolve 的导航契约。
 * 一对多宏输出可以共享导航位置，但必须保留各自的规范声明。
 */
class CangJieResolveResult(
    val canonicalTarget: PsiElement,
    navigationTarget: PsiElement = canonicalTarget,
    private val validity: () -> Boolean = { true },
) : PsiElementResolveResult(navigationTarget) {
    /** 只读会话有效性由产生结果的 API 层提供；PSI 层不保存 CFIR。 */
    val isCurrent: Boolean
        get() = validity() && canonicalTarget.isValid && element?.isValid == true

    override fun isValidResult(): Boolean = isCurrent
}

/** 普通平台结果的 element 就是声明；仓颉结果显式保存独立的声明身份。 */
val ResolveResult.cangJieCanonicalTarget: PsiElement?
    get() = if (this is CangJieResolveResult) canonicalTarget else element

/** Symbol、搜索与 evaluator 共用 ResolveCache 已发布的声明结果，不重新解析导航位置。 */
fun PsiReference.cangJieCanonicalTargets(): Set<PsiElement> = when (this) {
    is PsiPolyVariantReference -> multiResolve(false)
        .filter(ResolveResult::isValidResult)
        .mapNotNullTo(linkedSetOf()) { it.cangJieCanonicalTarget }
    else -> listOfNotNull(resolve()).toSet()
}
