package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
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
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.api.types.symbol
import kotlin.takeIf

/**
 * 仓颉公共类型 renderer。
 *
 * 这一层只依赖公开 `CaType` 结构，不依赖 CFIR 内部类型树。
 */
class CaTypeRenderer private constructor(
    val expandedTypeRenderingMode: CaExpandedTypeRenderingMode,
    val classIdRenderer: CaClassTypeQualifierRenderer,
    val typeNameRenderer: CaTypeNameRenderer,
    val typeApproximator: CaRendererTypeApproximator,
    val typeProjectionRenderer: CaTypeProjectionRenderer,
    val usualClassTypeRenderer: CaUsualClassTypeRenderer,
    val functionalTypeRenderer: CaFunctionalTypeRenderer,
    val typeParameterTypeRenderer: CaTypeParameterTypeRenderer,
    val annotationsRenderer: CaAnnotationRenderer,
    val unresolvedClassErrorTypeRenderer: CaUnresolvedClassErrorTypeRenderer,
    val tupleTypeRenderer: CaTupleTypeRenderer,
    val intersectionTypeRenderer: CaIntersectionTypeRenderer,
    val unionTypeRenderer: CaUnionTypeRenderer,
    val errorTypeRenderer: CaErrorTypeRenderer,
    val keywordsRenderer: CaKeywordsRenderer,
) {
    private fun CaSession.renderAbbreviatedType(type: CaType, printer: PrettyPrinter) {
        renderTypeAsIs(type.abbreviation ?: type, printer)
    }

    /**
     * Renders [type] directly without considering its abbreviation or expansion.
     */
    private fun CaSession.renderTypeAsIs(type: CaType, printer: PrettyPrinter) {
        when (type) {
            is CaUsualClassType -> usualClassTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaFunctionType -> functionalTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaTupleType -> tupleTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaIntersectionType -> intersectionTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaUnionType -> unionTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaTypeParameterType -> typeParameterTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaClassErrorType -> unresolvedClassErrorTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
            is CaErrorType -> errorTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
        }
    }

    private fun CaSession.renderExpandedTypeComment(type: CaType, printer: PrettyPrinter) {
        val expandedType = when {
            type.abbreviation != null -> type
            type.symbol is CaTypeAliasSymbol -> type.fullyExpandedType
            else -> return
        }

        printer.append(" /* = ")
        renderTypeAsIs(expandedType, printer)
        printer.append(" */")
    }

    private fun CaSession.renderExpandedType(type: CaType, printer: PrettyPrinter) {
        renderTypeAsIs(type.fullyExpandedType, printer)
    }

    fun renderType(analysisSession: CaSession, type: CaType, printer: PrettyPrinter) {
        with(analysisSession) {
            when (expandedTypeRenderingMode) {
                CaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE -> renderAbbreviatedType(type, printer)
                CaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT -> {
                    renderAbbreviatedType(type, printer)
                    renderExpandedTypeComment(type, printer)
                }

                CaExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE -> renderExpandedType(type, printer)
                CaExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT -> {
                    renderExpandedType(type, printer)
                    renderAbbreviatedTypeComment(type, printer)
                }
            }
        }
    }

    private fun CaSession.renderAbbreviatedTypeComment(type: CaType, printer: PrettyPrinter) {
        val abbreviatedType = type.abbreviation
            ?: type.takeIf { it.symbol is CaTypeAliasSymbol }
            ?: return

        printer.append(" /* from: ")
        renderTypeAsIs(abbreviatedType, printer)
        printer.append(" */")
    }

    fun with(action: Builder.() -> Unit): CaTypeRenderer {
        val current = this
        return Builder().apply {
            this.expandedTypeRenderingMode = current.expandedTypeRenderingMode
            this.unresolvedClassErrorTypeRenderer = current.unresolvedClassErrorTypeRenderer
            this.typeParameterTypeRenderer = current.typeParameterTypeRenderer
            this.annotationsRenderer = current.annotationsRenderer
            this.typeProjectionRenderer = current.typeProjectionRenderer
            classIdRenderer = current.classIdRenderer
            typeNameRenderer = current.typeNameRenderer
            typeApproximator = current.typeApproximator
            usualClassTypeRenderer = current.usualClassTypeRenderer
            functionalTypeRenderer = current.functionalTypeRenderer
            tupleTypeRenderer = current.tupleTypeRenderer
            intersectionTypeRenderer = current.intersectionTypeRenderer
            unionTypeRenderer = current.unionTypeRenderer
            errorTypeRenderer = current.errorTypeRenderer
            keywordsRenderer = current.keywordsRenderer
            action()
        }.build()
    }

    class Builder {
        lateinit var expandedTypeRenderingMode: CaExpandedTypeRenderingMode
        lateinit var typeProjectionRenderer: CaTypeProjectionRenderer
        lateinit var classIdRenderer: CaClassTypeQualifierRenderer
        lateinit var typeNameRenderer: CaTypeNameRenderer
        lateinit var typeApproximator: CaRendererTypeApproximator
        lateinit var usualClassTypeRenderer: CaUsualClassTypeRenderer
        lateinit var functionalTypeRenderer: CaFunctionalTypeRenderer
        lateinit var tupleTypeRenderer: CaTupleTypeRenderer
        lateinit var intersectionTypeRenderer: CaIntersectionTypeRenderer
        lateinit var unionTypeRenderer: CaUnionTypeRenderer
        lateinit var errorTypeRenderer: CaErrorTypeRenderer
        lateinit var keywordsRenderer: CaKeywordsRenderer
        lateinit var typeParameterTypeRenderer: CaTypeParameterTypeRenderer
        lateinit var annotationsRenderer: CaAnnotationRenderer
        lateinit var unresolvedClassErrorTypeRenderer: CaUnresolvedClassErrorTypeRenderer

        fun build(): CaTypeRenderer = CaTypeRenderer(
            expandedTypeRenderingMode,
            classIdRenderer = classIdRenderer,
            typeNameRenderer = typeNameRenderer,
            typeApproximator = typeApproximator,
            typeProjectionRenderer = typeProjectionRenderer,
            usualClassTypeRenderer = usualClassTypeRenderer,
            functionalTypeRenderer = functionalTypeRenderer,
            typeParameterTypeRenderer = typeParameterTypeRenderer,
            annotationsRenderer = annotationsRenderer,
            unresolvedClassErrorTypeRenderer = unresolvedClassErrorTypeRenderer,
            tupleTypeRenderer = tupleTypeRenderer,
            intersectionTypeRenderer = intersectionTypeRenderer,
            unionTypeRenderer = unionTypeRenderer,
            errorTypeRenderer = errorTypeRenderer,
            keywordsRenderer = keywordsRenderer,
        )
    }

    companion object {
        operator fun invoke(action: Builder.() -> Unit): CaTypeRenderer =
            Builder().apply(action).build()
    }
}
