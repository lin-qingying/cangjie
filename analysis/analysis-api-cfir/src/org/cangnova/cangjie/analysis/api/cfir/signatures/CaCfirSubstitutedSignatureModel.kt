package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.buildSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirFunctionSubstitutorBasedSignature<out S : CaFunctionSymbol>(
    override val token: CaLifetimeToken,
    override val cfirSymbol: CfirFunctionSymbol<*>,
    override val cfirSymbolBuilder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
    private val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : CaCfirFunctionSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { cfirSymbol.buildSymbol(cfirSymbolBuilder) as S }

    override val returnType: CaType by cached {
        cfirSymbolBuilder.typeBuilder.buildType(coneSubstitutor.substituteOrSelf(cfirSymbol.resolvedReturnType))
    }

    override val receiverType: CaType? by cached {
        symbol.receiverType?.let { substitutePublicType(it, coneSubstitutor, cfirSymbolBuilder) }
    }

    override val valueParameters: List<CaVariableSignature<CaValueParameterSymbol>> by cached {
        cfirSymbol.cfir.valueParameters.map {
            CaCfirVariableSubstitutorBasedSignature(token, it.symbol, cfirSymbolBuilder, coneSubstitutor)
        }
    }

    override fun substitute(substitutor: CaSubstitutor): CaCfirFunctionSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        error("Chained signature substitution is not wired for CFIR yet")
    }
}

@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirVariableSubstitutorBasedSignature<out S : CaVariableSymbol>(
    override val token: CaLifetimeToken,
    override val cfirSymbol: CfirCallableSymbol<*>,
    override val cfirSymbolBuilder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
    private val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : CaCfirVariableSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { cfirSymbol.buildSymbol(cfirSymbolBuilder) as S }

    override val returnType: CaType by cached {
        cfirSymbolBuilder.typeBuilder.buildType(coneSubstitutor.substituteOrSelf((symbol.returnType as CaCfirType).coneType))
    }

    override val receiverType: CaType? by cached {
        symbol.receiverType?.let { substitutePublicType(it, coneSubstitutor, cfirSymbolBuilder) }
    }

    override fun substitute(substitutor: CaSubstitutor): CaCfirVariableSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        error("Chained signature substitution is not wired for CFIR yet")
    }
}

private fun substitutePublicType(
    type: CaType,
    substitutor: ConeSubstitutor,
    builder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
): CaType {
    val cfirType = type as? CaCfirType
        ?: error("Only CFIR public types can participate in CFIR signature substitution")
    return builder.typeBuilder.buildType(substitutor.substituteOrSelf(cfirType.coneType))
}
