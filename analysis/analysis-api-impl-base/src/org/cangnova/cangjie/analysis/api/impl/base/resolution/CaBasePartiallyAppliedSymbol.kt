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
    /**
     * 已经完成类型替换后的 callable signature。
     */
    private val backingSignature: C,
    dispatchReceiver: CaReceiverValue?,
) : CaPartiallyAppliedSymbol<S, C> {
    /**
     * 当前调用绑定的 dispatch receiver。
     */
    private val backingDispatchReceiver: CaReceiverValue? = dispatchReceiver

    /**
     * 部分应用符号沿用 signature 的 lifetime token。
     */
    override val token: CaLifetimeToken
        get() = backingSignature.token

    /**
     * 返回已应用调用上下文后的 signature。
     */
    override val signature: C
        get() = withValidityAssertion { backingSignature }

    /**
     * 返回 dispatch receiver。
     */
    override val dispatchReceiver: CaReceiverValue?
        get() = withValidityAssertion { backingDispatchReceiver }
}
