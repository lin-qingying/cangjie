package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 对齐 Kotlin `KaPropertyGetterSymbolRenderer` 的 getter 叶子 renderer。
 */
fun interface CaPropertyGetterSymbolRenderer {
    /** 渲染 getter [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaPropertyGetterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 输出 `get()` 头部, 后跟 accessor body renderer 写出的 body。
         *
         * 修饰符与头部以空格分隔; 形参始终为空, 但仍走 valueParametersRenderer 以保持 hook。
         */
        val AS_SOURCE: CaPropertyGetterSymbolRenderer = CaPropertyGetterSymbolRenderer {
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
                        declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, CjTokens.GET_KEYWORD, symbol, this)
                        declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
                    },
                )
            }
            declarationRenderer.accessorBodyRenderer.renderBody(analysisSession, symbol, printer)
        }
    }
}
