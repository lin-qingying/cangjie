package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.renderClassIdQualifier
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol

/**
 * class-like(类/接口/struct/enum) 渲染器。
 *
 * 关键字由调用方传入(如 `class`/`interface`), 因此一个 renderer 即可服务所有 class-like kind。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassLikeSymbolRenderer`。
 */
fun interface CaClassLikeSymbolRenderer {
    /** 用 [keyword](如 `class`、`interface`)渲染 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        keyword: String,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 按源码顺序输出修饰符、关键字、可选父限定名、类型参数、超类型列表、主体。
         *
         * 主体渲染交给 [CaDeclarationRenderer.classifierBodyRenderer]。
         */
        val AS_SOURCE: CaClassLikeSymbolRenderer = CaClassLikeSymbolRenderer { analysisSession, symbol, keyword, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append(keyword)
                append(" ")
                declarationRenderer.typeRenderer.classIdRenderer.renderClassIdQualifier(symbol.classId, this)
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.superTypeListRenderer.renderSuperTypeList(analysisSession, symbol, declarationRenderer, this)
                withPrefix(" ") {
                    declarationRenderer.classifierBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, this)
                }
            }
        }
    }
}
