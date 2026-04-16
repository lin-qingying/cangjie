package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.types.CaType

fun interface CaSuperTypeRenderer {
    fun renderSuperType(
        analysisSession: CaSession,
        superType: CaType,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
