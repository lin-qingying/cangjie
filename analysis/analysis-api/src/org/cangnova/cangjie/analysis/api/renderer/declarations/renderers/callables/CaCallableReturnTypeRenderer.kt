package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol

/**
 * callable 返回类型 renderer。
 *
 * 决定 `: T` 的写法; 构造器没有返回类型, 由实现直接跳过。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableReturnTypeRenderer`。
 */
fun interface CaCallableReturnTypeRenderer {
    /** 写出 [symbol] 的返回类型到 [printer]。 */
    fun renderReturnType(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 经过类型近似化后输出, 并尊重 [CaDeclarationRenderer.returnTypeFilter];
         * 构造器始终跳过。
         */
        val WITH_OUT_APPROXIMATION = CaCallableReturnTypeRenderer {
                analysisSession: CaSession,
                symbol: CaCallableSymbol,
                declarationRenderer: CaDeclarationRenderer,
                printer: PrettyPrinter,
            ->
            if (symbol is CaConstructorSymbol) return@CaCallableReturnTypeRenderer
            val type = declarationRenderer.declarationTypeApproximator.approximateType(
                analysisSession,
                symbol.returnType,
            )
            if (!declarationRenderer.returnTypeFilter.shouldRenderReturnType(analysisSession, type, symbol)) return@CaCallableReturnTypeRenderer
            declarationRenderer.typeRenderer.renderType(analysisSession, type, printer)
        }
    }
}
