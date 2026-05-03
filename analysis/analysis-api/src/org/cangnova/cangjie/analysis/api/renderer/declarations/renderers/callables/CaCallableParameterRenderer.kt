package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

fun interface CaCallableParameterRenderer {
    fun renderValueParameters(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val PARAMETERS_IN_PARENS = CaCallableParameterRenderer {
                analysisSession,
                symbol,
                declarationRenderer,
                printer,
            ->
            val valueParameters = when (symbol) {
                is CaFunctionSymbol -> symbol.valueParameters
                else -> return@CaCallableParameterRenderer
            }
            printer.printCollection(valueParameters, prefix = "(", postfix = ")") {
                declarationRenderer.renderDeclaration(analysisSession, it, printer)
            }
        }
    }
}
