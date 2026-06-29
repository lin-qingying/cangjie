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
    /**
     * 将引用元素重命名为新的简单名称。
     */
    fun handleElementRename(cjReference: CjReference, newElementName: String): PsiElement?

    /**
     * 将引用绑定到目标 PSI 元素。
     */
    fun bindToElement(cjReference: CjReference, element: PsiElement): PsiElement

    /**
     * 将简单名称引用绑定到目标 PSI 元素，并按指定模式处理可能的导入缩短。
     */
    fun bindToElement(
        simpleNameReference: CjSimpleNameReference,
        element: PsiElement,
        shorteningMode: CjSimpleNameReference.ShorteningMode,
    ): PsiElement

    /**
     * 将简单名称引用绑定到指定全限定名。
     */
    fun bindToFqName(
        simpleNameReference: CjSimpleNameReference,
        fqName: FqName,
        shorteningMode: CjSimpleNameReference.ShorteningMode = CjSimpleNameReference.ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null,
    ): PsiElement
}

/**
 * 默认仓颉 reference mutation 服务实现。
 */
internal class CangJieReferenceMutateServiceImpl : CangJieReferenceMutateService {
    /**
     * 根据引用承载的 PSI 类型执行具体重命名。
     */
    override fun handleElementRename(cjReference: CjReference, newElementName: String): PsiElement? {
        return when (val element = cjReference.element) {
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
                "${cjReference::class.java.simpleName} does not support PSI mutation for ${element::class.java.simpleName}",
            )
        }
    }

    /**
     * 将引用绑定到具名 PSI 元素的简单名称。
     */
    override fun bindToElement(cjReference: CjReference, element: PsiElement): PsiElement {
        val targetName = (element as? PsiNamedElement)?.name
            ?: throw IncorrectOperationException("Cannot bind ${cjReference::class.java.simpleName} to unnamed PSI")
        return handleElementRename(cjReference, targetName) ?: cjReference.element
    }

    /**
     * 将简单名称引用绑定到具名 PSI 元素；当前实现只替换短名。
     */
    override fun bindToElement(
        simpleNameReference: CjSimpleNameReference,
        element: PsiElement,
        shorteningMode: CjSimpleNameReference.ShorteningMode,
    ): PsiElement {
        val targetName = (element as? PsiNamedElement)?.name
            ?: throw IncorrectOperationException("Cannot bind ${simpleNameReference::class.java.simpleName} to unnamed PSI")
        return handleElementRename(simpleNameReference, targetName) ?: simpleNameReference.element
    }

    /**
     * 将简单名称引用绑定到全限定名的短名部分。
     */
    override fun bindToFqName(
        simpleNameReference: CjSimpleNameReference,
        fqName: FqName,
        shorteningMode: CjSimpleNameReference.ShorteningMode,
        targetElement: PsiElement?,
    ): PsiElement {
        val renderedName = fqName.shortName().asString()
        return handleElementRename(simpleNameReference, renderedName) ?: simpleNameReference.element
    }
}
