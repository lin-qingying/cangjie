package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol

/**
 * 函数关键字 renderer。
 *
 * 用于在 `CaDeclarationRenderer` 中作为"兜底"路径渲染函数类(`CaFunctionSymbol`)符号:
 * 当没有更具体的子 renderer 时, 通过这一层统一输出"修饰符 + 关键字 + 名字 + 形参 + 返回类型 + body"。
 *
 * 对齐 Kotlin Analysis API 的 `KaFunctionLikeKeywordRenderer`。
 */
fun interface CaFunctionLikeKeywordRenderer {
    /** 用关键字 [keyword](例如 `func` / `finalizer`) 渲染 [symbol]。 */
    fun renderFunctionLike(
        analysisSession: CaSession,
        symbol: CaFunctionSymbol,
        keyword: String,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 按源码顺序写出 modifiers / mut / const / keyword / receiver / name /
         * 类型参数 / 形参 / 返回类型 / 函数体。
         */
        val AS_SOURCE: CaFunctionLikeKeywordRenderer = CaFunctionLikeKeywordRenderer { analysisSession, symbol, keyword, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                if (symbol.isMutating) {
                    append("mut")
                    append(" ")
                }
                if (symbol.isConst) {
                    append("const")
                    append(" ")
                }
                append(keyword)
                append(" ")
                declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, this)
                when (symbol) {
                    is CaConstructorSymbol -> append("init")
                    is org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol -> append("finalizer")
                    is CaNamedSymbol -> declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                    else -> append("<anonymous>")
                }
                if (symbol is CaTypeParameterOwnerSymbol) {
                    declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                }
                if (symbol is CaValueParameterOwnerSymbol) {
                    declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
                }
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
                declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, this)
            }
        }
    }
}
