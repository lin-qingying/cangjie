package org.cangnova.cangjie.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.utils.firstIsInstance
import org.cangnova.cangjie.utils.firstIsInstanceOrNull

/**
 * 对齐 Kotlin `idea.references.referenceUtils` 的统一引用入口。
 *
 * 上层 analysis / usages / rename / target extraction 不应自行猜“这个 PSI 应该取哪个 reference”，
 * 而应通过这里拿到当前元素的主引用。
 */
val CjSimpleNameExpression.mainReference: CjSimpleNameReference
    get() = references.firstIsInstance()

val CjReferenceExpression.mainReference: CjReference?
    get() = if (this is CjSimpleNameExpression) mainReference else references.firstIsInstance()

val CjBasicType.mainReference: PsiReference?
    get() = references.firstOrNull()

val CjValueArgumentName.mainReference: PsiReference?
    get() = references.firstOrNull()

val CjSuperTypeCallEntry.mainReference: PsiReference?
    get() = references.firstOrNull()
val CjElement.mainReference: CjReference?
    get() = when (this) {
        is CjReferenceExpression -> mainReference
        is CDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }
val CDocName.mainReference: CDocReference
    get() = references.firstIsInstance()


val PsiReference.unwrappedTargets: Set<PsiElement>
    get() = when (this) {
        is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(linkedSetOf()) { result -> result.element }
        else -> listOfNotNull(resolve()).toSet()
    }
