package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId


/**
 * 可调用符号的 use-site 签名视图。
 *
 * 与符号本身([CaCallableSymbol])相比,签名记录的是"在使用现场"经过类型替换后的形态:
 *
 * - 例如对 `fun foo(list: List<String>) = list.get(1)`,`get` 的符号侧返回类型是 `T`,
 *   而签名侧返回类型已经实例化为 `String`。
 *
 * 签名的相等性由其内容(符号 + 已替换的类型集合)决定,
 * 而不是引用相等;具体子接口应给出 `equals/hashCode` 的稳定实现。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableSignature`。
 *
 * @param S 实际可调用符号类型(协变)。
 */
@OptIn(CaImplementationDetail::class)
sealed interface CaCallableSignature<out S : CaCallableSymbol> : CaLifetimeOwner {
    /**
     * 与签名关联的可调用符号,签名携带其 use-site 信息。
     */
    val symbol: S

    /**
     * 经过 use-site 替换的返回类型,对齐 [CaCallableSymbol.returnType]。
     */
    val returnType: CaType

    /**
     * 经过 use-site 替换的扩展 receiver 类型,对齐 [CaCallableSymbol.receiverParameter]。
     *
     * 非扩展声明返回 `null`。
     */
    val receiverType: CaType?

    /**
     * 签名对应的 [CallableId],默认沿用符号自身的 [CaCallableSymbol.callableId]。
     */
    val callableId: CallableId? get() = withValidityAssertion { symbol.callableId }


    /**
     * 在当前签名上应用 [substitutor],返回经过类型替换的新签名。
     *
     * @see CaSubstitutor.substitute
     */
    @CaExperimentalApi
    fun substitute(substitutor: CaSubstitutor): CaCallableSignature<S>

    /**
     * 基于内容的相等性,具体实现必须提供稳定的 `equals`。
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * 基于内容的哈希,需与 [equals] 保持一致。
     */
    abstract override fun hashCode(): Int
}
