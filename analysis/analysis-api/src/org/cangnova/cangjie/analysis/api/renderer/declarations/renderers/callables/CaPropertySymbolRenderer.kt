package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 属性符号 renderer。
 *
 * 对齐 Kotlin Analysis API 的 `KaPropertySymbolRenderer`。
 */
fun interface CaPropertySymbolRenderer {
    /** 渲染属性 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaPropertySymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 完整签名 + 初始化器 + 访问器(getter/setter)。 */
        val AS_SOURCE: CaPropertySymbolRenderer = CaPropertySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.callableSignatureRenderer
                    .renderCallableSignature(analysisSession, symbol, CjTokens.PROP_KEYWORD, declarationRenderer, this)
            }
            declarationRenderer.variableInitializerRenderer.renderInitializer(analysisSession, symbol, printer)
            declarationRenderer.propertyAccessorsRenderer.renderAccessors(analysisSession, symbol, declarationRenderer, printer)
        }

        /** 预设: 仅输出"原始签名"(接收者 + 名字 + 返回类型), 适合 mangling/diff。 */
        val AS_RAW_SIGNATURE: CaPropertySymbolRenderer = CaPropertySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, this)
                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, this)
                }
            }
        }
    }
}
