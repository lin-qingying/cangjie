package org.cangnova.cangjie.analysis.api.platform

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.utils.mergeOnly

/**
 * 组合多个平台 provider 的通用工厂。
 */
@CaPlatformInterface
class CaCompositeProviderFactory<P : CangJieComposableProvider>(
    /**
     * provider 列表为空时返回的空 provider。
     */
    private val emptyProvider: P,
    /**
     * 将多个 provider 合成为一个 provider 的函数。
     */
    private val composeProviders: (List<P>) -> P,
) {
    /**
     * 根据 provider 数量创建最小必要的 provider 实例。
     */
    fun create(providers: List<P>): P = when (providers.size) {
        0 -> emptyProvider
        1 -> providers.single()
        else -> composeProviders(providers)
    }

    /**
     * 先展开嵌套 composite provider，再创建组合 provider。
     */
    fun createFlattened(providers: List<P>): P =
        create(if (providers.size > 1) flatten(providers) else providers)

    /**
     * 将列表中的 composite provider 展开为直接 provider 列表。
     */
    fun flatten(providers: List<P>): List<P> =
        providers.flatMap { provider ->
            @Suppress("UNCHECKED_CAST")
            when (provider) {
                is CangJieCompositeProvider<*> -> (provider as CangJieCompositeProvider<P>).providers
                else -> listOf(provider)
            }
        }
}

/**
 * 在 provider 列表中合并指定实现类型 [T]，再用 [factory] 构造最终 provider。
 */
@CaPlatformInterface
inline fun <P : CangJieComposableProvider, reified T : P> List<P>.mergeSpecificProviders(
    factory: CaCompositeProviderFactory<P>,
    crossinline mergeTargets: (List<T>) -> P,
): P {
    return factory.createFlattened(factory.flatten(this).mergeOnly<_, T> { mergeTargets(it) })
}
