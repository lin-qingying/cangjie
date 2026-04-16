package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

internal fun renderValueParameterSource(
    analysisSession: CaSession,
    symbol: CaValueParameterSymbol,
    declarationRenderer: CaDeclarationRenderer,
    printer: PrettyPrinter,
) {
    declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer)
    declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)
    printer {
        if (symbol.isNamed) {
            append("!")
        }
        append(if (declarationRenderer.codeStyle.spaceAfterColon) ": " else ":")
        declarationRenderer.typeRenderer.renderType(
            analysisSession,
            declarationRenderer.declarationTypeApproximator.approximateType(
                analysisSession,
                symbol.returnType,
            ),
            this,
        )
    }
    declarationRenderer.parameterDefaultValueRenderer.renderDefaultValue(analysisSession, symbol, printer)
}

internal fun CaCallableSymbol.renderNameText(): String = when (this) {
    is CaConstructorSymbol -> "init"
    is CaFinalizerSymbol -> "~init"
    is CaNamedSymbol -> name.asString()
    else -> "<anonymous>"
}
