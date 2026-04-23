package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol

fun interface CaValueParameterSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaValueParameterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaValueParameterSymbolRenderer = CaValueParameterSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->


            printer {
                " = ".separated(
                    {
                        declarationRenderer.callableSignatureRenderer
                            .renderCallableSignature(analysisSession, symbol, keyword = null, declarationRenderer, printer)
                    },
                    { declarationRenderer.parameterDefaultValueRenderer.renderDefaultValue(analysisSession, symbol, printer) },
                )
            }
        }
    }
}
