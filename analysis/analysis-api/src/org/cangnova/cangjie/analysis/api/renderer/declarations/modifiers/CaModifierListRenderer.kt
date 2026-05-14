package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 声明修饰符列表渲染器。
 *
 * 该层负责把 visibility / modality / other modifiers 聚合成一个稳定序列，
 * 并输出成可直接拼接到声明头部的文本。
 *
 * 对齐 Kotlin Analysis API 的 `KaModifierListRenderer`。
 */
fun interface CaModifierListRenderer {
    /** 将 [symbol] 的所有修饰符写入 [printer]。 */
    fun renderModifiers(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
        declarationModifiersRenderer: CaDeclarationModifiersRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 用空格分隔修饰符依次输出, 末尾补一个空格以便后续直接接续关键字/名称。
         *
         * 渲染步骤:
         * 1. 从可见性 / 模态 / 其他修饰符 provider 收集所有修饰符;
         * 2. 去重并交由 sorter 排序;
         * 3. 若结果为空则直接返回, 不产生多余空格。
         */
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
