package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor

/**
 * `CaSignatureSubstitutor` 的通用默认流程。
 *
 * 这里严格对齐 Kotlin `KaBaseSignatureSubstitutor` 的主线思路：
 * 1. `asSignature()` 由后端提供具体签名构造；
 * 2. `substitute()` 统一复用 `Empty` 快路径；
 * 3. function / variable 族通过精确返回类型保留公开 API 语义。
 */
abstract class CaBaseSignatureSubstitutor<T : CaSession> :
    CaBaseSessionComponent<T>(),
    CaSignatureSubstitutor {
    protected abstract fun <S : CaCallableSymbol> buildSignature(symbol: S): CaSignature<S>

    protected open fun <S : CaFunctionSymbol> buildFunctionSignature(symbol: S): CaFunctionSignature<S> {
        @Suppress("UNCHECKED_CAST")
        return buildSignature(symbol) as CaFunctionSignature<S>
    }

    protected open fun <S : CaVariableSymbol> buildVariableSignature(symbol: S): CaVariableSignature<S> {
        @Suppress("UNCHECKED_CAST")
        return buildSignature(symbol) as CaVariableSignature<S>
    }

    final override fun <S : CaCallableSymbol> S.asSignature(): CaSignature<S> = withValidityAssertion {
        buildSignature(this@asSignature)
    }

    final override fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S> = withValidityAssertion {
        buildFunctionSignature(this@asSignature)
    }

    final override fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S> = withValidityAssertion {
        buildVariableSignature(this@asSignature)
    }

    final override fun <S : CaCallableSymbol> S.substitute(substitutor: CaSubstitutor): CaSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    final override fun <S : CaFunctionSymbol> S.substitute(substitutor: CaSubstitutor): CaFunctionSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return asSignature()
        @Suppress("UNCHECKED_CAST")
        return asSignature().substitute(substitutor) as CaFunctionSignature<S>
    }

    final override fun <S : CaVariableSymbol> S.substitute(substitutor: CaSubstitutor): CaVariableSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return asSignature()
        @Suppress("UNCHECKED_CAST")
        return asSignature().substitute(substitutor) as CaVariableSignature<S>
    }
}
