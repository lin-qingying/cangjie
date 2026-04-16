package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderNameText
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol

object CaCallableSignatureRendererForSource {
    val FOR_SOURCE: CaCallableSignatureRenderer = CaCallableSignatureRenderer { analysisSession, symbol, declarationRenderer, printer ->
        declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
        when (symbol) {
            is CaNamedSymbol -> declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)
            else -> printer.append(symbol.renderNameText())
        }
        if (symbol is CaTypeParameterOwnerSymbol) {
            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
        }
        if (symbol is CaValueParameterOwnerSymbol) {
            declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
        }
        declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
    }
}
