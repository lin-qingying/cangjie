package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope

/**
 * 包级公开作用域。
 */
internal class CaCfirPackageScope(
    packageScope: CfirContainingNamesAwareScope,
    analysisSession: CaCfirSession,
    private val packageSymbol: CaPackageSymbol,
    token: CaLifetimeToken,
) : CaCfirBasedScope<CfirContainingNamesAwareScope>(packageScope, analysisSession, token) {
    override val eagerSymbols: List<CaSymbol>
        get() = listOf(packageSymbol)
}
