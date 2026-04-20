package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

fun interface CaTypeParameterSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaTypeParameterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaTypeParameterSymbolRenderer = CaTypeParameterSymbolRenderer { session, symbol, declarationRenderer, printer ->

            printer{
                " ".separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(session, symbol, printer) },
                    { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(session, symbol, printer) },
                    { declarationRenderer.nameRenderer.renderName(session, symbol,declarationRenderer, printer) },


                    {
                        if (symbol.upperBounds.isNotEmpty()) {
                            withPrefix("<: ") {
                                printCollection(symbol.upperBounds) {
                                    val approximatedType = declarationRenderer.declarationTypeApproximator
                                        .approximateType(session, it, )

                                    declarationRenderer.typeRenderer.renderType(session, approximatedType, printer)
                                }
                            }
                        }
                    }
                    )
            }


        }
    }
}
