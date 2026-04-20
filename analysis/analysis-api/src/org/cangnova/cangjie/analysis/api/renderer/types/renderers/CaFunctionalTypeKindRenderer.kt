package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/**
 * 函数类型 kind 关键字渲染协议。
 */
fun interface CaFunctionalTypeKindRenderer {
    fun renderKind(typeRenderer: CaTypeRenderer, type: CaFunctionType, printer: PrettyPrinter)

    companion object {
        val NO_KEYWORDS: CaFunctionalTypeKindRenderer = CaFunctionalTypeKindRenderer { _, _, _ -> }

        val WITH_KIND_KEYWORDS: CaFunctionalTypeKindRenderer = CaFunctionalTypeKindRenderer { typeRenderer, type, printer ->
            printer {
                if (type.isCFunction) {
                    append("cfunc")
                    append(" ")
                }
                if (type.isClosureType) {
                    append("closure")
                    append(" ")
                }
            }
        }
    }
}
