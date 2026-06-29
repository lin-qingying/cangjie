package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor.ReferenceProvider
import org.cangnova.cangjie.psi.psiUtil.parentOfType

/**
 * 对齐 Kotlin `KotlinReferenceProvidersServiceImpl` 的 contributor 分发实现。
 *
 * 这里不再维护“手写 when + 少量特判”的中心化逻辑，而是：
 * 1. 从 `org.cangnova.cangjie.psiReferenceProvider` 扩展点收集 contributor；
 * 2. 按 PSI 元素类型分发 provider；
 * 3. 统一缓存 provider 产出的 `PsiReference[]`。
 */
internal class CangJieReferenceProvidersServiceImpl(
    /**
     * 用于按项目读取 reference provider 扩展点和缓存 PSI reference 的 IntelliJ project。
     */
    private val project: Project,
) : CangJieReferenceProvidersService() {
    /**
     * 扩展点声明的原始 PSI 元素类型到 provider 的绑定表。
     */
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, ReferenceProvider<CjElement>> by lazy {
        MultiMap<Class<out PsiElement>, ReferenceProvider<CjElement>>(LinkedHashMap()).apply {
            EP_NAME.getExtensionList(project).forEach { contributor ->
                runSafely {
                    putValue(contributor.elementClass, contributor.referenceProvider)
                }
            }
        }
    }

    /**
     * 按具体 PSI 运行时类型缓存可适用 provider 列表。
     *
     * 缓存会把父类/接口上注册的 provider 展开到具体元素类，避免每次取引用都扫描扩展点绑定表。
     */
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

    /**
     * 从仓颉 contributor provider 中收集指定上下文元素的 references。
     */
    private fun doGetCangJieReferencesFromProviders(context: CjElement): Array<PsiReference> {
        if (context is CjSimpleNameExpression && context.isDeclarationNameExpression()) {
            return PsiReference.EMPTY_ARRAY
        }

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

    /**
     * 返回指定 PSI 元素上的引用集合。
     *
     * IntelliJ contributed reference host 交回平台 registry；仓颉 PSI 走本模块 contributor 分发，
     * 非仓颉 PSI 直接返回空引用。
     */
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
        /**
         * reference provider 扩展点异常上报使用的 logger。
         */
        val LOG: Logger = Logger.getInstance(CangJieReferenceProvidersServiceImpl::class.java)

        /**
         * 仓颉 PSI reference provider contributor 扩展点。
         */
        val EP_NAME: ExtensionPointName<CangJiePsiReferenceProviderContributor<in CjElement>> = ExtensionPointName(
            "org.cangnova.cangjie.psiReferenceProvider",
        )
    }

    /**
     * 运行扩展点代码并把异常集中记录到日志。
     */
    private inline fun runSafely(action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
            LOG.error(t)
        }
    }
}

/**
 * 仓颉的声明名 PSI 当前会物化成 [CjSimpleNameExpression]。
 *
 * Kotlin 的声明名通常不会沿着 simple-name reference provider 这条链路分发，
 * 但仓颉这里如果不先过滤，会把 `let result = ...` 里的 `result` 错当成引用。
 * 因此统一在 reference service 入口排除 declaration-name 场景，再继续 contributor 分发。
 */
private fun CjSimpleNameExpression.isDeclarationNameExpression(): Boolean {
    val owner = parentOfType<PsiNameIdentifierOwner>(withSelf = true) ?: return false
    val nameIdentifier = owner.nameIdentifier ?: return false
    return referencedNameElement == nameIdentifier
}
