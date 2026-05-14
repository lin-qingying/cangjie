package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol

/**
 * 字段符号 renderer。
 *
 * 输出 `[modifiers] (const|let|var) name: T [= init]` 形式;
 * 修饰符、初始化器等子组件由 declaration renderer 管理。
 *
 * 对齐 Kotlin Analysis API 中 property 风格的字段渲染概念(仓颉特有的 field 概念)。
 */
fun interface CaFieldSymbolRenderer {
    /** 渲染字段 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaFieldSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 按源码风格输出。
         *
         * - `const` 字段使用 `const` 关键字;
         * - 不可变字段使用 `let`, 否则使用 `var`;
         * - 类型用 `: T` 形式; 初始化器走 variableInitializerRenderer。
         */
        val AS_SOURCE: CaFieldSymbolRenderer = CaFieldSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                when {
                    symbol.isConst -> append("const")
                    symbol.isLet -> append("let")
                    else -> append("var")
                }
                append(" ")
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
            }
            declarationRenderer.variableInitializerRenderer.renderInitializer(analysisSession, symbol, printer)
        }
    }
}
