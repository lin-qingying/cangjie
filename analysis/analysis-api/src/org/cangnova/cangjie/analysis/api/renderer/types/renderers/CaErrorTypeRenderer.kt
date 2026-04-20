package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaErrorType

interface CaErrorTypeRenderer {
    fun renderType(
        analysisSession: CaSession,
        type: CaErrorType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    object AS_CODE_IF_POSSIBLE : CaErrorTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaErrorType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.presentableText?.let {
                printer.append(it)
                return
            }
            printer.append("ERROR")
        }
    }

    object AS_ERROR_WORD : CaErrorTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaErrorType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR")
        }
    }

    object WITH_ERROR_MESSAGE : CaErrorTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaErrorType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR(${type.errorMessage})")
        }
    }
}
