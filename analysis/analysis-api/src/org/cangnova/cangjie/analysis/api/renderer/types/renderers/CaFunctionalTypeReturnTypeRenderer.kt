package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/**
 * 函数类型返回类型渲染协议。
 */
fun interface CaFunctionalTypeReturnTypeRenderer {
    fun renderReturnType(
        analysisSession: CaSession,
        typeRenderer: CaTypeRenderer,
        type: CaFunctionType,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFunctionalTypeReturnTypeRenderer = CaFunctionalTypeReturnTypeRenderer { analysisSession, renderer, type, printer ->
            printer {
                append(" -> ")
                renderer.renderType(analysisSession, type.returnType, this)
            }
        }
    }
}
