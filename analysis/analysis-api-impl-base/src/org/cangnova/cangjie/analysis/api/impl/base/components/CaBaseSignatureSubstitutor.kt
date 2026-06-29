package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor

/**
 * 对齐 Kotlin `KaBaseSignatureSubstitutor`。
 *
 * 公共 callable 入口只做 function / variable 两族分派，
 * 具体签名构造由后端子类在精确族分支上提供。
 */
@CaImplementationDetail
abstract class CaBaseSignatureSubstitutor<T : CaSession> :
    CaBaseSessionComponent<T>(),
    CaSignatureSubstitutor {
    /**
     * 将函数符号转换为函数签名。
     */
    abstract override fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S>

    /**
     * 将变量符号转换为变量签名。
     */
    abstract override fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S>

    /**
     * 对函数符号签名应用类型替换。
     */
    @OptIn(CaExperimentalApi::class)
    override fun <S : CaFunctionSymbol> S.substitute(substitutor: CaSubstitutor): CaFunctionSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    /**
     * 对变量符号签名应用类型替换。
     */
    @OptIn(CaExperimentalApi::class)
    override fun <S : CaVariableSymbol> S.substitute(substitutor: CaSubstitutor): CaVariableSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    /**
     * 对 callable 符号签名应用类型替换，并按函数/变量符号分派。
     */
    @OptIn(CaExperimentalApi::class)
    override fun <S : CaCallableSymbol> S.substitute(substitutor: CaSubstitutor): CaCallableSignature<S> = withValidityAssertion {
        when (this) {
            is CaFunctionSymbol -> substitute(substitutor)
            is CaVariableSymbol -> substitute(substitutor)
            else -> error("Unsupported callable signature substitution for `${this::class.simpleName}`")
        }
    }

    /**
     * 将 callable 符号转换为签名，并按函数/变量符号分派。
     */
    override fun <S : CaCallableSymbol> S.asSignature(): CaCallableSignature<S> = withValidityAssertion {
        when (this) {
            is CaFunctionSymbol -> asSignature()
            is CaVariableSymbol -> asSignature()
            else -> error("Unsupported callable signature construction for `${this::class.simpleName}`")
        }
    }
}
