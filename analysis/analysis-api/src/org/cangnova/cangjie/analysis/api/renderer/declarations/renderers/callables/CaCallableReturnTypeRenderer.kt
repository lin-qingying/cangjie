package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol

fun interface CaCallableReturnTypeRenderer {
    fun renderReturnType(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val WITH_OUT_APPROXIMATION = CaCallableReturnTypeRenderer {
                analysisSession: CaSession,
                symbol: CaCallableSymbol,
                declarationRenderer: CaDeclarationRenderer,
                printer: PrettyPrinter,
            ->
            if (symbol is CaConstructorSymbol) return@CaCallableReturnTypeRenderer
            val type = declarationRenderer.declarationTypeApproximator.approximateType(
                analysisSession,
                symbol.returnType,
            )
            if (!declarationRenderer.returnTypeFilter.shouldRenderReturnType(analysisSession, type, symbol)) return@CaCallableReturnTypeRenderer
            declarationRenderer.typeRenderer.renderType(analysisSession, type, printer)
        }
    }
}
