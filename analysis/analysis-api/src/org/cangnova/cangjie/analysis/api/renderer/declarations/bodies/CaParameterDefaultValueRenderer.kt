package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.psi.CjParameter

/**
 * 值参数默认值 renderer。
 *
 * 按 Kotlin analysis-api 的同构架构，
 * 这一层只消费 analysis-api 已公开的稳定默认值语义，
 * 不直接回读 PSI，也不要求符号暴露默认值源码文本。
 */
fun interface CaParameterDefaultValueRenderer {
    fun renderDefaultValue(
        analysisSession: CaSession,
        symbol: CaValueParameterSymbol,
        printer: PrettyPrinter,
    )

    companion object {
        val NO_DEFAULT_VALUE: CaParameterDefaultValueRenderer = CaParameterDefaultValueRenderer { _, _, _ -> }

        /**
         * source preset 读取参数源码中的默认值文本。
         */
        val AS_SOURCE: CaParameterDefaultValueRenderer = CaParameterDefaultValueRenderer { _, symbol, printer ->
            val parameter = symbol.psi as? CjParameter ?: return@CaParameterDefaultValueRenderer
            parameter.defaultValue?.let { defaultValue ->
                printer.append(defaultValue.text)
            }
        }

        /**
         * 当符号只公开“存在默认值”这一稳定语义时，
         * 用 `...` 表达“此处存在默认值，但 analysis-api 不公开其源码”。
         */
        val THREE_DOTS: CaParameterDefaultValueRenderer = CaParameterDefaultValueRenderer { _, symbol, printer ->
            if (symbol.hasDefaultValue) {
                printer.append("...")
            }
        }
    }
}
