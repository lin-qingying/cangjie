package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

/**
 * 标记型 trait：声明该符号可以持有类型参数（即"泛型声明"）。
 *
 * 典型实现：类型声明（class / interface / struct / enum）、函数、属性、`extend`、构造器。
 * 非泛型实现以空列表表达，不影响类型层逻辑。
 */
interface CaTypeParameterOwnerSymbol : CaSymbol {
    /**
     * 当前声明声明侧的类型参数列表（按源码顺序）。
     */
    val typeParameters: List<CaTypeParameterSymbol>
}
