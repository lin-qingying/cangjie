package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.lexer.CjTokens

fun interface CaPropertySymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaPropertySymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaPropertySymbolRenderer = CaPropertySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.callableSignatureRenderer
                    .renderCallableSignature(analysisSession, symbol, CjTokens.PROP_KEYWORD, declarationRenderer, this)
            }
            declarationRenderer.variableInitializerRenderer.renderInitializer(analysisSession, symbol, printer)
            declarationRenderer.propertyAccessorsRenderer.renderAccessors(analysisSession, symbol, declarationRenderer, printer)
        }

        val AS_RAW_SIGNATURE: CaPropertySymbolRenderer = CaPropertySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
            }
        }
    }
}
