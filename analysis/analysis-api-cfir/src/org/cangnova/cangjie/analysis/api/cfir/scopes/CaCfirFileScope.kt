package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope

/**
 * 文件级公开作用域。
 *
 * 当前仓颉文件作用域需要同时体现：
 * 1. 当前文件自身的顶层声明
 * 2. 同 package 中可直接可见的顶层声明
 * 因此这里组合“文件声明 scope + package scope”，但两者都是真实 CFIR scope。
 */
internal class CaCfirFileScope(
    fileDeclaredScope: CfirContainingNamesAwareScope,
    private val packageScope: CfirContainingNamesAwareScope?,
    analysisSession: CaCfirSession,
    private val fileSymbol: CaFileSymbol,
    token: CaLifetimeToken,
) : CaCfirBasedScope<CfirContainingNamesAwareScope>(fileDeclaredScope, analysisSession, token) {
    override val additionalScopes: List<CfirContainingNamesAwareScope>
        get() = listOfNotNull(packageScope)

    override val eagerSymbols: List<CaSymbol>
        get() = listOf(fileSymbol)
}
