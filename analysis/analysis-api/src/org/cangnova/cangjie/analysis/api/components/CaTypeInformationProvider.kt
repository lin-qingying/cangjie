package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer

/**
 * 类型附加信息协议。
 *
 * 该层负责类型对象与公开 symbol 之间的稳定关联，
 * 以及错误类型、指针等非声明级类型元信息。
 */
interface CaTypeInformationProvider : CaLifetimeOwner {
    fun CaType.createPointer(): CaTypePointer<CaType>

    val CaType.isErrorType: Boolean
    val CaType.fullyExpandedType: CaType
    val CaType.classLikeSymbol: CaClassLikeSymbol?
}
