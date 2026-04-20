package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol

fun interface CaSuperTypeListRenderer {
    fun renderSuperTypeList(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
