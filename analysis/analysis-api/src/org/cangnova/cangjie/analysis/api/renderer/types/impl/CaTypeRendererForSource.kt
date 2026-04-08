package org.cangnova.cangjie.analysis.api.renderer.types.impl

import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaClassTypeQualifierRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaErrorTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaFunctionalTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaFunctionalTypeRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.CaIntersectionTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeArgumentRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeArgumentsRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaTupleTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaUnionTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaUsualClassTypeRenderer

object CaTypeRendererForSource {
    val WITH_QUALIFIED_NAMES: CaTypeRenderer = CaTypeRenderer {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
        typeNameRenderer = CaTypeNameRenderer.QUOTED
        typeApproximator = CaRendererTypeApproximator.TO_DENOTABLE
        typeArgumentRenderer = CaTypeArgumentRenderer.AS_SOURCE
        typeArgumentsRenderer = CaTypeArgumentsRenderer.AS_SOURCE
        usualClassTypeRenderer = CaUsualClassTypeRenderer.AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS
        functionalTypeRenderer = CaFunctionalTypeRenderer.AS_SOURCE
        tupleTypeRenderer = CaTupleTypeRenderer.AS_SOURCE
        intersectionTypeRenderer = CaIntersectionTypeRenderer.AS_INTERSECTION
        unionTypeRenderer = CaUnionTypeRenderer.AS_UNION
        errorTypeRenderer = CaErrorTypeRenderer.AS_PRESENTATION
        keywordsRenderer = CaKeywordsRenderer.AS_WORD
    }

    val WITH_SHORT_NAMES: CaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
    }

    val WITH_QUALIFIED_NAMES_WITHOUT_TYPE_ARGUMENTS: CaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        typeArgumentsRenderer = CaTypeArgumentsRenderer.NO_TYPE_ARGUMENTS
    }

    val WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS: CaTypeRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_TYPE_ARGUMENTS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
        }

    val WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        functionalTypeRenderer = CaFunctionalTypeRendererForSource.WITHOUT_KIND_KEYWORDS
    }

    val WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
        }

    /**
     * 当前公开 `CaFunctionType` 尚未暴露参数名，因此这个 preset 与 `WITH_QUALIFIED_NAMES` 行为一致，
     * 先把命名粒度对齐到 Kotlin preset 面，后续若公开参数名可直接收紧实现。
     */
    val WITH_QUALIFIED_NAMES_WITHOUT_PARAMETER_NAMES: CaTypeRenderer = WITH_QUALIFIED_NAMES

    val WITH_SHORT_NAMES_WITHOUT_PARAMETER_NAMES: CaTypeRenderer = WITH_SHORT_NAMES
}
