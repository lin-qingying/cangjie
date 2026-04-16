package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

fun interface CaTypeParametersRenderer {
    fun renderTypeParameters(
        analysisSession: CaSession,
        owner: CaTypeParameterOwnerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
