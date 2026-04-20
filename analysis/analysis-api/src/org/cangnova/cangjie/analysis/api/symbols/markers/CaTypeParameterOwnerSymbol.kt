package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

/**
 * 持有类型参数的公开符号能力接口。
 */
interface CaTypeParameterOwnerSymbol : CaSymbol {
    val typeParameters: List<CaTypeParameterSymbol>
}
