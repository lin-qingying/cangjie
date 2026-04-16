package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol

fun interface CaCallableParameterRenderer {
    fun renderParameters(
        analysisSession: CaSession,
        symbol: CaValueParameterOwnerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
