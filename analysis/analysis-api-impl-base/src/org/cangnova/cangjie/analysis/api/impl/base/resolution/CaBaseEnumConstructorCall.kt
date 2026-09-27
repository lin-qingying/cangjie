package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaEnumConstructorCall
import org.cangnova.cangjie.analysis.api.resolution.CaPartiallyAppliedSymbol
import org.cangnova.cangjie.analysis.api.signatures.CaEnumConstructorSignature
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjExpression

/** Analysis API 实现基类中的枚举构造器调用。 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
@CaImplementationDetail
class CaBaseEnumConstructorCall<S : CaEnumConstructorSymbol>(
    private val backingPartiallyAppliedSymbol: CaPartiallyAppliedSymbol<S, CaEnumConstructorSignature<S>>,
    payloadArgumentMapping: Map<CjExpression, CaType>,
    typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>,
) : CaEnumConstructorCall<S> {
    private val backingPayloadArgumentMapping = payloadArgumentMapping.toMap()
    private val backingTypeArgumentsMapping = typeArgumentsMapping.toMap()

    override val token: CaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    @Suppress("DEPRECATION")
    override val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<S, CaEnumConstructorSignature<S>>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val signature: CaEnumConstructorSignature<S>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    override val dispatchReceiver
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    override val payloadArgumentMapping: Map<CjExpression, CaType>
        get() = withValidityAssertion { backingPayloadArgumentMapping }

    override val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
        get() = withValidityAssertion { backingTypeArgumentsMapping }
}
