package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaClassTypeQualifierRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaErrorTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaFunctionalTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaIntersectionTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaPrimitiveTypeRenderer
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
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
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
 *
 * - 顶层入口为 [renderType], 按 [expandedTypeRenderingMode] 决定 typealias 展开策略;
 * - 内部通过 `when` 分发到具体 kind 的 sub-renderer(primitive/class/function/tuple/...);
 * - 通过 [Builder] / [with] 在 preset 基础上派生新配置。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeRenderer`。
 */
class CaTypeRenderer private constructor(
    val expandedTypeRenderingMode: CaExpandedTypeRenderingMode,
    val classIdRenderer: CaClassTypeQualifierRenderer,
    val typeNameRenderer: CaTypeNameRenderer,
    val typeApproximator: CaRendererTypeApproximator,
    val typeProjectionRenderer: CaTypeProjectionRenderer,
    val primitiveTypeRenderer: CaPrimitiveTypeRenderer,
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
    /** 按"缩写形态"渲染类型, 若有 abbreviation 则使用 abbreviation。 */
    private fun CaSession.renderAbbreviatedType(type: CaType, printer: PrettyPrinter) {
        renderTypeAsIs(type.abbreviation ?: type, printer)
    }

    /**
     * 不考虑 abbreviation / 展开, 按类型 kind 直接分发到 sub-renderer。
     */
    private fun CaSession.renderTypeAsIs(type: CaType, printer: PrettyPrinter) {
        when (type) {
            is CaPrimitiveType -> primitiveTypeRenderer.renderType(this, type, this@CaTypeRenderer, printer)
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

    /** 输出 `/* = ExpandedType */` 形式的展开注释。 */
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

    /** 按完全展开形态渲染类型(典型场景: 把 typealias 全部展开成具体类型)。 */
    private fun CaSession.renderExpandedType(type: CaType, printer: PrettyPrinter) {
        renderTypeAsIs(type.fullyExpandedType, printer)
    }

    /**
     * 类型渲染入口, 委托给上述四种模式之一。
     *
     * 调用方一般通过 [CaDeclarationRenderer.typeRenderer] 间接调用。
     */
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

    /** 输出 `/* from: AliasName */` 形式的缩写注释。 */
    private fun CaSession.renderAbbreviatedTypeComment(type: CaType, printer: PrettyPrinter) {
        val abbreviatedType = type.abbreviation
            ?: type.takeIf { it.symbol is CaTypeAliasSymbol }
            ?: return

        printer.append(" /* from: ")
        renderTypeAsIs(abbreviatedType, printer)
        printer.append(" */")
    }

    /** 在当前 renderer 基础上派生新配置, 未覆盖字段沿用原值。 */
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
            primitiveTypeRenderer = current.primitiveTypeRenderer
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

    /**
     * 类型渲染器构建器, 字段必须在 [build] 前赋值。
     */
    class Builder {
        /** typealias 展开模式。 */
        lateinit var expandedTypeRenderingMode: CaExpandedTypeRenderingMode
        /** 类型投影(协变/逆变)渲染策略。 */
        lateinit var typeProjectionRenderer: CaTypeProjectionRenderer
        /** classId 限定名渲染策略(全限定名/短名)。 */
        lateinit var classIdRenderer: CaClassTypeQualifierRenderer
        /** 简单类型名渲染策略。 */
        lateinit var typeNameRenderer: CaTypeNameRenderer
        /** 类型近似化策略。 */
        lateinit var typeApproximator: CaRendererTypeApproximator
        /** 基础类型渲染策略(Int/String/...)。 */
        lateinit var primitiveTypeRenderer: CaPrimitiveTypeRenderer
        /** 普通类类型渲染策略。 */
        lateinit var usualClassTypeRenderer: CaUsualClassTypeRenderer
        /** 函数类型渲染策略。 */
        lateinit var functionalTypeRenderer: CaFunctionalTypeRenderer
        /** 元组类型渲染策略。 */
        lateinit var tupleTypeRenderer: CaTupleTypeRenderer
        /** 交集类型渲染策略。 */
        lateinit var intersectionTypeRenderer: CaIntersectionTypeRenderer
        /** 并集类型渲染策略。 */
        lateinit var unionTypeRenderer: CaUnionTypeRenderer
        /** error 类型渲染策略。 */
        lateinit var errorTypeRenderer: CaErrorTypeRenderer
        /** 关键字渲染器(传递给 sub-renderer)。 */
        lateinit var keywordsRenderer: CaKeywordsRenderer
        /** 类型形参类型渲染策略。 */
        lateinit var typeParameterTypeRenderer: CaTypeParameterTypeRenderer
        /** 类型注解渲染策略。 */
        lateinit var annotationsRenderer: CaAnnotationRenderer
        /** 未解析 class error 类型渲染策略。 */
        lateinit var unresolvedClassErrorTypeRenderer: CaUnresolvedClassErrorTypeRenderer

        /** 构建最终类型渲染器。 */
        fun build(): CaTypeRenderer = CaTypeRenderer(
            expandedTypeRenderingMode,
            classIdRenderer = classIdRenderer,
            typeNameRenderer = typeNameRenderer,
            typeApproximator = typeApproximator,
            typeProjectionRenderer = typeProjectionRenderer,
            primitiveTypeRenderer = primitiveTypeRenderer,
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
        /** DSL 入口, 等价于 `Builder().apply(action).build()`。 */
        operator fun invoke(action: Builder.() -> Unit): CaTypeRenderer =
            Builder().apply(action).build()
    }
}
