package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

/**
 * callable 形参列表 renderer。
 *
 * 决定 `(p1: T1, p2: T2)` 这段如何输出。具体单个参数的渲染交由
 * [CaDeclarationRenderer.valueParameterRenderer] 处理。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableParameterRenderer`。
 */
fun interface CaCallableParameterRenderer {
    /** 把 [symbol] 的全部形参写入 [printer]。 */
    fun renderValueParameters(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 用圆括号包裹参数列表, 逐个委托给 declarationRenderer 渲染。
         *
         * 仅处理 [CaFunctionSymbol]; 其他 callable kind 直接跳过。
         */
        val PARAMETERS_IN_PARENS = CaCallableParameterRenderer {
                analysisSession,
                symbol,
                declarationRenderer,
                printer,
            ->
            val valueParameters = when (symbol) {
                is CaFunctionSymbol -> symbol.valueParameters
                else -> return@CaCallableParameterRenderer
            }
            printer.printCollection(valueParameters, prefix = "(", postfix = ")") {
                declarationRenderer.renderDeclaration(analysisSession, it, printer)
            }
        }
    }
}
