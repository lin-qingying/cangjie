package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 命名函数(顶层/成员函数)渲染器。
 *
 * 对齐 Kotlin Analysis API 的 `KaNamedFunctionSymbolRenderer`。
 */
fun interface CaNamedFunctionSymbolRenderer {
    /** 渲染函数 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaNamedFunctionSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 完整签名 + 函数体, 关键字使用 `func`。 */
        val AS_SOURCE: CaNamedFunctionSymbolRenderer = CaNamedFunctionSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.callableSignatureRenderer
                .renderCallableSignature(analysisSession, symbol, CjTokens.FUNC_KEYWORD, declarationRenderer, printer)

            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer)
        }

        /** 预设: 仅输出"原始签名"(接收者 + 名字 + 类型形参 + 形参 + 返回类型), 适合 mangling/diff。 */
        val AS_RAW_SIGNATURE: CaNamedFunctionSymbolRenderer = CaNamedFunctionSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
            }
        }
    }
}
