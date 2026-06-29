package org.cangnova.cangjie.analysis.api.platform

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * 可参与平台层组合的 provider 标记接口。
 */
@CaPlatformInterface
interface CangJieComposableProvider

/**
 * 由多个同类 provider 组合得到的 provider。
 */
@CaPlatformInterface
interface CangJieCompositeProvider<P : CangJieComposableProvider> : CangJieComposableProvider {
    /**
     * 被组合的原始 provider 列表。
     */
    val providers: List<P>
}

/**
 * 同类 provider 的合并器。
 */
@CaPlatformInterface
interface CaComposableProviderMerger<P : CangJieComposableProvider> {
    /**
     * 将多个 provider 合并为一个 provider。
     */
    fun merge(providers: List<P>): P
}
