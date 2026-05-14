package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol

/**
 * "其他修饰符"提供者。
 *
 * 集中处理仓颉特有的声明前缀修饰符: `static` / `const` / `mut` / `override` /
 * `operator` / `unsafe` / `foreign` 等, 由具体声明 kind 决定哪些适用。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererOtherModifiersProvider`。
 */
fun interface CaRendererOtherModifiersProvider {
    /** 返回 [symbol] 应当渲染的所有"非可见性、非模态"修饰符列表。 */
    fun getOtherModifiers(symbol: CaDeclarationSymbol): List<String>

    companion object {
        /** 预设: 输出全部"其他修饰符", 包括 `const` 与 `mut`。 */
        val ALL: CaRendererOtherModifiersProvider = CaRendererOtherModifiersProvider { symbol ->
            buildList {
                addAll(symbol.renderedOtherModifiers(includeConst = true, includeMutating = true))
            }
        }

        /**
         * 预设: 仅渲染贴在声明头部的前缀修饰符, 屏蔽 `const` / `mut` 这种"贴近类型"的修饰符。
         *
         * 用于一些只关心声明前缀关键字的场景。
         */
        val DECLARATION_PREFIX_ONLY: CaRendererOtherModifiersProvider = CaRendererOtherModifiersProvider { symbol ->
            buildList {
                addAll(symbol.renderedOtherModifiers(includeConst = false, includeMutating = false))
            }
        }
    }
}

/**
 * 按声明 kind 计算适用的"其他修饰符"集合。
 *
 * [includeConst] / [includeMutating] 决定 `const` 与 `mut` 是否参与输出,
 * 仅供本文件内部预设使用。
 */
private fun CaDeclarationSymbol.renderedOtherModifiers(
    includeConst: Boolean,
    includeMutating: Boolean,
): List<String> = when (this) {
    is CaFunctionSymbol -> buildList {
        if (isStatic) add("static")
        if (includeConst && isConst) add("const")
        if (includeMutating && isMutating) add("mut")
        if (isOverride) add("override")
        if (isOperator) add("operator")
        if (isUnsafe) add("unsafe")
        if (isForeign) add("foreign")
    }

    is CaPropertySymbol -> buildList {
        if (isStatic) add("static")
        if (includeConst && isConst) add("const")
        if (includeMutating && isMutating) add("mut")
        if (isOverride) add("override")
        if (isUnsafe) add("unsafe")
        if (isForeign) add("foreign")
    }

    is CaFieldSymbol -> buildList {
        if (isStatic) add("static")
        if (includeConst && isConst) add("const")
    }

    else -> emptyList()
}
