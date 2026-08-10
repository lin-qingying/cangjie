package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjTypeReference

/**
 * 类型构造与查询协议(从声明侧出发)。
 *
 * 设计要点/职责:
 * - 暴露从声明 symbol 出发取得默认类型的入口,作为 IDE/分析层快速取到 `Self` 类型的稳定通道。
 * - 与 [CaTypeCreator] 区分:本协议负责"由 symbol 看类型",[CaTypeCreator] 负责"构造类型对象"。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeProvider`。
 */
interface CaTypeProvider : CaLifetimeOwner {
    /**
     * 该 class-like 声明对应的默认类型(未带类型实参或带全部类型参数自身)。
     */
    val CaClassLikeSymbol.defaultType: CaType

    /**
     * 如果该值参数是 `vararg`，返回承载其实参数列的 `Array<T>` 类型。
     *
     * 非 `vararg` 参数返回 `null`。
     */
    val CaValueParameterSymbol.varargArrayType: CaType?
}



/**
 * Resolves the given [CjTypeReference] to its corresponding [CaType].
 *
 * This may raise an exception if the resolution ends up with an unexpected result.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!

context(session: CaSession)
public val CjTypeReference.type: CaType
    get() = with(session) { type }
