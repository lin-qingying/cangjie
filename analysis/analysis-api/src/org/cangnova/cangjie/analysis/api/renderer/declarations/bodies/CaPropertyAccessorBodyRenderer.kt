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
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaPropertyAccessorSymbol,
        printer: PrettyPrinter,
    )

    companion object {
        val NO_BODY: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, _, _ -> }

        val AS_SOURCE: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, symbol, printer ->
            val accessor = symbol.psi as? CjPropertyAccessor ?: return@CaPropertyAccessorBodyRenderer
            renderCallableSourceBody(printer, accessor.bodyExpression, accessor.hasBlockBody(), accessor)
        }

        val AS_PLACEHOLDER: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, symbol, printer ->
            val accessor = symbol.psi as? CjPropertyAccessor ?: return@CaPropertyAccessorBodyRenderer
            renderCallablePlaceholderBody(printer, accessor.bodyExpression, accessor.hasBlockBody(), accessor)
        }
    }
}
