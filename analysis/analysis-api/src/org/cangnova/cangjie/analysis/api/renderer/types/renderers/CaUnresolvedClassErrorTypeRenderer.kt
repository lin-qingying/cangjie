package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType

interface CaUnresolvedClassErrorTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaClassErrorType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 以未解析限定名形式渲染 class-like 错误类型。
         */
        val UNRESOLVED_QUALIFIER: CaUnresolvedClassErrorTypeRenderer = object : CaUnresolvedClassErrorTypeRenderer {
            override fun renderType(
                analysisSession: CaSession,
                type: CaClassErrorType,
                typeRenderer: CaTypeRenderer,
                printer: PrettyPrinter,
            ) {
                val qualifiers = type.qualifiers
                if (qualifiers.isEmpty()) {
                    typeRenderer.errorTypeRenderer.renderType(analysisSession, type, typeRenderer, printer)
                    return
                }

                printer.printCollection(qualifiers, separator = ".") { qualifier ->
                    append(qualifier.name.asString())
                    printCollectionIfNotEmpty(
                        qualifier.typeArguments,
                        prefix = "<",
                        postfix = ">",
                    ) { argument ->
                        typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, argument, typeRenderer, this)
                    }
                }
            }
        }
    }
}
