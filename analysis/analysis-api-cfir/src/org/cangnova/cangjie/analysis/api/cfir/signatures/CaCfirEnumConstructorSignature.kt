package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaEnumConstructorSignature
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType

/** 对应 CFIR 枚举构造器符号的调用点签名。 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirEnumConstructorSignature<S : CaEnumConstructorSymbol>(
    /** 当前签名对应的枚举构造器符号。 */
    override val symbol: S,
    /** 按调用点顺序应用于 payload 与返回类型的公开替换器。 */
    private val substitutors: List<CaSubstitutor> = emptyList(),
) : CaEnumConstructorSignature<S> {
    override val token = symbol.token

    override val returnType: CaType by cached {
        applySubstitutors(symbol.returnType)
    }

    override val receiverType: CaType? by cached {
        symbol.receiverType?.let(::applySubstitutors)
    }

    override val payloadTypes: List<CaType> by cached {
        symbol.payloadTypes.map(::applySubstitutors)
    }

    override fun substitute(substitutor: CaSubstitutor): CaEnumConstructorSignature<S> = withValidityAssertion {
        if (substitutor is CaSubstitutor.Empty) return@withValidityAssertion this
        CaCfirEnumConstructorSignature(symbol, substitutors + substitutor)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is CaCfirEnumConstructorSignature<*> &&
            symbol == other.symbol &&
            returnType == other.returnType &&
            receiverType == other.receiverType &&
            payloadTypes == other.payloadTypes

    override fun hashCode(): Int =
        (((symbol.hashCode() * 31) + returnType.hashCode()) * 31 + (receiverType?.hashCode() ?: 0)) * 31 + payloadTypes.hashCode()

    private fun applySubstitutors(type: CaType): CaType = substitutors.fold(type) { current, substitutor ->
        substitutor.substitute(current)
    }
}
