package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 类型附加信息协议。
 *
 * 该层负责类型对象与公开 symbol 之间的稳定关联，
 * 以及错误类型、展开类型等非声明级类型元信息。
 *
 * `createPointer()` 已经是 `CaType` 自身的稳定成员协议，
 * 因此这里不再重复暴露第二套 component 入口。
 */
interface CaTypeInformationProvider : CaLifetimeOwner {
    val CaType.isErrorType: Boolean
    val CaType.fullyExpandedType: CaType
    val CaType.classLikeSymbol: CaClassLikeSymbol?
}
