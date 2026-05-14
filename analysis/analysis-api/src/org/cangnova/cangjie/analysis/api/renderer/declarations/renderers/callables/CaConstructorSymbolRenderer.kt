package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 构造器(`init`)符号 renderer。
 *
 * 对齐 Kotlin Analysis API 的 `KaConstructorSymbolRenderer`。
 */
fun interface CaConstructorSymbolRenderer {
    /** 渲染构造器 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaConstructorSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 走完整签名渲染管线, 包含修饰符、`init` 关键字、参数列表和函数体。 */
        val AS_SOURCE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.callableSignatureRenderer
                .renderCallableSignature(analysisSession, symbol, CjTokens.INIT_KEYWORD, declarationRenderer, printer)

            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer)
        }

        /** 预设: 仅输出原始签名 `init(...)`, 用于 mangling/diff 等场景。 */
        val AS_RAW_SIGNATURE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                append("init")
                declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, this)
            }
        }
    }
}
