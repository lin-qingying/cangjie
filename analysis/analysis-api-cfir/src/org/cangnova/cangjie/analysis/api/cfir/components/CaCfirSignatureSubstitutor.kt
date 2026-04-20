package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.signatures.renderSignature
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

/**
 * CFIR 侧的公开签名构造组件。
 *
 * `asSignature()` / `substitute()` 的通用流程已经下沉到
 * `CaBaseSignatureSubstitutor`，这里只负责把公开 callable symbol
 * 构造成 CFIR use-site 签名。
 */
internal class CaCfirSignatureSubstitutor(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSignatureSubstitutor<CaCfirSession>() {
    override fun <S : CaCallableSymbol> buildSignature(symbol: S): CaSignature<S> {
        return analysisSession.renderSignature(symbol)
    }
}
