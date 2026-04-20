package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend

fun interface CaExtendSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaExtendSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaExtendSymbolRenderer = CaExtendSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append("extend")
                append(" ")
                declarationRenderer.typeRenderer.renderType(analysisSession, symbol.extendedType, this)
                if (symbol.superTypes.isNotEmpty()) {
                    append(" <: ")
                    printCollection(
                        symbol.superTypes,
                        separator = " & ",
                    ) { superType ->
                        declarationRenderer.typeRenderer.renderType(analysisSession, superType, this)
                    }
                }
            }
            val members = with(analysisSession) {
                val extendPsi = symbol.getOriginalPsi() as? CjExtend
                extendPsi?.declarations
                    ?.mapNotNull { declaration ->
                        (declaration as? CjDeclaration)?.let { it.symbol as? org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol }
                    }
                    .orEmpty()
            }
            if (members.isEmpty()) {
                printer.append(" {}")
                return@CaExtendSymbolRenderer
            }
            printer.appendLine(" {")
            printer.withIndent {
                members.forEachIndexed { index, member ->
                    if (index > 0) printer.appendLine()
                    printer.append(declarationRenderer.renderDeclaration(analysisSession, member))
                }
            }
            printer.append("}")
        }
    }
}
