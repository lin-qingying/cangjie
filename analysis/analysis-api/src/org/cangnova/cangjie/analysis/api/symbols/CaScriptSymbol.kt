package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/**
 * script 的公开语义视图。
 *
 * script 是 file 和其顶层声明之间的语义容器。
 */
interface CaScriptSymbol : CaDeclarationSymbol, CaDeclarationContainerSymbol, CaNamedSymbol {
    val fileSymbol: CaFileSymbol?
}
