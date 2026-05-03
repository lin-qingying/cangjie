package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.renderClassIdQualifier
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol

fun interface CaClassLikeSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        keyword: String,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaClassLikeSymbolRenderer = CaClassLikeSymbolRenderer { analysisSession, symbol, keyword, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append(keyword)
                append(" ")
                declarationRenderer.typeRenderer.classIdRenderer.renderClassIdQualifier(symbol.classId, this)
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.superTypeListRenderer.renderSuperTypeList(analysisSession, symbol, declarationRenderer, this)
                withPrefix(" ") {
                    declarationRenderer.classifierBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, this)
                }
            }
        }
    }
}
