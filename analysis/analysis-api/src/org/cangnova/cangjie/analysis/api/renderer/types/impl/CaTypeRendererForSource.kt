package org.cangnova.cangjie.analysis.api.renderer.types.impl

import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.CaExpandedTypeRenderingMode
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaClassTypeQualifierRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaErrorTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaFunctionalTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaIntersectionTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaTupleTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaTypeNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaTypeParameterTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaTypeProjectionRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaUnionTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaUnresolvedClassErrorTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaUsualClassTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.impl.CaFunctionalTypeRendererForSource

/**
 * Source 代码风格下的公共类型 renderer 预设。
 *
 * 当前公开 `CaFunctionType` 模型只暴露参数类型序列，不暴露参数名，
 * 因此这里不再提供伪装成“去掉参数名”的 preset。
 * 只有 analysis-api 真正能够表达的语义差异，才允许进入公开 preset 集合。
 */
object CaTypeRendererForSource {
    val WITH_QUALIFIED_NAMES: CaTypeRenderer = CaTypeRenderer {
        expandedTypeRenderingMode = CaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
        typeNameRenderer = CaTypeNameRenderer.QUOTED
        typeApproximator = CaRendererTypeApproximator.NO_APPROXIMATION
        typeProjectionRenderer = CaTypeProjectionRenderer.WITH_TYPE_ARGUMENTS
        usualClassTypeRenderer = CaUsualClassTypeRenderer.AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS
        functionalTypeRenderer = CaFunctionalTypeRenderer.AS_SOURCE
        typeParameterTypeRenderer = CaTypeParameterTypeRenderer.AS_SOURCE
        annotationsRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        unresolvedClassErrorTypeRenderer = CaUnresolvedClassErrorTypeRenderer.UNRESOLVED_QUALIFIER
        tupleTypeRenderer = CaTupleTypeRenderer.AS_SOURCE
        intersectionTypeRenderer = CaIntersectionTypeRenderer.AS_INTERSECTION
        unionTypeRenderer = CaUnionTypeRenderer.AS_UNION
        errorTypeRenderer = CaErrorTypeRenderer.AS_CODE_IF_POSSIBLE
        keywordsRenderer = CaKeywordsRenderer.AS_WORD
    }

    val WITH_SHORT_NAMES: CaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
        annotationsRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
    }

    val WITH_QUALIFIED_NAMES_WITHOUT_TYPE_ARGUMENTS: CaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        usualClassTypeRenderer = CaUsualClassTypeRenderer.AS_CLASS_TYPE_WITHOUT_TYPE_ARGUMENTS
        typeProjectionRenderer = CaTypeProjectionRenderer.NONE
    }

    val WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS: CaTypeRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_TYPE_ARGUMENTS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
            annotationsRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
        }

    val WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        functionalTypeRenderer = CaFunctionalTypeRendererForSource.WITHOUT_KIND_KEYWORDS
    }

    val WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
            annotationsRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
        }
}
