package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.psi.CjPropertyAccessor

/**
 * 属性访问器体 renderer。
 *
 * 对齐 Kotlin `KaPropertyAccessorBodyRenderer`：body slot 直接消费 accessor symbol。
 */
fun interface CaPropertyAccessorBodyRenderer {
    /** 渲染访问器 [symbol] 的函数体到 [printer]。 */
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaPropertyAccessorSymbol,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 不渲染访问器体, 仅保留头部 `get`/`set`。 */
        val NO_BODY: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, _, _ -> }

        /** 预设: 渲染源码中真实的访问器体文本(块体或表达式体)。 */
        val AS_SOURCE: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, symbol, printer ->
            val accessor = symbol.psi as? CjPropertyAccessor ?: return@CaPropertyAccessorBodyRenderer
            renderCallableSourceBody(printer, accessor.bodyExpression, accessor.hasBlockBody(), accessor)
        }

        /** 预设: 用 `{ ... }` / `= ...` 占位, 屏蔽访问器内部细节。 */
        val AS_PLACEHOLDER: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, symbol, printer ->
            val accessor = symbol.psi as? CjPropertyAccessor ?: return@CaPropertyAccessorBodyRenderer
            renderCallablePlaceholderBody(printer, accessor.bodyExpression, accessor.hasBlockBody(), accessor)
        }
    }
}
