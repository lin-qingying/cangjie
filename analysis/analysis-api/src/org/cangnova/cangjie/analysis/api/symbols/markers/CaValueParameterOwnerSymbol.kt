package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol

/**
 * 持有值参数的公开符号能力接口。
 */
interface CaValueParameterOwnerSymbol : CaSymbol {
    val valueParameters: List<CaValueParameterSymbol>
}
