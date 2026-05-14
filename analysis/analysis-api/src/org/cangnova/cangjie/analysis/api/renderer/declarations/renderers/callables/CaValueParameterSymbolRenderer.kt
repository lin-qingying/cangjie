package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol

/**
 * 值参数(形参)符号 renderer。
 *
 * 对齐 Kotlin Analysis API 的 `KaValueParameterSymbolRenderer`。
 */
fun interface CaValueParameterSymbolRenderer {
    /** 渲染形参 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaValueParameterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 完整签名 + 默认值, 形如 `name: T = expr` 或 `name!: T`。 */
        val AS_SOURCE: CaValueParameterSymbolRenderer = CaValueParameterSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                " = ".separated(
                    {
                        declarationRenderer.callableSignatureRenderer
                            .renderCallableSignature(analysisSession, symbol, keyword = null, declarationRenderer, this)
                    },
                    { declarationRenderer.parameterDefaultValueRenderer.renderDefaultValue(analysisSession, symbol, this) },
                )
            }
        }

        /** 预设: 仅输出参数类型, 适合纯函数类型展示场景。 */
        val TYPE_ONLY: CaValueParameterSymbolRenderer = CaValueParameterSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.typeRenderer.renderType(analysisSession, symbol.returnType, this)
            }
        }
    }
}
