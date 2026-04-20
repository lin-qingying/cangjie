package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol

fun interface CaClassifierBodyRenderer {
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
