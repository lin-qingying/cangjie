package org.cangnova.cangjie.analysis.api.platform

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.utils.mergeOnly

@CaPlatformInterface
class CaCompositeProviderFactory<P : CangJieComposableProvider>(
    private val emptyProvider: P,
    private val composeProviders: (List<P>) -> P,
) {
    fun create(providers: List<P>): P = when (providers.size) {
        0 -> emptyProvider
        1 -> providers.single()
        else -> composeProviders(providers)
    }

    fun createFlattened(providers: List<P>): P =
        create(if (providers.size > 1) flatten(providers) else providers)

    fun flatten(providers: List<P>): List<P> =
        providers.flatMap { provider ->
            @Suppress("UNCHECKED_CAST")
            when (provider) {
                is CangJieCompositeProvider<*> -> (provider as CangJieCompositeProvider<P>).providers
                else -> listOf(provider)
            }
        }
}

@CaPlatformInterface
inline fun <P : CangJieComposableProvider, reified T : P> List<P>.mergeSpecificProviders(
    factory: CaCompositeProviderFactory<P>,
    crossinline mergeTargets: (List<T>) -> P,
): P {
    return factory.createFlattened(factory.flatten(this).mergeOnly<_, T> { mergeTargets(it) })
}
