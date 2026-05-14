package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 修饰符排序器。
 *
 * 仓颉当前没有公开的语法级修饰符排序基础设施，
 * 因此这里直接以语言级关键字顺序定义稳定输出顺序。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererModifiersSorter`。
 */
fun interface CaModifiersSorter {
    /** 对 [modifiers] 进行排序并返回新列表。 */
    fun sort(
        analysisSession: CaSession,
        modifiers: List<String>,
        owner: CaDeclarationSymbol,
    ): List<String>

    companion object {
        /** 仓颉语言级关键字的"规范顺序", 用于在 [CANONICAL] 中查表。 */
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

        /**
         * 预设: 按 [canonicalOrder] 给出的语言级顺序排序;
         * 未在表中出现的修饰符放最后, 并按字典序稳定排列。
         */
        val CANONICAL: CaModifiersSorter = CaModifiersSorter { _, modifiers, _ ->
            modifiers.sortedWith(
                compareBy<String> { canonicalOrder[it] ?: Int.MAX_VALUE }
                    .thenBy { it }
            )
        }
    }
}
