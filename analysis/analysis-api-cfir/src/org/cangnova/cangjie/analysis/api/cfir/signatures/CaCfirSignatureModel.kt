package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.buildSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.AbstractCaCfirSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.impl.base.signatures.CaBaseVariableSignature
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 对齐 Kotlin `KaFirFunctionSignature` 的 CFIR 函数签名叶子。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal sealed class CaCfirFunctionSignature<out S : CaFunctionSymbol> : CaFunctionSignature<S>, CfirSymbolBasedSignature {
    abstract override fun substitute(substitutor: CaSubstitutor): CaCfirFunctionSignature<S>

    override fun equals(other: Any?): Boolean =
        this === other || other?.javaClass == javaClass && (other as CaCfirFunctionSignature<*>).cfirSymbol == cfirSymbol

    override fun hashCode(): Int = cfirSymbol.hashCode()
}

@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirFunctionDummySignature<out S : CaFunctionSymbol>(
    override val token: CaLifetimeToken,
    override val cfirSymbol: CfirFunctionSymbol<*>,
    override val cfirSymbolBuilder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
) : CaCfirFunctionSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { cfirSymbol.buildSymbol(cfirSymbolBuilder) as S }

    override val returnType: CaType
        get() = withValidityAssertion { symbol.returnType }

    override val receiverType: CaType?
        get() = withValidityAssertion { symbol.receiverType }

    override val valueParameters: List<CaVariableSignature<CaValueParameterSymbol>> by cached {
        cfirSymbol.cfir.valueParameters.map { CaCfirVariableDummySignature(token, it.symbol, cfirSymbolBuilder) }
    }

    override fun substitute(substitutor: CaSubstitutor): CaCfirFunctionSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractCaCfirSubstitutor<*>)
        CaCfirFunctionSubstitutorBasedSignature(token, cfirSymbol, cfirSymbolBuilder, substitutor.substitutor)
    }
}

/**
 * 对齐 Kotlin `KaFirVariableSignature` 的 CFIR 变量签名叶子。
 *
 * 由于仓颉当前 `CfirPropertySymbol` 不在 `CfirVariableSymbol` 继承树内，
 * 这里按 callable 级底层 symbol 建模，但公开层仍严格只暴露 `CaVariableSymbol`。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal sealed class CaCfirVariableSignature<out S : CaVariableSymbol> : CaBaseVariableSignature<S>(), CfirSymbolBasedSignature {
    abstract override fun substitute(substitutor: CaSubstitutor): CaCfirVariableSignature<S>

    override fun equals(other: Any?): Boolean =
        this === other || other?.javaClass == javaClass && (other as CaCfirVariableSignature<*>).cfirSymbol == cfirSymbol

    override fun hashCode(): Int = cfirSymbol.hashCode()
}

@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirVariableDummySignature<out S : CaVariableSymbol>(
    override val token: CaLifetimeToken,
    override val cfirSymbol: CfirCallableSymbol<*>,
    override val cfirSymbolBuilder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
) : CaCfirVariableSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { cfirSymbol.buildSymbol(cfirSymbolBuilder) as S }

    override val returnType: CaType
        get() = withValidityAssertion { symbol.returnType }

    override val receiverType: CaType?
        get() = withValidityAssertion { symbol.receiverType }

    override fun substitute(substitutor: CaSubstitutor): CaCfirVariableSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractCaCfirSubstitutor<*>)
        CaCfirVariableSubstitutorBasedSignature(token, cfirSymbol, cfirSymbolBuilder, substitutor.substitutor)
    }
}

/**
 * 从 CFIR public function symbol 构造未替换 use-site signature。
 */
@OptIn(CaExperimentalApi::class)
internal fun <S : CaFunctionSymbol> CaCfirSession.renderFunctionSignature(symbol: S): CaFunctionSignature<S> {
    val cfirSymbol = (symbol as? CaCfirSymbol<*>)?.cfirSymbol as? CfirFunctionSymbol<*>
        ?: error("CFIR function signature construction requires a CFIR-backed function symbol")
    return CaCfirFunctionDummySignature(token, cfirSymbol, cfirSymbolBuilder)
}

/**
 * 从 CFIR public variable symbol 构造未替换 use-site signature。
 */
@OptIn(CaExperimentalApi::class)
internal fun <S : CaVariableSymbol> CaCfirSession.renderVariableSignature(symbol: S): CaVariableSignature<S> {
    val cfirSymbol = (symbol as? CaCfirSymbol<*>)?.cfirSymbol
        as? org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol<*>
        ?: error("CFIR variable signature construction requires a CFIR-backed variable symbol")
    return CaCfirVariableDummySignature(token, cfirSymbol, cfirSymbolBuilder)
}
