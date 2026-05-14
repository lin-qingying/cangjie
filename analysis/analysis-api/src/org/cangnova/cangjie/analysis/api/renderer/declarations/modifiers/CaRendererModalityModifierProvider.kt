package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

/**
 * 模态修饰符提供者。
 *
 * 决定 `open` / `sealed` / `abstract` / `final` 是否出现在输出中, 以及如何映射成关键字字面值。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererModalityModifierProvider`。
 */
fun interface CaRendererModalityModifierProvider {
    /** 返回 [symbol] 对应的模态关键字, 若不应渲染则返回 null。 */
    fun getModalityModifier(symbol: CaDeclarationSymbol): String?

    companion object {
        /**
         * 预设: 仅当模态在源码中显式书写时才输出, 隐式 `final` 等不展示。
         *
         * 适合贴近源码的展示模式。
         */
        val NO_IMPLICIT_MODALITY: CaRendererModalityModifierProvider = CaRendererModalityModifierProvider { symbol ->
            if (!symbol.isModalityExplicit) return@CaRendererModalityModifierProvider null
            when (symbol.modality) {
                CaSymbolModality.SEALED -> "sealed"
                CaSymbolModality.OPEN -> "open"
                CaSymbolModality.ABSTRACT -> "abstract"
                CaSymbolModality.FINAL,
                null,
                -> null
            }
        }

        /**
         * 预设: 始终补全模态修饰符, 包括隐式默认值。
         *
         * 对若干本身不携带模态语义的 symbol(参数、类型参数、字段、构造器、enum 构造子、
         * typealias、匿名函数、局部变量)直接跳过。
         */
        val WITH_IMPLICIT_MODALITY: CaRendererModalityModifierProvider = CaRendererModalityModifierProvider { symbol ->
            when (symbol) {
                is CaPropertyAccessorSymbol,
                is CaParameterSymbol,
                is CaFieldSymbol,
                is CaTypeParameterSymbol,
                is CaConstructorSymbol,
                is CaEnumConstructorSymbol,
                is CaTypeAliasSymbol,
                is CaAnonymousFunctionSymbol,
                is CaLocalVariableSymbol,
                -> return@CaRendererModalityModifierProvider null

                else -> {}
            }

            when (symbol.modality) {
                CaSymbolModality.FINAL -> "final"
                CaSymbolModality.SEALED -> "sealed"
                CaSymbolModality.OPEN -> "open"
                CaSymbolModality.ABSTRACT -> "abstract"
                null -> null
            }
        }
    }
}
