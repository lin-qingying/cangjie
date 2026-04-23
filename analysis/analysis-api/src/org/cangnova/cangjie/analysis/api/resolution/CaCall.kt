package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression

/**
 * 单个调用的公开语义视图。
 *
 * 该视图稳定暴露调用目标、适用性、接收者拆分、显式类型参数以及实参与形参映射，
 * 供 Analysis API 测试框架、引用服务、渲染器和上层工具共享。
 */
public sealed interface CaCall : CaLifetimeOwner

/**
 * A call to a function, or a simple/compound access to a property.
 */
public sealed interface CaCallableMemberCall<S : CaCallableSymbol, C : CaCallableSignature<S>> : CaCall {
    /**
     * A symbol wrapper for the callee, containing a substituted declaration signature (parameter types for functions, return type for
     * functions and properties), and the actual dispatch receiver.
     */
    public val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<S, C>

    /**
     * A map of inferred type arguments. If type placeholders were used, the actual inferred type will be used as a value. The keys for this
     * map are from [partiallyAppliedSymbol]'s type parameters.
     *
     * In case of a resolution or inference error, the map might be empty.
     */
    public val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
}

@OptIn(CaImplementationDetail::class, CaExperimentalApi::class)
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaFunctionCall<S : CaFunctionSymbol> : CaSingleCall<S, CaFunctionSignature<S>>,
    CaCallableMemberCall<S, CaFunctionSignature<S>> {

    /**
     * A mapping from the call's argument expressions to their associated parameter symbols in a stable order. In case of `vararg`
     * parameters, multiple arguments may be mapped to the same [CaValueParameterSymbol].
     *
     * @see contextArgumentMapping
     * @see combinedArgumentMapping
     */
    public val valueArgumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>


    /**
     * A combined mapping from the call's argument expressions to their associated parameter symbols in a stable order.
     * This includes both [value arguments][valueArgumentMapping] and [context arguments][contextArgumentMapping].
     *
     * In case of `vararg` parameters, multiple arguments may be mapped to the same [CaValueParameterSymbol].
     *
     * @see valueArgumentMapping
     * @see contextArgumentMapping
     */
    public val combinedArgumentMapping: Map<CjExpression, CaVariableSignature<CaParameterSymbol>>

    /**
     * A mapping from the call's argument expressions to their associated parameter symbols in a stable order. In case of `vararg`
     * parameters, multiple arguments may be mapped to the same [CaValueParameterSymbol].
     */
    @Deprecated("Use 'valueArgumentMapping' or 'combinedArgumentMapping' instead", ReplaceWith("valueArgumentMapping"))
    public val argumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>
        get() = valueArgumentMapping

    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<S, CaFunctionSignature<S>>
}

public sealed interface CaSingleOrMultiCall : CaLifetimeOwner


@CaExperimentalApi
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaSingleCall<S : CaCallableSymbol, C : CaCallableSignature<S>> : CaSingleOrMultiCall {
    /**
     * The function or variable declaration.
     */
    public val signature: C

    /**
     * The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) for this symbol access. A dispatch
     * receiver is available if the callable is declared inside a class or object.
     */
    public val dispatchReceiver: CaReceiverValue?

    /**
     * A map of inferred type arguments. If type placeholders were used, the actual inferred type will be used as a value. The keys for this
     * map are from [signature]'s type parameters.
     *
     * In case of a resolution or inference error, the map might be empty.
     */
    public val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
}

public sealed interface CaReceiverValue : CaLifetimeOwner {
    /**
     * The inferred [CaType] of the receiver. This is a smart-casted type in the case of a smart cast on the receiver.
     */
    public val type: CaType
}