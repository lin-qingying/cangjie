package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjParameter

/**
 * 对齐 Kotlin `KaPropertySetterSymbolRenderer` 的 setter 叶子 renderer。
 */
fun interface CaPropertySetterSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaPropertySetterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 仓颉 setter 头部语法只保留参数名，不回显参数类型。
         *
         * Kotlin 的 source renderer 会把 setter 参数当作普通 value parameter 渲染，
         * 因而输出 `set(value: T)`；仓颉语法则是 `set(value)`。
         * 这里保持 Kotlin 的 renderer 落点不变，但在仓颉 setter 叶子层切换为仓颉语法头部。
         */
        val AS_SOURCE: CaPropertySetterSymbolRenderer = CaPropertySetterSymbolRenderer {
                analysisSession,
                symbol,
                declarationRenderer,
                printer,
            ->
            printer {
                " ".separated(
                    {
                        declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                    },
                    {
                        declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, CjTokens.SET_KEYWORD, symbol, this)
                        renderSourceParameterList(symbol)
                    },
                )
            }
            declarationRenderer.accessorBodyRenderer.renderBody(analysisSession, symbol, printer)
        }

        /**
         * placeholder 细节下，setter 参数位置只保留省略号占位。
         */
        val WITH_PARAMETER_PLACEHOLDER: CaPropertySetterSymbolRenderer = CaPropertySetterSymbolRenderer {
                analysisSession,
                symbol,
                declarationRenderer,
                printer,
            ->
            printer {
                " ".separated(
                    {
                        declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                    },
                    {
                        declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, CjTokens.SET_KEYWORD, symbol, this)
                        append("(...)")
                    },
                )
            }
            declarationRenderer.accessorBodyRenderer.renderBody(analysisSession, symbol, printer)
        }

        private fun PrettyPrinter.renderSourceParameterList(symbol: CaPropertySetterSymbol) {
            val parameterText = (symbol.parameter.psi as? CjParameter)?.text ?: symbol.parameter.name.asString()
            printCollection(listOf(parameterText), prefix = "(", postfix = ")") { text ->
                append(text)
            }
        }
    }
}
