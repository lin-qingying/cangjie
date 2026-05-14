package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * typealias 的公开语义视图。
 *
 * 类型别名只引入"另一种写法"，不引入新的类型实体：
 * 它与 [CaClassSymbol] 同为 [CaClassLikeSymbol] 的子族，但本身不参与继承结构。
 */
interface CaTypeAliasSymbol : CaClassLikeSymbol {
    /**
     * 展开后的目标类型。
     *
     * 这是别名背后真正指向的类型，调用方需要时自行决定是否进一步递归展开。
     */
    val expandedType: CaType
}
