package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol

fun interface CaFieldSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaFieldSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFieldSymbolRenderer = CaFieldSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                when {
                    symbol.isConst -> append("const")
                    symbol.isLet -> append("let")
                    else -> append("var")
                }
                append(" ")
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
            }
            declarationRenderer.variableInitializerRenderer.renderInitializer(analysisSession, symbol, printer)
        }
    }
}
