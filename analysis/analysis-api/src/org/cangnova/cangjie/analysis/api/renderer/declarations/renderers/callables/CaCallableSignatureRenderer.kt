package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

/**
 * callable 整体签名 renderer。
 *
 * 集中安排 callable 头部各组件的输出顺序: 修饰符、关键字、接收者、名称、类型参数、
 * 形参列表、返回类型。具体每段的细节交给对应子 renderer。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableSignatureRenderer`。
 */
fun interface CaCallableSignatureRenderer {
    /** 写出 [symbol] 的完整签名, [keyword] 为可选的函数关键字。 */
    fun renderCallableSignature(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        keyword: CjKeywordToken?,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 对齐 Kotlin `KaCallableSignatureRenderer.FOR_SOURCE` 的职责边界：
         * 统一负责 callable 头部的关键字、名字、类型参数、值参数和返回类型分隔符。
         *
         * 其中类型参数和 `where` 子句的具体文本顺序仍遵循仓颉语法，
         * 因而保留在仓颉自己的 `typeParametersRenderer` 实现中。
         */
        val FOR_SOURCE: CaCallableSignatureRenderer = CaCallableSignatureRenderer {
                analysisSession,
                symbol,
                keyword,
                declarationRenderer,
                printer,
            ->
            printer {
                " ".separated(
                    {
                        declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                        if (keyword != null) {
                            declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, keyword, symbol, this)
                        }
                    },
                    {
                        declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, this)

                        if (symbol is CaNamedSymbol) {
                            declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                            if (symbol is CaValueParameterSymbol && symbol.isNamed) {
                                append("!")
                            }
                        }
                        if (symbol is CaTypeParameterOwnerSymbol) {
                            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                        }
                    },
                )

                declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
            }
        }
    }
}
