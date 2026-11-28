package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/** Quick Doc 的非模态 modifier 语义，供共享文本 renderer 与高亮 renderer 共用。 */
object CaQuickDocumentationModifierPolicy {
    /** 函数类型参数和局部绑定显示 `local`；类型参数本身不显示该前缀。 */
    fun localModifier(symbol: CaDeclarationSymbol): String? =
        if (
            symbol !is CaTypeParameterSymbol &&
            symbol is CaNamedSymbol &&
            symbol.visibility == CaSymbolVisibility.LOCAL
        ) {
            "local"
        } else {
            null
        }
}
