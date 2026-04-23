package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.signatures.renderFunctionSignature
import org.cangnova.cangjie.analysis.api.cfir.signatures.renderVariableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol

/**
 * 对齐 Kotlin `KaFirSignatureSubstitutor` 的 CFIR 后端实现。
 */
@CaImplementationDetail
internal class CaCfirSignatureSubstitutor(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSignatureSubstitutor<CaCfirSession>() {
    @OptIn(CaExperimentalApi::class)
    override fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S> {
        return analysisSession.renderFunctionSignature(this)
    }

    @OptIn(CaExperimentalApi::class)
    override fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S> {
        return analysisSession.renderVariableSignature(this)
    }
}
