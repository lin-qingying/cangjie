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
     * 这是别名右侧最终解析完成后的目标类型。
     * 若右侧仍引用其他 typealias，这里返回继续展开后的最终结果。
     */
    val expandedType: CaType
}
