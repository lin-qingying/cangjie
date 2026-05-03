package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionCall
import org.cangnova.cangjie.analysis.api.resolution.CaPartiallyAppliedFunctionSymbol
import org.cangnova.cangjie.analysis.api.resolution.CaReceiverValue
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjExpression

/**
 * 对齐 Kotlin `KaBaseSimpleFunctionCall` 的当前仓颉 public API 版本。
 *
 * 现阶段仓颉 public API 只暴露 value/combined argument mapping，
 * 因此这里不引入 Kotlin 上游已有、但仓颉接口尚未公开的 context argument 映射面。
 */
@CaImplementationDetail
class CaBaseSimpleFunctionCall(
    private val backingPartiallyAppliedSymbol: CaPartiallyAppliedFunctionSymbol<CaFunctionSymbol>,
    private val backingValueArgumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>,
    private val backingTypeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>,
) : CaFunctionCall<CaFunctionSymbol> {
    override val token: CaLifetimeToken
        get() = backingPartiallyAppliedSymbol.token

    @Suppress("DEPRECATION")
    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: CaPartiallyAppliedFunctionSymbol<CaFunctionSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val signature: CaFunctionSignature<CaFunctionSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    override val dispatchReceiver: CaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    override val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
        get() = withValidityAssertion { backingTypeArgumentsMapping }

    override val valueArgumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>
        get() = withValidityAssertion { backingValueArgumentMapping }

    override val combinedArgumentMapping: Map<CjExpression, CaVariableSignature<CaParameterSymbol>>
        get() = withValidityAssertion { backingValueArgumentMapping }
}
