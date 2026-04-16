package org.cangnova.cangjie.analysis.api.renderer.types.impl

import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaClassTypeQualifierRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaErrorTypeRenderer

object CaTypeRendererForDebug {
    val WITH_QUALIFIED_NAMES: CaTypeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
        errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
    }

    val WITH_SHORT_NAMES: CaTypeRenderer = CaTypeRendererForSource.WITH_SHORT_NAMES.with {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
        errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
    }



    val WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS: CaTypeRenderer =
        CaTypeRendererForSource.WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
            errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
        }

    val WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer =
        CaTypeRendererForSource.WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
            errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
        }

    val WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer =
        CaTypeRendererForSource.WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
            errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
        }

}
