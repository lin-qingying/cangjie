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

/**
 * 基于 Cone substitutor 的函数签名实现。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirFunctionSubstitutorBasedSignature<out S : CaFunctionSymbol>(
    /**
     * 签名所属生命周期令牌。
     */
    override val token: CaLifetimeToken,
    /**
     * 签名对应的底层 CFIR 函数符号。
     */
    override val cfirSymbol: CfirFunctionSymbol<*>,
    /**
     * 用于构造公开符号和类型的 CFIR builder。
     */
    override val cfirSymbolBuilder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
    /**
     * 应用于返回类型、receiver 和值参数的底层替换器。
     */
    private val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : CaCfirFunctionSignature<S>() {
    /**
     * 当前签名对应的公开函数符号。
     */
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { cfirSymbol.buildSymbol(cfirSymbolBuilder) as S }

    /**
     * 替换后的函数返回类型。
     */
    override val returnType: CaType by cached {
        cfirSymbolBuilder.typeBuilder.buildType(coneSubstitutor.substituteOrSelf(cfirSymbol.resolvedReturnType))
    }

    /**
     * 替换后的 receiver 类型。
     */
    override val receiverType: CaType? by cached {
        symbol.receiverType?.let { substitutePublicType(it, coneSubstitutor, cfirSymbolBuilder) }
    }

    /**
     * 替换后的值参数签名列表。
     */
    override val valueParameters: List<CaVariableSignature<CaValueParameterSymbol>> by cached {
        cfirSymbol.cfir.valueParameters.map {
            CaCfirVariableSubstitutorBasedSignature(token, it.symbol, cfirSymbolBuilder, coneSubstitutor)
        }
    }

    /**
     * 当前实现暂不支持链式签名替换。
     */
    override fun substitute(substitutor: CaSubstitutor): CaCfirFunctionSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        error("Chained signature substitution is not wired for CFIR yet")
    }
}

/**
 * 基于 Cone substitutor 的变量签名实现。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirVariableSubstitutorBasedSignature<out S : CaVariableSymbol>(
    /**
     * 签名所属生命周期令牌。
     */
    override val token: CaLifetimeToken,
    /**
     * 签名对应的底层 CFIR callable 符号。
     */
    override val cfirSymbol: CfirCallableSymbol<*>,
    /**
     * 用于构造公开符号和类型的 CFIR builder。
     */
    override val cfirSymbolBuilder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
    /**
     * 应用于返回类型和 receiver 的底层替换器。
     */
    private val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : CaCfirVariableSignature<S>() {
    /**
     * 当前签名对应的公开变量符号。
     */
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { cfirSymbol.buildSymbol(cfirSymbolBuilder) as S }

    /**
     * 替换后的变量返回类型。
     */
    override val returnType: CaType by cached {
        cfirSymbolBuilder.typeBuilder.buildType(coneSubstitutor.substituteOrSelf((symbol.returnType as CaCfirType).coneType))
    }

    /**
     * 替换后的 receiver 类型。
     */
    override val receiverType: CaType? by cached {
        symbol.receiverType?.let { substitutePublicType(it, coneSubstitutor, cfirSymbolBuilder) }
    }

    /**
     * 当前实现暂不支持链式签名替换。
     */
    override fun substitute(substitutor: CaSubstitutor): CaCfirVariableSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        error("Chained signature substitution is not wired for CFIR yet")
    }
}

/**
 * 将公开 CFIR 类型按指定 Cone substitutor 替换。
 */
private fun substitutePublicType(
    type: CaType,
    substitutor: ConeSubstitutor,
    builder: org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder,
): CaType {
    val cfirType = type as? CaCfirType
        ?: error("Only CFIR public types can participate in CFIR signature substitution")
    return builder.typeBuilder.buildType(substitutor.substituteOrSelf(cfirType.coneType))
}
