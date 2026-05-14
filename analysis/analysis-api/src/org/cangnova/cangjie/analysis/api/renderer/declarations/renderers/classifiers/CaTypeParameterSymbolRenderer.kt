package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol

/**
 * 单个类型形参 renderer。
 *
 * 渲染 `T` / `T <: Bound` 这样的形态; 多个类型形参的整体排版由
 * [CaTypeParametersRenderer] 负责。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeParameterSymbolRenderer`。
 */
fun interface CaTypeParameterSymbolRenderer {
    /** 渲染类型形参 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaTypeParameterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 按 `annotations modifiers Name [<: Bound1 & Bound2]` 形式输出。
         *
         * 上界经过类型近似化, 多个上界以 `&` 分隔(交集语义)。
         */
        val AS_SOURCE: CaTypeParameterSymbolRenderer = CaTypeParameterSymbolRenderer { session, symbol, declarationRenderer, printer ->

            printer{
                " ".separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(session, symbol, printer) },
                    { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(session, symbol, printer) },
                    { declarationRenderer.nameRenderer.renderName(session, symbol,declarationRenderer, printer) },


                    {
                        if (symbol.upperBounds.isNotEmpty()) {
                            withPrefix("<: ") {
                                printCollection(symbol.upperBounds) {
                                    val approximatedType = declarationRenderer.declarationTypeApproximator
                                        .approximateType(session, it, )

                                    declarationRenderer.typeRenderer.renderType(session, approximatedType, printer)
                                }
                            }
                        }
                    }
                    )
            }


        }
    }
}
