package org.cangnova.cangjie.analysis.api.impl.base

import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 能够还原成 `类型参数符号 -> 类型` 映射的 substitutor。
 *
 * 这是 impl-base 级别的实现标记，只服务于后端与测试框架的结构化协作，
 * public API 调用方不应依赖某个 substitutor 是否实现该接口。
 */
interface CaMapBackedSubstitutor : CaSubstitutor {
    fun getAsMap(): Map<CaTypeParameterSymbol, CaType>
}

/**
 * 由两个 substitutor 顺序串联得到的 substitutor。
 *
 * 这同样只是实现层协议，用来对齐 Kotlin impl-base 的 chained substitutor 结构。
 */
interface CaChainedSubstitutor : CaSubstitutor {
    val first: CaSubstitutor
    val second: CaSubstitutor
}
