package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

/**
 * 可见性修饰符提供者。
 *
 * 把 [CaSymbolVisibility] 映射为输出关键字; 同时决定何时省略 `public` 等隐式默认值。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererVisibilityModifierProvider`。
 */
fun interface CaRendererVisibilityModifierProvider {
    /** 返回 [symbol] 对应的可见性关键字, 若不应渲染则返回 null。 */
    fun getVisibilityModifier(symbol: CaDeclarationSymbol): String?

    companion object {
        /**
         * 预设: 仅当源码中显式书写了可见性时才输出。
         *
         * 默认 `public` 等隐式可见性会被省略。
         */
        val NO_IMPLICIT_VISIBILITY: CaRendererVisibilityModifierProvider = CaRendererVisibilityModifierProvider { symbol ->
            if (!symbol.isVisibilityExplicit) return@CaRendererVisibilityModifierProvider null
            when (symbol.visibility) {
                CaSymbolVisibility.PRIVATE -> "private"
                CaSymbolVisibility.PRIVATE_TO_THIS -> "private"
                CaSymbolVisibility.PROTECTED -> "protected"
                CaSymbolVisibility.INTERNAL -> "internal"
                CaSymbolVisibility.LOCAL -> "local"
                CaSymbolVisibility.PUBLIC,
                CaSymbolVisibility.UNKNOWN,
                -> null
            }
        }

        /**
         * 预设: 始终补全可见性, 包括默认 `public`。
         *
         * 类型参数 / 参数 / 匿名函数等天然不携带可见性的 symbol 仍然跳过。
         */
        val WITH_IMPLICIT_VISIBILITY: CaRendererVisibilityModifierProvider = CaRendererVisibilityModifierProvider { symbol ->
            when (symbol) {
                is CaTypeParameterSymbol,
                is CaParameterSymbol,
                is CaAnonymousFunctionSymbol,
                -> return@CaRendererVisibilityModifierProvider null

                else -> {}
            }

            when (symbol.visibility) {
                CaSymbolVisibility.PRIVATE -> "private"
                CaSymbolVisibility.PRIVATE_TO_THIS -> "private"
                CaSymbolVisibility.PROTECTED -> "protected"
                CaSymbolVisibility.INTERNAL -> "internal"
                CaSymbolVisibility.LOCAL -> null
                CaSymbolVisibility.PUBLIC,
                CaSymbolVisibility.UNKNOWN,
                -> "public"
            }
        }
    }
}
