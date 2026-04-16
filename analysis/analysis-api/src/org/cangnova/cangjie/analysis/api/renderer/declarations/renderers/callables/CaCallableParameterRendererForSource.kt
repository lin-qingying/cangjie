package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderValueParameterSource

object CaCallableParameterRendererForSource {
    val PARAMETERS_IN_PARENS: CaCallableParameterRenderer = CaCallableParameterRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val parameterSeparator = if (declarationRenderer.codeStyle.spaceAfterComma) ", " else ","
        printer.append(
            symbol.valueParameters.joinToString(prefix = "(", postfix = ")", separator = parameterSeparator) { parameter ->
                prettyPrint {
                    renderValueParameterSource(
                        analysisSession = analysisSession,
                        symbol = parameter,
                        declarationRenderer = declarationRenderer,
                        printer = this,
                    )
                }.trim()
            },
        )
    }
}
