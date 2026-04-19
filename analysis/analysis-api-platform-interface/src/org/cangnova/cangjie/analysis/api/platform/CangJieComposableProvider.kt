package org.cangnova.cangjie.analysis.api.platform

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

@CaPlatformInterface
interface CangJieComposableProvider

@CaPlatformInterface
interface CangJieCompositeProvider<P : CangJieComposableProvider> : CangJieComposableProvider {
    val providers: List<P>
}

@CaPlatformInterface
interface CaComposableProviderMerger<P : CangJieComposableProvider> {
    fun merge(providers: List<P>): P
}
