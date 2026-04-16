package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/**
 * 函数类型参数列表渲染协议。
 */
fun interface CaFunctionalTypeParameterListRenderer {
    fun renderParameters(
        analysisSession: CaSession,
        typeRenderer: CaTypeRenderer,
        type: CaFunctionType,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFunctionalTypeParameterListRenderer = CaFunctionalTypeParameterListRenderer { analysisSession, renderer, type, printer ->
            printer {
                append("(")
                printCollection(type.parameterTypes) { parameterType ->
                    renderer.renderType(analysisSession, parameterType, this)
                }
                if (type.hasVariableLengthArgument) {
                    if (type.parameterTypes.isNotEmpty()) {
                        append(", ")
                    }
                    append("...")
                }
                append(")")
            }
        }
    }
}
