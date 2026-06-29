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
    /**
     * 将 PSI 元素包装为可被 IntelliJ Symbol API 识别的基础 symbol。
     */
    override fun asSymbol(element: PsiElement): Symbol {
        return PsiElementBackedSymbol(element)
    }

    /**
     * 将传统 `PsiReference` 包装为 `PsiSymbolReference`。
     */
    override fun asSymbolReference(reference: PsiReference): PsiSymbolReference {
        return PsiReferenceBackedSymbolReference(reference, this)
    }

    /**
     * 从本服务创建的 symbol 中取回原始 PSI 元素。
     */
    override fun extractElementFromSymbol(symbol: Symbol): PsiElement? {
        return (symbol as? PsiElementBackedSymbol)?.element
    }

    /**
     * 直接持有 PSI 元素的 headless symbol 实现。
     */
    private data class PsiElementBackedSymbol(
        /**
         * 当前 symbol 代表的 PSI 元素。
         */
        val element: PsiElement,
    ) : Symbol {
        /**
         * 创建硬引用指针，headless 环境中不做额外索引恢复。
         */
        override fun createPointer(): Pointer<out Symbol> {
            return Pointer.hardPointer(this)
        }
    }

    /**
     * 基于传统 `PsiReference` 的 `PsiSymbolReference` 适配器。
     */
    private class PsiReferenceBackedSymbolReference(
        /**
         * 被适配的传统 PSI reference。
         */
        private val delegate: PsiReference,
        /**
         * 用于把解析结果 PSI 元素转换为 symbol 的服务。
         */
        private val symbolService: PsiSymbolService,
    ) : PsiSymbolReference {
        /**
         * 返回 reference 所属的 PSI 元素。
         */
        override fun getElement(): PsiElement = delegate.element

        /**
         * 返回 reference 在宿主元素中的文本范围。
         */
        override fun getRangeInElement(): TextRange = delegate.rangeInElement

        /**
         * 解析底层 reference，并把解析到的 PSI 元素转换为 Symbol 集合。
         */
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
    /**
     * 使用空 hint 获取元素可见的全部 symbol reference。
     */
    override fun getReferences(element: PsiElement): Collection<out PsiSymbolReference> {
        return getReferences(element, EMPTY_HINTS)
    }

    /**
     * 获取指定 reference 类型的 symbol reference，并保持返回类型收窄。
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : PsiSymbolReference> getReferences(
        host: PsiElement,
        referenceClass: Class<T>,
    ): Collection<T> {
        return getReferences(host, PsiSymbolReferenceHints.referenceClassHint(referenceClass)) as Collection<T>
    }

    /**
     * 获取元素的 own/external symbol reference，并应用类型、offset 和 target 过滤条件。
     */
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

    /**
     * 只读取外部 reference provider 暴露的 symbol reference。
     */
    override fun getExternalReferences(
        host: PsiExternalReferenceHost,
        hints: PsiSymbolReferenceHints,
    ): Collection<out PsiSymbolReference> {
        return applyHints(doGetExternalReferences(host, hints), hints)
    }

    /**
     * 读取 PSI 元素自身暴露的 `ownReferences`，并按 PSI 修改计数缓存。
     */
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

    /**
     * 从 `psi.symbolReferenceProvider` 扩展点收集适用于当前宿主元素的外部 references。
     */
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

    /**
     * 根据 hint 对 references 做统一过滤。
     */
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

    /**
     * 判断扩展点 bean 声明的 reference 类型是否满足调用方要求。
     */
    private fun matchesReferenceClass(
        bean: PsiSymbolReferenceProviderBean,
        requiredReferenceClass: Class<out PsiSymbolReference>,
    ): Boolean {
        return requiredReferenceClass == PsiSymbolReference::class.java ||
            bean.anyReferenceClass ||
            requiredReferenceClass.isAssignableFrom(bean.getReferenceClass())
    }

    /**
     * 判断 provider 语言是否适用于当前元素语言，支持 meta-language 和 base-language 链。
     */
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
        /**
         * 不带过滤条件的空 hint 实例。
         */
        private val EMPTY_HINTS = object : PsiSymbolReferenceHints {}

        /**
         * IntelliJ 外部 symbol reference provider 扩展点。
         */
        private val SYMBOL_REFERENCE_PROVIDER_EP =
            ExtensionPointName.create<PsiSymbolReferenceProviderBean>("com.intellij.psi.symbolReferenceProvider")

        /**
         * 缓存 PSI own references 的 parameterized cached value key。
         */
        private val OWN_REFERENCES_KEY =
            Key.create<ParameterizedCachedValue<List<PsiSymbolReference>, PsiElement>>(
                "CangJieHeadlessPsiSymbolReferenceService.OWN_REFERENCES",
            )

        /**
         * 根据 PSI 修改计数缓存 own references 的 provider。
         */
        private val OWN_REFERENCES_PROVIDER =
            ParameterizedCachedValueProvider<List<PsiSymbolReference>, PsiElement> { element ->
                val references = element.ownReferences.toList()
                CachedValueProvider.Result.create(
                    references,
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            }

        /**
         * 当前 application 中已经注册的外部 symbol reference provider bean 列表。
         */
        private val externalReferenceProviderBeans: List<PsiSymbolReferenceProviderBean>
            get() = SYMBOL_REFERENCE_PROVIDER_EP.extensionList
    }
}
