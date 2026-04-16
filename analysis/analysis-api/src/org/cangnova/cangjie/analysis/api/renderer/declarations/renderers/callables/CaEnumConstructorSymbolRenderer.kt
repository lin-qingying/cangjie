package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol

/**
 * 枚举构造器 renderer。
 *
 * 这里严格基于公开语义渲染：
 * - 名字来自 symbol 本身；
 * - payload 来自 `payloadTypes`，不再回退到 PSI 文本拼装。
 */
fun interface CaEnumConstructorSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaEnumConstructorSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaEnumConstructorSymbolRenderer =
            CaEnumConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
                printer {
                    declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                    declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, this)
                    printCollectionIfNotEmpty(
                        symbol.payloadTypes,
                        prefix = "(",
                        postfix = ")",
                    ) { payloadType ->
                        declarationRenderer.typeRenderer.renderType(analysisSession, payloadType, this)
                    }
                }
            }
    }
}
