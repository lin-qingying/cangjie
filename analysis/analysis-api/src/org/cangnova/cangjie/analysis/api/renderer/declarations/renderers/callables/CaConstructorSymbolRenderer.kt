package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol

fun interface CaConstructorSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaConstructorSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append("init")
                declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, this)
            }
        }

        val AS_RAW_SIGNATURE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                append("init")
                declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, this)
            }
        }
    }
}
