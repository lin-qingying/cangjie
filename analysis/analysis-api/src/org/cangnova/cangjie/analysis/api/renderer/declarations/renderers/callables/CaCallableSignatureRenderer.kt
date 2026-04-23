package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.lexer.CjKeywordToken

fun interface CaCallableSignatureRenderer {

    public fun renderCallableSignature(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        keyword: CjKeywordToken?,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object{
        val FOR_SOURCE = CaCallableSignatureRenderer{
                analysisSession: CaSession,
                symbol: CaCallableSymbol,
                keyword: CjKeywordToken?,
                declarationRenderer: CaDeclarationRenderer,
                printer: PrettyPrinter, ->
        }
    }
}
