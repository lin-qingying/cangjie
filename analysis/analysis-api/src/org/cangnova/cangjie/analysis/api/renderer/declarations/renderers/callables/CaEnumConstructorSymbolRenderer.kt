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
 *
 * 对齐 Kotlin Analysis API 的 `KaEnumEntryInitializerRenderer`(语义略有差异)。
 */
fun interface CaEnumConstructorSymbolRenderer {
    /** 渲染 enum 构造子 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaEnumConstructorSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 输出 `Name` 或 `Name(T1, T2)` 形式。
         *
         * payload 为空时不输出括号; 修饰符通过 declaration modifiers renderer 写出。
         */
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
