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
 * Analysis API 需要对外暴露"调用点最终看到了哪些候选、最终选择了哪个候选",
 * 但不能把底层 CFIR 的候选对象直接泄漏到上层。
 *
 * 因此这里稳定公开两层信息:
 * 1. [CaSuccessCallInfo]:无错误的最终选中调用;
 * 2. [CaErrorCallInfo]:错误解析下可观察到的全部候选 + 诊断。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallInfo`。
 */
@OptIn(CaImplementationDetail::class)
sealed interface CaCallInfo : CaLifetimeOwner

/**
 * 成功的调用解析结果。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaSuccessCallInfo : CaCallInfo {
    /**
     * 成功解析得到的 [CaCall]。
     */
    val call: CaCall
}

/**
 * 错误的调用解析结果,携带候选集合与诊断信息。
 */
interface CaErrorCallInfo : CaCallInfo {
    /**
     * 解析过程中考虑过、但最终未被选中的候选调用列表。
     *
     * 例如出现重载歧义时,会以多候选错误形式返回。错误解析不保证一定有候选。
     */
    val candidateCalls: List<CaCall>

    /**
     * 描述错误的 [CaDiagnostic]。
     */
    val diagnostic: CaDiagnostic
}

/**
 * 当前 [CaCallInfo] 对外可见的调用列表。
 *
 * - 成功结果只暴露最终选中的调用;
 * - 错误结果暴露所有候选。
 */
val CaCallInfo.calls: List<CaCall>
    get() = when (this) {
        is  CaErrorCallInfo -> candidateCalls
        is CaSuccessCallInfo -> listOf(call)
    }

/**
 * 如果只有唯一一个类型为 [T] 的调用,返回之;否则返回 `null`。
 *
 * 错误调用时同样适用:在所有候选中查找单一类型匹配。
 */
inline fun <reified T : CaCall> CaCallInfo.singleCallOrNull(): T? {
    return calls.singleOrNull { it is T } as T?
}

/**
 * 如果只有唯一一个 [CaFunctionCall],返回之;否则返回 `null`。
 *
 * @see singleCallOrNull
 */
fun CaCallInfo.singleFunctionCallOrNull(): CaFunctionCall<*>? = singleCallOrNull()

/**
 * 如果只有唯一一个 [CaVariableAccessCall],返回之;否则返回 `null`。
 *
 * @see singleCallOrNull
 */
fun CaCallInfo.singleVariableAccessCall(): CaVariableAccessCall? = singleCallOrNull()

/**
 * 如果只有唯一一个目标为构造器的 [CaFunctionCall],返回之;否则返回 `null`。
 *
 * @see singleCallOrNull
 */
@Suppress("UNCHECKED_CAST")
fun CaCallInfo.singleConstructorCallOrNull(): CaFunctionCall<CaConstructorSymbol>? =
    singleCallOrNull<CaFunctionCall<*>>()?.takeIf { it.symbol is CaConstructorSymbol } as CaFunctionCall<CaConstructorSymbol>?

/**
 * 返回类型为 [T] 的成功调用;若调用本身不成功,或类型不匹配,则返回 `null`。
 */
inline fun <reified T : CaCall> CaCallInfo.successfulCallOrNull(): T? {
    return (this as? CaSuccessCallInfo)?.call as? T
}

/**
 * 返回成功的 [CaFunctionCall];若不存在则返回 `null`。
 *
 * @see successfulCallOrNull
 */
fun CaCallInfo.successfulFunctionCallOrNull(): CaFunctionCall<*>? = successfulCallOrNull()

/**
 * 返回成功的 [CaVariableAccessCall];若不存在则返回 `null`。
 *
 * @see successfulCallOrNull
 */
fun CaCallInfo.successfulVariableAccessCall(): CaVariableAccessCall? = successfulCallOrNull()

/**
 * 返回成功的、目标为构造器的 [CaFunctionCall];若不存在则返回 `null`。
 *
 * @see successfulCallOrNull
 */
@Suppress("UNCHECKED_CAST")
fun CaCallInfo.successfulConstructorCallOrNull(): CaFunctionCall<CaConstructorSymbol>? =
    successfulCallOrNull<CaFunctionCall<*>>()?.takeIf { it.symbol is CaConstructorSymbol } as CaFunctionCall<CaConstructorSymbol>?

/**
 * [CaPartiallyAppliedSymbol] 所代表的可调用符号。
 *
 * 部分应用符号虽然缺少完整运行时信息(实参等),但被调用的具体声明是确定的。
 */
val <S : CaCallableSymbol, C : CaCallableSignature<S>> CaPartiallyAppliedSymbol<S, C>.symbol: S get() = signature.symbol

/**
 * [CaCallableMemberCall] 的被调用符号。
 */
val <S : CaCallableSymbol, C : CaCallableSignature<S>> CaCallableMemberCall<S, C>.symbol: S
    get() = partiallyAppliedSymbol.symbol

/**
 * 对变量(含属性)的访问调用。
 *
 * 与函数调用相对应:目标是变量/属性,需要额外区分访问模式(读 / 写)。
 */
@OptIn(CaImplementationDetail::class, CaExperimentalApi::class)
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaVariableAccessCall : CaSingleCall<CaVariableSymbol, CaVariableSignature<CaVariableSymbol>>,
    CaCallableMemberCall<CaVariableSymbol, CaVariableSignature<CaVariableSymbol>> {

    /**
     * 部分应用符号(变量族特化)。
     *
     * 已 deprecated,建议直接访问 partiallyAppliedSymbol 内部组件。
     */
    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<CaVariableSymbol, CaVariableSignature<CaVariableSymbol>>

    /**
     * 该调用是否经由
     * [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) 解析得到。
     */
    @CaExperimentalApi
    val isContextSensitive: Boolean

    /**
     * 对变量的访问类型(读 / 写),以及伴随的附加信息。
     */
    val kind: Kind

    /**
     * 访问模式分类。
     *
     * 当前提供两个直接子接口:[Read] 与 [Write]。
     *
     * @see CaVariableAccessCall
     */
    sealed interface Kind {
        /**
         * 读取变量。
         */
        @SubclassOptInRequired(CaImplementationDetail::class)
        interface Read : Kind

        /**
         * 写入变量。
         */
        @SubclassOptInRequired(CaImplementationDetail::class)
        interface Write : Kind {
            /**
             * 赋值表达式右侧的新值。
             *
             * 当赋值不完整(缺少右值)时为 `null`。
             */
            val value: CjExpression?
        }
    }
}
