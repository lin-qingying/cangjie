package org.cangnova.cangjie.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.CjValueArgumentName

/**
 * 对齐 Kotlin `idea.references.referenceUtils` 的统一引用入口。
 *
 * 上层 analysis / usages / rename / target extraction 不应自行猜“这个 PSI 应该取哪个 reference”，
 * 而应通过这里拿到当前元素的主引用。
 */
val CjSimpleNameExpression.mainReference: PsiReference
    get() = references.first()

val CjReferenceExpression.mainReference: PsiReference?
    get() = if (this is CjSimpleNameExpression) mainReference else references.firstOrNull()

val CjBasicType.mainReference: PsiReference?
    get() = references.firstOrNull()

val CjValueArgumentName.mainReference: PsiReference?
    get() = references.firstOrNull()

val CjSuperTypeCallEntry.mainReference: PsiReference?
    get() = references.firstOrNull()

val CjElement.mainReference: PsiReference?
    get() = when (this) {
        is CjSimpleNameExpression -> mainReference
        is CjReferenceExpression -> mainReference
        is CjBasicType -> mainReference
        is CjValueArgumentName -> mainReference
        is CjSuperTypeCallEntry -> mainReference
        else -> references.firstOrNull()
    }

val PsiReference.unwrappedTargets: Set<PsiElement>
    get() = when (this) {
        is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(linkedSetOf()) { result -> result.element }
        else -> listOfNotNull(resolve()).toSet()
    }
