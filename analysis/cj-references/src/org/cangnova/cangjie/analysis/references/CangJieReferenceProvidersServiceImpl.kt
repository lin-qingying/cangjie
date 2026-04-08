package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor.ReferenceProvider

/**
 * 对齐 Kotlin `KotlinReferenceProvidersServiceImpl` 的 contributor 分发实现。
 *
 * 这里不再维护“手写 when + 少量特判”的中心化逻辑，而是：
 * 1. 从 `org.cangnova.cangjie.psiReferenceProvider` 扩展点收集 contributor；
 * 2. 按 PSI 元素类型分发 provider；
 * 3. 统一缓存 provider 产出的 `PsiReference[]`。
 */
internal class CangJieReferenceProvidersServiceImpl(
    private val project: Project,
) : CangJieReferenceProvidersService() {
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, ReferenceProvider<CjElement>> by lazy {
        MultiMap<Class<out PsiElement>, ReferenceProvider<CjElement>>(LinkedHashMap()).apply {
            EP_NAME.getExtensionList(project).forEach { contributor ->
                runSafely {
                    putValue(contributor.elementClass, contributor.referenceProvider)
                }
            }
        }
    }

    private val providersBindingCache: Map<Class<out PsiElement>, List<ReferenceProvider<CjElement>>> =
        ConcurrentFactoryMap.createMap { klass ->
            buildList {
                for (bindingClass in originalProvidersBinding.keySet()) {
                    if (bindingClass.isAssignableFrom(klass)) {
                        addAll(originalProvidersBinding[bindingClass])
                    }
                }
            }
        }

    private fun doGetCangJieReferencesFromProviders(context: CjElement): Array<PsiReference> {
        val providers = providersBindingCache[context.javaClass]
        if (providers.isNullOrEmpty()) return PsiReference.EMPTY_ARRAY

        val result = buildList {
            for (provider in providers) {
                runSafely {
                    addAll(provider(context))
                }
            }
        }

        return if (result.isEmpty()) PsiReference.EMPTY_ARRAY else result.toTypedArray()
    }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> = when (psiElement) {
        is ContributedReferenceHost -> ReferenceProvidersRegistry.getReferencesFromProviders(psiElement)
        !is CjElement -> PsiReference.EMPTY_ARRAY
        else -> CachedValuesManager.getCachedValue(psiElement) {
            CachedValueProvider.Result.create(
                doGetCangJieReferencesFromProviders(psiElement),
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }
    }

    private companion object {
        val LOG: Logger = Logger.getInstance(CangJieReferenceProvidersServiceImpl::class.java)

        val EP_NAME: ExtensionPointName<CangJiePsiReferenceProviderContributor<in CjElement>> = ExtensionPointName(
            "org.cangnova.cangjie.psiReferenceProvider",
        )
    }

    private inline fun runSafely(action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
            LOG.error(t)
        }
    }
}
