package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

context(session: CaSession)
fun CaDeclarationSymbol.render(
    renderer: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES,
): String {
    return with(session) {
        render(
            renderer = renderer,
        )
    }
}

interface CaRenderer : CaSessionComponent {
    fun CaDeclarationSymbol.render(
        renderer: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES,
    ): String

    fun CaType.render(
        renderer: CaTypeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES,
    ): String
}
