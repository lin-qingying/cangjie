package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 修饰符排序器。
 *
 * 仓颉当前没有公开的语法级修饰符排序基础设施，
 * 因此这里直接以语言级关键字顺序定义稳定输出顺序。
 */
fun interface CaModifiersSorter {
    fun sort(
        analysisSession: CaSession,
        modifiers: List<String>,
        owner: CaDeclarationSymbol,
    ): List<String>

    companion object {
        private val canonicalOrder = listOf(
            "public",
            "protected",
            "internal",
            "private",
            "sealed",
            "abstract",
            "open",
            "final",
            "static",
            "const",
            "mut",
            "override",
            "operator",
            "unsafe",
            "foreign",
            "local",
        ).withIndex().associate { (index, modifier) -> modifier to index }

        val CANONICAL: CaModifiersSorter = CaModifiersSorter { _, modifiers, _ ->
            modifiers.sortedWith(
                compareBy<String> { canonicalOrder[it] ?: Int.MAX_VALUE }
                    .thenBy { it }
            )
        }
    }
}
