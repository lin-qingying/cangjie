package org.cangnova.cangjie.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjValueArgumentName

/**
 * 对齐 Kotlin `KtReferenceMutateService` 的仓颉版本。
 *
 * 这层负责把 rename / bind 的 PSI 变更从具体 reference 实现中抽离出来，
 * 避免每个 reference 子类重复直接操作语法树。
 */
interface CangJieReferenceMutateService {
    fun handleElementRename(cangJieReference: CangJieReference, newElementName: String): PsiElement?

    fun bindToElement(cangJieReference: CangJieReference, element: PsiElement): PsiElement

    fun bindToElement(
        simpleNameReference: CangJieSimpleNameReference,
        element: PsiElement,
        shorteningMode: CangJieSimpleNameReference.ShorteningMode,
    ): PsiElement

    fun bindToFqName(
        simpleNameReference: CangJieSimpleNameReference,
        fqName: FqName,
        shorteningMode: CangJieSimpleNameReference.ShorteningMode = CangJieSimpleNameReference.ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null,
    ): PsiElement
}

internal class CangJieReferenceMutateServiceImpl : CangJieReferenceMutateService {
    override fun handleElementRename(cangJieReference: CangJieReference, newElementName: String): PsiElement? {
        return when (val element = cangJieReference.element) {
            is CjSimpleNameExpression -> {
                element.referencedNameElement.replace(CjPsiFactory(element.project).createNameIdentifier(newElementName))
                element
            }

            is CjValueArgumentName -> {
                element.referenceExpression.referencedNameElement.replace(
                    CjPsiFactory(element.project).createNameIdentifier(newElementName),
                )
                element
            }

            is CjBasicType -> {
                val replacement = CjPsiFactory(element.project).createType(newElementName).typeElement ?: return null
                element.replace(replacement)
            }

            else -> throw IncorrectOperationException(
                "${cangJieReference::class.java.simpleName} does not support PSI mutation for ${element::class.java.simpleName}",
            )
        }
    }

    override fun bindToElement(cangJieReference: CangJieReference, element: PsiElement): PsiElement {
        val targetName = (element as? PsiNamedElement)?.name
            ?: throw IncorrectOperationException("Cannot bind ${cangJieReference::class.java.simpleName} to unnamed PSI")
        return handleElementRename(cangJieReference, targetName) ?: cangJieReference.element
    }

    override fun bindToElement(
        simpleNameReference: CangJieSimpleNameReference,
        element: PsiElement,
        shorteningMode: CangJieSimpleNameReference.ShorteningMode,
    ): PsiElement {
        val targetName = (element as? PsiNamedElement)?.name
            ?: throw IncorrectOperationException("Cannot bind ${simpleNameReference::class.java.simpleName} to unnamed PSI")
        return handleElementRename(simpleNameReference, targetName) ?: simpleNameReference.element
    }

    override fun bindToFqName(
        simpleNameReference: CangJieSimpleNameReference,
        fqName: FqName,
        shorteningMode: CangJieSimpleNameReference.ShorteningMode,
        targetElement: PsiElement?,
    ): PsiElement {
        val renderedName = fqName.shortName().asString()
        return handleElementRename(simpleNameReference, renderedName) ?: simpleNameReference.element
    }
}
