package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.resolution.successfulCallOrNull
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.psi.CjExpression

/**
 * 调用解析结果。
 *
 * Analysis API 需要对外暴露“调用点最终看到了哪些候选、最终选择了哪个候选”，
 * 但不能把底层 CFIR 的候选对象直接泄漏到上层。
 *
 * 因此这里稳定公开两层信息：
 * 1. [successfulCall] 表示无错误的最终选中调用。
 * 2. [calls] 表示当前调用点可观察到的调用视图集合，允许包含带错误的已选候选。
 */
@OptIn(CaImplementationDetail::class)
public sealed interface CaCallInfo : CaLifetimeOwner
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaSuccessCallInfo : CaCallInfo {
    /**
     * The successfully resolved [CaCall].
     */
    public val call: CaCall
}
public interface CaErrorCallInfo : CaCallInfo {
    /**
     * A list of [CaCall]s to candidates that were considered during the call resolution process, but ultimately not selected. This may be
     * due to various errors. For example, an ambiguity results in an error call with multiple candidates.
     *
     * An error call is not guaranteed to have any candidates.
     */
    public val candidateCalls: List<CaCall>

    /**
     * The [CaDiagnostic] describing the error.
     */
    public val diagnostic: CaDiagnostic
}

public val CaCallInfo.calls: List<CaCall>
    get() = when (this) {
        is  CaErrorCallInfo -> candidateCalls
        is CaSuccessCallInfo -> listOf(call)
    }

/**
 * Returns the single [CaCall] of type [T] associated with the [CaCallInfo], or `null` if there is no such exact single call.
 *
 * In the case of an [error call][CaErrorCallInfo], returns a single [candidate call][CaErrorCallInfo.candidateCalls] of type [T].
 */
public inline fun <reified T : CaCall> CaCallInfo.singleCallOrNull(): T? {
    return calls.singleOrNull { it is T } as T?
}

/**
 * Returns the single [CaFunctionCall] associated with the [CaCallInfo], or `null` if there is no such exact single call.
 *
 * @see singleCallOrNull
 */
public fun CaCallInfo.singleFunctionCallOrNull(): CaFunctionCall<*>? = singleCallOrNull()

/**
 * Returns the single [CaVariableAccessCall] associated with the [CaCallInfo], or `null` if there is no such exact single call.
 *
 * @see singleCallOrNull
 */
public fun CaCallInfo.singleVariableAccessCall(): CaVariableAccessCall? = singleCallOrNull()

/**
 * Returns the single [CaFunctionCall] with a [CaConstructorSymbol] associated with the [CaCallInfo], or `null` if there is no such exact
 * single call.
 *
 * @see singleCallOrNull
 */
@Suppress("UNCHECKED_CAST")
public fun CaCallInfo.singleConstructorCallOrNull(): CaFunctionCall<CaConstructorSymbol>? =
    singleCallOrNull<CaFunctionCall<*>>()?.takeIf { it.symbol is CaConstructorSymbol } as CaFunctionCall<CaConstructorSymbol>?

/**
 * Returns the successful [CaCall] of type [T] associated with the [CaCallInfo], or `null` if there is no such exact call (either the call
 * is not successful, or the successful call is of another type).
 */
public inline fun <reified T : CaCall> CaCallInfo.successfulCallOrNull(): T? {
    return (this as? CaSuccessCallInfo)?.call as? T
}

/**
 * Returns the successful [CaFunctionCall] associated with the [CaCallInfo], or `null` if there is no such exact call.
 *
 * @see successfulCallOrNull
 */
public fun CaCallInfo.successfulFunctionCallOrNull(): CaFunctionCall<*>? = successfulCallOrNull()

/**
 * Returns the successful [CaVariableAccessCall] associated with the [CaCallInfo], or `null` if there is no such exact call.
 *
 * @see successfulCallOrNull
 */
public fun CaCallInfo.successfulVariableAccessCall(): CaVariableAccessCall? = successfulCallOrNull()

/**
 * Returns the successful [CaFunctionCall] with a [CaConstructorSymbol] associated with the [CaCallInfo], or `null` if there is no such
 * exact call.
 *
 * @see successfulCallOrNull
 */
@Suppress("UNCHECKED_CAST")
public fun CaCallInfo.successfulConstructorCallOrNull(): CaFunctionCall<CaConstructorSymbol>? =
    successfulCallOrNull<CaFunctionCall<*>>()?.takeIf { it.symbol is CaConstructorSymbol } as CaFunctionCall<CaConstructorSymbol>?

/**
 * The [callable symbol][CaCallableSymbol] which the [CaPartiallyAppliedSymbol] represents. While the information contained in a partially
 * applied symbol is not exhaustive (e.g. applied functions are missing value arguments), the symbol of the callable which is called is
 * definite.
 */
public val <S : CaCallableSymbol, C : CaCallableSignature<S>> CaPartiallyAppliedSymbol<S, C>.symbol: S get() = signature.symbol

/**
 * The [CaCallableSymbol] of the [CaCallableMemberCall]'s callee.
 */
public val <S : CaCallableSymbol, C : CaCallableSignature<S>> CaCallableMemberCall<S, C>.symbol: S
    get() = partiallyAppliedSymbol.symbol
/**
 * Access to variables (including properties).
 */
@OptIn(CaImplementationDetail::class, CaExperimentalApi::class)
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaVariableAccessCall : CaSingleCall<CaVariableSymbol, CaVariableSignature<CaVariableSymbol>>,
    CaCallableMemberCall<CaVariableSymbol, CaVariableSignature<CaVariableSymbol>> {

    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<CaVariableSymbol, CaVariableSignature<CaVariableSymbol>>

    /**
     * Whether the call was resolved using the [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) feature
     */
    @CaExperimentalApi
    public val isContextSensitive: Boolean

    /**
     * The kind of access to the variable (read or write), alongside additional information
     */
    public val kind: Kind

    /**
     * Determines the kind of access to the [variable][CaVariableAccessCall] (read or write), alongside additional information
     *
     * @see CaVariableAccessCall
     */
    public sealed interface Kind {
        /**
         * The [variable access][CaVariableAccessCall] reads the variable.
         */
        @SubclassOptInRequired(CaImplementationDetail::class)
        public interface Read : Kind

        /**
         * The [variable access][CaVariableAccessCall] writes to the variable.
         */
        @SubclassOptInRequired(CaImplementationDetail::class)
        public interface Write : Kind {
            /**
             * A [CjExpression] that represents the new value which is assigned to this variable, or `null` if the assignment is incomplete and
             * lacks the new value.
             */
            public val value: CjExpression?
        }
    }
}