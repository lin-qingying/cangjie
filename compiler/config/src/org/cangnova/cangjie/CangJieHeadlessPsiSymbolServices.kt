package org.cangnova.cangjie

import com.intellij.lang.Language
import com.intellij.lang.MetaLanguage
import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiExternalReferenceHost
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceHints
import com.intellij.model.psi.PsiSymbolReferenceProviderBean
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.model.psi.PsiSymbolService
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue
import com.intellij.psi.util.ParameterizedCachedValueProvider
import com.intellij.psi.util.PsiModificationTracker

/**
 * 仓颉 headless 环境下对 IntelliJ Symbol API 的基础服务实现。
 *
 * Kotlin 编译器运行在更完整的平台容器里，因此不会手工补 `PsiSymbolService`；
 * 仓颉当前仍使用自建 headless 宿主，所以这层必须由环境统一提供。
 */
internal class CangJieHeadlessPsiSymbolService : PsiSymbolService {
    override fun asSymbol(element: PsiElement): Symbol {
        return PsiElementBackedSymbol(element)
    }

    override fun asSymbolReference(reference: PsiReference): PsiSymbolReference {
        return PsiReferenceBackedSymbolReference(reference, this)
    }

    override fun extractElementFromSymbol(symbol: Symbol): PsiElement? {
        return (symbol as? PsiElementBackedSymbol)?.element
    }

    private data class PsiElementBackedSymbol(
        val element: PsiElement,
    ) : Symbol {
        override fun createPointer(): Pointer<out Symbol> {
            return Pointer.hardPointer(this)
        }
    }

    private class PsiReferenceBackedSymbolReference(
        private val delegate: PsiReference,
        private val symbolService: PsiSymbolService,
    ) : PsiSymbolReference {
        override fun getElement(): PsiElement = delegate.element

        override fun getRangeInElement(): TextRange = delegate.rangeInElement

        override fun resolveReference(): Collection<Symbol> {
            val resolvedElements = when (delegate) {
                is PsiPolyVariantReference -> delegate.multiResolve(false).mapNotNull { result -> result.element }
                else -> listOfNotNull(delegate.resolve())
            }
            return resolvedElements.map(symbolService::asSymbol)
        }
    }
}

/**
 * 仓颉 headless 环境下的 `PsiSymbolReferenceService`。
 *
 * 这里不再停留在“只看 ownReferences 的最小实现”，而是对齐 IntelliJ
 * `PsiSymbolReferenceServiceImpl` 的职责边界：
 * 1. 读取并缓存 PSI 自身暴露的 own references
 * 2. 支持 `psi.symbolReferenceProvider` 扩展点提供的 external references
 * 3. 对 offset / referenceClass / target hints 做统一过滤
 */
internal class CangJieHeadlessPsiSymbolReferenceService : PsiSymbolReferenceService {
    override fun getReferences(element: PsiElement): Collection<out PsiSymbolReference> {
        return getReferences(element, EMPTY_HINTS)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : PsiSymbolReference> getReferences(
        host: PsiElement,
        referenceClass: Class<T>,
    ): Collection<T> {
        return getReferences(host, PsiSymbolReferenceHints.referenceClassHint(referenceClass)) as Collection<T>
    }

    override fun getReferences(
        element: PsiElement,
        hints: PsiSymbolReferenceHints,
    ): Collection<out PsiSymbolReference> {
        val ownReferences = doGetOwnReferences(element)
        val references = if (ownReferences.isNotEmpty()) {
            ownReferences
        } else if (element is PsiExternalReferenceHost) {
            doGetExternalReferences(element, hints)
        } else {
            emptyList()
        }

        return applyHints(references, hints)
    }

    override fun getExternalReferences(
        host: PsiExternalReferenceHost,
        hints: PsiSymbolReferenceHints,
    ): Collection<out PsiSymbolReference> {
        return applyHints(doGetExternalReferences(host, hints), hints)
    }

    private fun doGetOwnReferences(
        element: PsiElement,
    ): List<PsiSymbolReference> {
        val cachedValuesManager = CachedValuesManager.getManager(element.project)
        return cachedValuesManager.getParameterizedCachedValue(
            element,
            OWN_REFERENCES_KEY,
            OWN_REFERENCES_PROVIDER,
            false,
            element,
        )
    }

    private fun doGetExternalReferences(
        element: PsiExternalReferenceHost,
        hints: PsiSymbolReferenceHints,
    ): List<PsiSymbolReference> {
        val producer = {
            externalReferenceProviderBeans
                .asSequence()
                .filter { bean -> isApplicableLanguage(bean.getHostLanguage(), element.language) }
                .filter { bean -> bean.getHostElementClass().isAssignableFrom(element.javaClass) }
                .filter { bean -> matchesReferenceClass(bean, hints.referenceClass) }
                .flatMap { bean -> bean.instance.getReferences(element, hints).asSequence() }
                .toList()
        }

        if (hints === EMPTY_HINTS) {
            return CachedValuesManager.getCachedValue(element) {
                CachedValueProvider.Result.create(
                    producer(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            }
        }

        return producer()
    }

    private fun applyHints(
        references: List<PsiSymbolReference>,
        hints: PsiSymbolReferenceHints,
    ): List<PsiSymbolReference> {
        if (hints === EMPTY_HINTS) {
            return references
        }

        var result = references

        val referenceClass = hints.referenceClass
        if (referenceClass != PsiSymbolReference::class.java) {
            result = result.filter(referenceClass::isInstance)
        }

        val offsetInElement = hints.offsetInElement
        if (offsetInElement >= 0) {
            result = result.filter { reference -> reference.rangeInElement.containsOffset(offsetInElement) }
        }

        val target = hints.target
        if (target != null) {
            result = result.filter { reference -> reference.resolvesTo(target) }
        }

        return result
    }

    private fun matchesReferenceClass(
        bean: PsiSymbolReferenceProviderBean,
        requiredReferenceClass: Class<out PsiSymbolReference>,
    ): Boolean {
        return requiredReferenceClass == PsiSymbolReference::class.java ||
            bean.anyReferenceClass ||
            requiredReferenceClass.isAssignableFrom(bean.getReferenceClass())
    }

    private fun isApplicableLanguage(
        providerLanguage: Language,
        elementLanguage: Language,
    ): Boolean {
        if (providerLanguage is MetaLanguage) {
            return providerLanguage.matchesLanguage(elementLanguage)
        }

        var current: Language? = elementLanguage
        while (current != null) {
            if (current == providerLanguage) {
                return true
            }
            current = current.baseLanguage
        }

        return false
    }

    private companion object {
        private val EMPTY_HINTS = object : PsiSymbolReferenceHints {}

        private val SYMBOL_REFERENCE_PROVIDER_EP =
            ExtensionPointName.create<PsiSymbolReferenceProviderBean>("com.intellij.psi.symbolReferenceProvider")

        private val OWN_REFERENCES_KEY =
            Key.create<ParameterizedCachedValue<List<PsiSymbolReference>, PsiElement>>(
                "CangJieHeadlessPsiSymbolReferenceService.OWN_REFERENCES",
            )

        private val OWN_REFERENCES_PROVIDER =
            ParameterizedCachedValueProvider<List<PsiSymbolReference>, PsiElement> { element ->
                val references = element.ownReferences.toList()
                CachedValueProvider.Result.create(
                    references,
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            }

        private val externalReferenceProviderBeans: List<PsiSymbolReferenceProviderBean>
            get() = SYMBOL_REFERENCE_PROVIDER_EP.extensionList
    }
}
