package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 类型参数的公开语义视图。
 */
interface CaTypeParameterSymbol : CaClassifierSymbol, CaNamedSymbol {
    val upperBounds: List<CaType>
}
