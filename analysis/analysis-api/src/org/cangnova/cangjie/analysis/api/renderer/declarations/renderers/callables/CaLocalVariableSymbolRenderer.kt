package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol

/**
 * 局部变量 renderer。
 *
 * 输出 `let|var name: T [= init]` 形式; 与字段不同, 这里不带 `const`。
 *
 * 对齐 Kotlin Analysis API 的 `KaLocalVariableSymbolRenderer`。
 */
fun interface CaLocalVariableSymbolRenderer {
    /** 渲染局部变量 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaVariableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 按源码顺序输出修饰符、`let`/`var`、名字、类型与初始化器。 */
        val AS_SOURCE: CaLocalVariableSymbolRenderer = CaLocalVariableSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                val modifiersStartOffset = builder.length
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                // 不同 modifier renderer 的尾随空白约定不同；这里保证与 let/var 之间恰好有分隔。
                if (
                    builder.length > modifiersStartOffset &&
                    builder.lastOrNull()?.isWhitespace() != true
                ) {
                    append(" ")
                }
                append(if (symbol.isLet) "let" else "var")
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
