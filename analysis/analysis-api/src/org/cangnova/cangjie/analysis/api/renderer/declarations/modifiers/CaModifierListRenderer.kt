package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 声明修饰符列表渲染器。
 *
 * 该层负责把 visibility / modality / other modifiers 聚合成一个稳定序列，
 * 并输出成可直接拼接到声明头部的文本。
 */
fun interface CaModifierListRenderer {
    fun renderModifiers(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
        declarationModifiersRenderer: CaDeclarationModifiersRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_LIST: CaModifierListRenderer = CaModifierListRenderer { analysisSession, symbol, declarationModifiersRenderer, printer ->
            val modifiers = buildList {
                declarationModifiersRenderer.visibilityProvider.getVisibilityModifier(symbol)?.let(::add)
                declarationModifiersRenderer.modalityProvider.getModalityModifier(symbol)?.let(::add)
                addAll(declarationModifiersRenderer.otherModifiersProvider.getOtherModifiers(symbol))
            }
                .distinct()
                .let { declarationModifiersRenderer.modifiersSorter.sort(analysisSession, it, symbol) }
                .ifEmpty { return@CaModifierListRenderer }

            printer {
                printCollection(modifiers, separator = " ") { modifier ->
                    append(modifier)
                }
                append(" ")
            }
        }
    }
}
