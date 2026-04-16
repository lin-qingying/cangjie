package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol

fun interface CaNamedFunctionSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaNamedFunctionSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaNamedFunctionSymbolRenderer = CaNamedFunctionSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.functionLikeKeywordRenderer.renderFunctionLike(
                analysisSession = analysisSession,
                symbol = symbol,
                keyword = "func",
                declarationRenderer = declarationRenderer,
                printer = printer,
            )
        }

        val AS_RAW_SIGNATURE: CaNamedFunctionSymbolRenderer = CaNamedFunctionSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}
