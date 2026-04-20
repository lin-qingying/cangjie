package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name

/**
 * 具备稳定名称的公开符号能力接口。
 */
interface CaNamedSymbol : CaSymbol {
    val name: Name
}
