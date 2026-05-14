package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol

/**
 * 标记型 trait：声明该符号持有值参数列表。
 *
 * 主要由函数族 ([CaFunctionSymbol][org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol])
 * 实现：函数、构造器、属性 setter 等。变量族不实现该接口。
 */
interface CaValueParameterOwnerSymbol : CaSymbol {
    /**
     * 当前 callable 的值参数列表（按源码顺序）。
     */
    val valueParameters: List<CaValueParameterSymbol>
}
