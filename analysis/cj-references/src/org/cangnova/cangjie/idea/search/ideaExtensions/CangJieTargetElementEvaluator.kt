package org.cangnova.cangjie.idea.search.ideaExtensions

import com.intellij.codeInsight.TargetElementEvaluatorEx
import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilExtender
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import org.cangnova.cangjie.idea.references.CjReference
import org.cangnova.cangjie.idea.references.unwrappedTargets
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CangJieResolveResult
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.psiUtil.parentOfType
import com.intellij.psi.PsiPolyVariantReference
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjCatchParameter
import org.cangnova.cangjie.psi.CjCommandTypePattern
import org.cangnova.cangjie.psi.CjTryResource
import org.cangnova.cangjie.psi.CjParameter

/** 平台导航消费规范声明决议，模式语法节点不能凭名字被当成变量声明。 */
class CangJieTargetElementEvaluator : TargetElementEvaluatorEx2(), TargetElementEvaluatorEx, TargetElementUtilExtender {
    override fun getAdditionalDefinitionSearchFlags(): Int = 0

    override fun getAdditionalReferenceSearchFlags(): Int = 0

    override fun getAllAdditionalFlags(): Int = 0

    /** 先消费真实声明决议，资源名的内部 parameter 不能成为平台导航身份。 */
    override fun getNamedElement(element: PsiElement): PsiElement? =
        CangJieReferenceProvidersService.getInstance(element.project).getNamedDeclarationTarget(element)

    override fun isAcceptableNamedParent(parent: PsiElement): Boolean = when (parent) {
        // 平台在声明决议缺失时也不能把资源的语法包装参数发布为形参。
        is CjParameter -> parent.parent !is CjTryResource && super.isAcceptableNamedParent(parent)
        is CjNamedPattern, is CjCatchParameter, is CjCommandTypePattern, is CjTryResource ->
            CangJieReferenceProvidersService.getInstance(parent.project).getNamedDeclarationTarget(parent) === parent
        else -> super.isAcceptableNamedParent(parent)
    }

    override fun getElementByReference(ref: PsiReference, flags: Int): PsiElement? {
        if (ref !is CjReference || flags and TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED == 0) return null
        val target = ref.unwrappedTargets.singleOrNull() ?: return null
        return CangJieReferenceProvidersService.getInstance(target.project).getNamedDeclarationTarget(target)
    }

    override fun getTargetCandidates(ref: PsiReference): Collection<PsiElement>? =
        if (ref is CjReference) ref.unwrappedTargets else super.getTargetCandidates(ref)

    /** Go to Declaration 使用已选定声明对应的导航位置，Find Usages 仍保留声明身份。 */
    override fun getGotoDeclarationTarget(sourceElement: PsiElement, targetElement: PsiElement?): PsiElement? {
        val target = targetElement ?: return null
        val expression = sourceElement.parentOfType<CjReferenceExpression>(withSelf = true) ?: return null
        val reference = expression.mainReference as? PsiPolyVariantReference ?: return null
        return reference.multiResolve(false).filterIsInstance<CangJieResolveResult>()
            .singleOrNull { result ->
                result.isValidResult && target.manager.areElementsEquivalent(result.canonicalTarget, target)
            }?.element
    }

    override fun isIdentifierPart(file: PsiFile, text: CharSequence, offset: Int): Boolean =
        file.findElementAt(offset)?.node?.elementType in setOf(CjTokens.IDENTIFIER, CjTokens.FIELD_IDENTIFIER)
}
