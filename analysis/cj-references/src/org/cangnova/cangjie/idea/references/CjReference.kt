package org.cangnova.cangjie.idea.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.PsiElementResolveResult
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * 对齐 Kotlin `KtReference` 的仓颉引用基础设施。
 *
 * `cj-references` 不应只提供一批孤立的 `PsiReferenceBase` 子类，
 * 而要有统一的 reference 抽象承载：
 * 1. resolve / multiResolve；
 * 2. rename / bind；
 * 3. 后续 references / rename / usages 的共同协议。
 */
interface CjReference : PsiPolyVariantReference {
    val resolver: ResolveCache.PolyVariantResolver<CjReference>

    val resolvesByNames: Collection<Name>
        get() = emptyList()

    override fun getElement(): CjElement
}

abstract class AbstractCjReference<T : CjElement>(
    element: T,
) : PsiPolyVariantReferenceBase<T>(element), CjReference {
    val expression: T
        get() = element

    final override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    override fun getCanonicalText(): String = expression.text

    open fun canRename(): Boolean = false

    protected open fun canBeReferenceTo(candidateTarget: PsiElement): Boolean = true

    protected open fun isReferenceToImportAlias(alias: CjImportAlias): Boolean = false

    override fun isReferenceTo(candidateTarget: PsiElement): Boolean {
        if (!canBeReferenceTo(candidateTarget)) return false

        if (
            candidateTarget is CjImportAlias &&
            this is CjSimpleNameReference &&
            candidateTarget.name == expression.referencedName
        ) {
            return isReferenceToImportAlias(candidateTarget)
        }

        return CangJieReferenceSearchSupport.matchesResolvedTargets(unwrappedTargets, candidateTarget)
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        if (!canRename()) {
            throw IncorrectOperationException("${this::class.java.simpleName} does not support rename")
        }
        return getCangJieReferenceMutateService().handleElementRename(this, newElementName)
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        return getCangJieReferenceMutateService().bindToElement(this, element)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getVariants(): Array<Any> = PsiReference.EMPTY_ARRAY as Array<Any>

    override fun isSoft(): Boolean = false

    override fun toString(): String = "${this::class.java.simpleName}: ${expression.text}"

    protected abstract fun resolveTargetElements(): Collection<PsiElement>

      override fun resolve(): PsiElement? = resolveTargetElements().singleOrNull()

    override val resolver: ResolveCache.PolyVariantResolver<CjReference> =
        ResolveCache.PolyVariantResolver { _, _ ->
            this.resolveTargetElements()
                .map { target -> PsiElementResolveResult(target) }
                .toTypedArray()
        }

    protected fun getCangJieReferenceMutateService(): CangJieReferenceMutateService {
        return ApplicationManager.getApplication().getService(CangJieReferenceMutateService::class.java)
            ?: error("Cannot handle element rename because CangJieReferenceMutateService is missing")
    }
}

abstract class CjSimpleReference<T : CjElement>(
    expression: T,
) : AbstractCjReference<T>(expression)
abstract class CjMultiReference<T : CjElement>(expression: T) : AbstractCjReference<T>(expression)

abstract class CjSimpleNameReference(
    expression: CjSimpleNameExpression,
) : CjSimpleReference<CjSimpleNameExpression>(expression) {
    enum class ShorteningMode {
        NO_SHORTENING,
        DELAYED_SHORTENING,
        FORCED_SHORTENING,
    }

    fun bindToElement(
        element: PsiElement,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
    ): PsiElement {
        return getCangJieReferenceMutateService().bindToElement(this, element, shorteningMode)
    }

    fun bindToFqName(
        fqName: FqName,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null,
    ): PsiElement {
        return getCangJieReferenceMutateService().bindToFqName(this, fqName, shorteningMode, targetElement)
    }

    abstract fun getImportAlias(): CjImportAlias?

    override fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        val importAlias = getImportAlias() ?: return false
        return CangJieReferenceSearchSupport.matchesResolvedTargets(setOf(importAlias), alias)
    }
}
