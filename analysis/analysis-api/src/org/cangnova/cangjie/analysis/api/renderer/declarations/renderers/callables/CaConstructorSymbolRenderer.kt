package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.lexer.CjTokens

fun interface CaConstructorSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaConstructorSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.callableSignatureRenderer
                .renderCallableSignature(analysisSession, symbol, CjTokens.INIT_KEYWORD, declarationRenderer, printer)

            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer)
        }

        val AS_RAW_SIGNATURE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                append("init")
                declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
            }
        }
    }
}
