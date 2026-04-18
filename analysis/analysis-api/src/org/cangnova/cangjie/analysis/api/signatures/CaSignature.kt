package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

/**
 * callable use-site 签名的公开语义快照。
 *
 * 这里对齐 Kotlin `KaCallableSignature` 的职责边界：
 * 1. 签名总是从某个公开 callable symbol 派生；
 * 2. 签名承载 use-site 视角下的 receiver / 参数 / 返回类型；
 * 3. 签名自身可以继续应用公开 substitutor，形成新的 use-site 签名。
 */
interface CaSignature<out S : CaCallableSymbol> : CaLifetimeOwner {
    /**
     * 该签名所对应的底层公开 callable symbol。
     */
    val symbol: S

    /**
     * 签名对应的稳定 callable 身份。
     *
     * 对匿名或局部 callable，允许为 `null`。
     */
    val callableId: CallableId?
        get() = symbol.callableId

    /**
     * 声明名。
     *
     * 默认与底层 symbol 名称保持一致；匿名 callable 允许为 `null`。
     */
    val declarationName: Name?
        get() = symbol.name

    /**
     * 类型参数符号列表，顺序与声明顺序一致。
     *
     * 这里保持“类型参数身份”而不是退化成纯名字，
     * 以便 substitutor 构造、签名实例化与后续 type-parameter 级语义判断
     * 都沿用同一套公开 symbol 身份。
     */
    val typeParameters: List<CaTypeParameterSymbol>

    /**
     * 值参数签名列表，顺序与声明顺序一致。
     */
    val valueParameters: List<CaValueParameterSignature>

    /**
     * 返回类型的语义对象。
     */
    val returnType: CaType?

    /**
     * use-site 视角下的显式 receiver 类型。
     */
    val receiverType: CaType?

    /**
     * 声明本身直接携带的注解。
     */
    val annotations: List<CaAnnotation>

    /**
     * 对当前 use-site 签名继续应用公开 substitutor。
     */
    fun substitute(substitutor: CaSubstitutor): CaSignature<S>
}
