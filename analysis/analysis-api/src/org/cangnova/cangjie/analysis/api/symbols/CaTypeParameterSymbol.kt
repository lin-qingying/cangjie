package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 类型参数的公开语义视图。
 *
 * 类型参数是 [CaClassifierSymbol] 的特化形态：它能作为类型出现，但身份只在所属泛型声明内有效。
 * 它持有声明侧的上界约束列表 [upperBounds]，供类型检查、推断与渲染使用。
 */
interface CaTypeParameterSymbol : CaClassifierSymbol, CaNamedSymbol {
    /**
     * 当前类型参数的上界约束列表。
     *
     * 多个上界以列表形式给出，空列表表示无显式约束（隐式上界由语言规则决定）。
     */
    val upperBounds: List<CaType>
}
