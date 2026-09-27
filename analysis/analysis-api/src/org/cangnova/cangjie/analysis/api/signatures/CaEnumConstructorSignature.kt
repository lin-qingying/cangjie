package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 仓颉枚举构造器的 use-site 签名。
 *
 * 枚举构造器是 callable，但其 payload 是匿名类型列表，不是函数值参数列表；签名因此保留独立的
 * [payloadTypes] 视图，并与 [CaCallableSignature] 一样承载替换后的返回类型。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaEnumConstructorSignature<out S : CaEnumConstructorSymbol> : CaCallableSignature<S> {
    /** 经 use-site 类型替换后的枚举 payload 类型。 */
    val payloadTypes: List<CaType>

    /** 对 payload 和返回类型应用 [substitutor]，并保留枚举构造器签名类型。 */
    @CaExperimentalApi
    abstract override fun substitute(substitutor: CaSubstitutor): CaEnumConstructorSignature<S>
}
