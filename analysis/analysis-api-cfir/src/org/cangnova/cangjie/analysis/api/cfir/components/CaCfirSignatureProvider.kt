package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.signatures.renderSignature
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbolByPsi
import org.cangnova.cangjie.analysis.api.components.CaSignatureProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.psi.CjCallableDeclaration

/**
 * CFIR 签名提供器。
 */
internal class CaCfirSignatureProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSignatureProvider {
    override fun CjCallableDeclaration.asSignature(): CaSignature<CaCallableSymbol>? = withValidityAssertion {
        val symbol = analysisSession.getPublicSymbolByPsi<CaCallableSymbol>(this@asSignature)
            ?: return@withValidityAssertion null
        with(analysisSession) {
            symbol.asSignature()
        }
    }
}
