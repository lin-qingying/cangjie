package org.cangnova.cangjie.idea.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
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
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.startOffset

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
    /**
     * 当前引用使用的 IntelliJ resolve cache resolver。
     */
    val resolver: ResolveCache.PolyVariantResolver<CjReference>

    /**
     * 当前引用可能解析到的名称集合，用于 usages 搜索的快速预过滤。
     */
    val resolvesByNames: Collection<Name>
        get() = emptyList()

    /**
     * 返回承载该引用的仓颉 PSI 元素。
     */
    override fun getElement(): CjElement
}

/**
 * 仓颉 PSI reference 的基础实现。
 */
abstract class AbstractCjReference<T : CjElement>(
    /**
     * 当前引用绑定的仓颉 PSI 元素。
     */
    element: T,
) : PsiPolyVariantReferenceBase<T>(element), CjReference {
    /**
     * 当前引用表达式的强类型别名。
     */
    val expression: T
        get() = element

    /**
     * 通过 IntelliJ resolve cache 执行多目标解析。
     */
    final override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    /**
     * 返回引用的规范文本，默认使用承载表达式文本。
     */
    override fun getCanonicalText(): String = expression.text

    /**
     * 当前引用是否支持 rename。
     */
    open fun canRename(): Boolean = false

    /**
     * 判断候选目标是否允许进入当前引用的目标匹配流程。
     */
    protected open fun canBeReferenceTo(candidateTarget: PsiElement): Boolean = true

    /**
     * 判断当前引用是否指向指定 import alias。
     */
    protected open fun isReferenceToImportAlias(alias: CjImportAlias): Boolean = false

    /**
     * 判断当前引用是否指向候选目标元素。
     */
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

    /**
     * 执行 rename 操作。
     */
    override fun handleElementRename(newElementName: String): PsiElement? {
        if (!canRename()) {
            throw IncorrectOperationException("${this::class.java.simpleName} does not support rename")
        }
        return getCangJieReferenceMutateService().handleElementRename(this, newElementName)
    }

    /**
     * 将当前引用绑定到目标 PSI 元素。
     */
    override fun bindToElement(element: PsiElement): PsiElement {
        return getCangJieReferenceMutateService().bindToElement(this, element)
    }

    /**
     * 返回代码补全候选；基础实现不提供变体。
     */
    @Suppress("UNCHECKED_CAST")
    override fun getVariants(): Array<Any> = PsiReference.EMPTY_ARRAY as Array<Any>

    /**
     * 仓颉引用默认不是 soft reference。
     */
    override fun isSoft(): Boolean = false

    /**
     * 返回便于调试的引用描述。
     */
    override fun toString(): String = "${this::class.java.simpleName}: ${expression.text}"

    /**
     * 子类提供的真实目标元素解析结果。
     */
    protected abstract fun resolveTargetElements(): Collection<PsiElement>

    /**
     * 将多目标解析结果收敛为单目标 resolve。
     */
      override fun resolve(): PsiElement? = resolveTargetElements().singleOrNull()

    /**
     * 基于 [resolveTargetElements] 的默认 resolve cache resolver。
     */
    override val resolver: ResolveCache.PolyVariantResolver<CjReference> =
        ResolveCache.PolyVariantResolver { _, _ ->
            this.resolveTargetElements()
                .map { target -> PsiElementResolveResult(target) }
                .toTypedArray()
        }

    /**
     * 取得 application 级 reference mutation 服务。
     */
    protected fun getCangJieReferenceMutateService(): CangJieReferenceMutateService {
        return ApplicationManager.getApplication().getService(CangJieReferenceMutateService::class.java)
            ?: error("Cannot handle element rename because CangJieReferenceMutateService is missing")
    }
}

/**
 * 单目标仓颉引用基类。
 */
abstract class CjSimpleReference<T : CjElement>(
    expression: T,
) : AbstractCjReference<T>(expression)

/**
 * 多目标仓颉引用基类。
 */
abstract class CjMultiReference<T : CjElement>(expression: T) : AbstractCjReference<T>(expression)

/**
 * 仓颉 simple-name reference 基类。
 */
abstract class CjSimpleNameReference(
    expression: CjSimpleNameExpression,
) : CjSimpleReference<CjSimpleNameExpression>(expression) {
    /**
     * simple name 支持 rename，但自增/自减运算符 token 不作为可重命名名称。
     */
    override fun canRename(): Boolean {
        val elementType = expression.referencedNameElementType
        return elementType != CjTokens.PLUSPLUS && elementType != CjTokens.MINUSMINUS
    }

    /**
     * 返回 simple name 在表达式内部的引用范围。
     */
    override fun getRangeInElement(): TextRange {
        val element = element.referencedNameElement
        val startOffset = getElement().startOffset
        return element.textRange.shiftRight(-startOffset)
    }

    /**
     * 引用绑定后导入缩短处理模式。
     */
    enum class ShorteningMode {
        /** 不执行导入缩短。 */
        NO_SHORTENING,

        /** 延迟到外层后处理阶段执行导入缩短。 */
        DELAYED_SHORTENING,

        /** 立即强制执行导入缩短。 */
        FORCED_SHORTENING,
    }

    /**
     * 将 simple-name 引用绑定到目标 PSI 元素。
     */
    fun bindToElement(
        element: PsiElement,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
    ): PsiElement {
        return getCangJieReferenceMutateService().bindToElement(this, element, shorteningMode)
    }

    /**
     * 将 simple-name 引用绑定到指定全限定名。
     */
    fun bindToFqName(
        fqName: FqName,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null,
    ): PsiElement {
        return getCangJieReferenceMutateService().bindToFqName(this, fqName, shorteningMode, targetElement)
    }

    /**
     * 返回当前 simple-name 引用使用的 import alias。
     */
    abstract fun getImportAlias(): CjImportAlias?

    /**
     * 判断当前引用是否指向指定 import alias。
     */
    override fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        val importAlias = getImportAlias() ?: return false
        return CangJieReferenceSearchSupport.matchesResolvedTargets(setOf(importAlias), alias)
    }
}
