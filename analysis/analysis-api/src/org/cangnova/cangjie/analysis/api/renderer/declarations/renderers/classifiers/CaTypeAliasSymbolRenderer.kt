package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol

fun interface CaTypeAliasSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaTypeAliasSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaTypeAliasSymbolRenderer = CaTypeAliasSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append("typealias")
                append(" ")
                symbol.name?.let { name ->
                    declarationRenderer.nameRenderer.renderName(analysisSession, name, symbol, declarationRenderer, this)
                } ?: append("<anonymous-alias>")
                declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                append(" = ")
                declarationRenderer.typeRenderer.renderType(analysisSession, symbol.expandedType, this)
            }
        }
    }
}
