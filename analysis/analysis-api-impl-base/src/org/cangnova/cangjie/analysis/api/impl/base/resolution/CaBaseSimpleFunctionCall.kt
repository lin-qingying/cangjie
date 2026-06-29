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
    /**
     * 当前函数调用解析出的部分应用函数符号。
     */
    private val backingPartiallyAppliedSymbol: CaPartiallyAppliedFunctionSymbol<CaFunctionSymbol>,
    /**
     * 源码实参与 value parameter signature 的映射。
     */
    private val backingValueArgumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>,
    /**
     * 类型参数到实际类型实参的映射。
     */
    private val backingTypeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>,
) : CaFunctionCall<CaFunctionSymbol> {
    /**
     * 函数调用沿用部分应用符号的 lifetime token。
     */
    override val token: CaLifetimeToken
        get() = backingPartiallyAppliedSymbol.token

    /**
     * 返回部分应用函数符号。
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: CaPartiallyAppliedFunctionSymbol<CaFunctionSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    /**
     * 返回当前函数调用使用的函数签名。
     */
    override val signature: CaFunctionSignature<CaFunctionSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    /**
     * 返回当前函数调用的 dispatch receiver。
     */
    override val dispatchReceiver: CaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    /**
     * 返回类型参数到实际类型的映射。
     */
    override val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
        get() = withValidityAssertion { backingTypeArgumentsMapping }

    /**
     * 返回源码值实参与 value parameter signature 的映射。
     */
    override val valueArgumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>
        get() = withValidityAssertion { backingValueArgumentMapping }

    /**
     * 返回源码实参与 callable parameter signature 的合并映射。
     */
    override val combinedArgumentMapping: Map<CjExpression, CaVariableSignature<CaParameterSymbol>>
        get() = withValidityAssertion { backingValueArgumentMapping }
}
