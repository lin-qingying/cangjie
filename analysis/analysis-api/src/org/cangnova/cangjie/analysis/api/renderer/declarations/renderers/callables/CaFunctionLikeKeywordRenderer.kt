package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol

fun interface CaFunctionLikeKeywordRenderer {
    fun renderFunctionLike(
        analysisSession: CaSession,
        symbol: CaFunctionSymbol,
        keyword: String,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFunctionLikeKeywordRenderer = CaFunctionLikeKeywordRenderer { analysisSession, symbol, keyword, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                if (symbol.isMutating) {
                    append("mut")
                    append(" ")
                }
                if (symbol.isConst) {
                    append("const")
                    append(" ")
                }
                append(keyword)
                append(" ")
                declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, this)
                when (symbol) {
                    is CaConstructorSymbol -> append("init")
                    is org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol -> append("finalizer")
                    is CaNamedSymbol -> declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                    else -> append("<anonymous>")
                }
                if (symbol is CaTypeParameterOwnerSymbol) {
                    declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                }
                if (symbol is CaValueParameterOwnerSymbol) {
                    declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
                }
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
                declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, this)
            }
        }
    }
}
