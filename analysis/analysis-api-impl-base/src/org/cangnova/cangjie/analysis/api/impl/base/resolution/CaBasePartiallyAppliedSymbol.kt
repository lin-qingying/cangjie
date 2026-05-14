package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaPartiallyAppliedSymbol
import org.cangnova.cangjie.analysis.api.resolution.CaReceiverValue
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

/**
 * 对齐 Kotlin `KaBasePartiallyAppliedSymbol` 的当前仓颉 public API 版本。
 *
 * 仓颉现有 public resolution 面尚未暴露 extension/context receiver，
 * 因此这里只稳定承载当前 API 已定义的 signature 与 dispatch receiver。
 */
@CaImplementationDetail
class CaBasePartiallyAppliedSymbol<out S : CaCallableSymbol, out C : CaCallableSignature<S>>(
    private val backingSignature: C,
    dispatchReceiver: CaReceiverValue?,
) : CaPartiallyAppliedSymbol<S, C> {
    private val backingDispatchReceiver: CaReceiverValue? = dispatchReceiver

    override val token: CaLifetimeToken
        get() = backingSignature.token

    override val signature: C
        get() = withValidityAssertion { backingSignature }

    override val dispatchReceiver: CaReceiverValue?
        get() = withValidityAssertion { backingDispatchReceiver }
}
