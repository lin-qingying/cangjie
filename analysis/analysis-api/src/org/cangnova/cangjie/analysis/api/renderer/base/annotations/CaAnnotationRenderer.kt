package org.cangnova.cangjie.analysis.api.renderer.base.annotations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers.CaAnnotationArgumentsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers.CaAnnotationListRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers.CaAnnotationQualifierRenderer

/**
 * 仓颉 Analysis API 的公开注解渲染入口。
 *
 * 这里按 Kotlin Analysis API 的组合式 renderer 架构组织，
 * 但只保留仓颉当前公开模型真正具备语义依据的层：
 * - 注解列表组织
 * - 注解名渲染
 * - 注解参数渲染
 *
 * 仓颉当前公开注解模型里，renderer 真正依赖的稳定语义字段只有：
 * - `shortName`
 * - `classId`
 * - `arguments`
 *
 * 因此本层不引入 Kotlin 才有的 use-site target/filter 等语义。
 */
class CaAnnotationRenderer internal constructor(
    val annotationListRenderer: CaAnnotationListRenderer,
    val annotationQualifierRenderer: CaAnnotationQualifierRenderer,
    val annotationArgumentsRenderer: CaAnnotationArgumentsRenderer,
) {
    /**
     * 渲染附着在 [owner] 上的全部注解。
     */
    fun renderAnnotations(analysisSession: CaSession, owner: CaAnnotated, printer: PrettyPrinter) {
        annotationListRenderer.renderAnnotations(analysisSession, owner, this, printer)
    }

    /**
     * 基于当前 renderer 派生一个新配置。
     */
    fun with(action: Builder.() -> Unit): CaAnnotationRenderer {
        val current = this
        return Builder().apply {
            annotationListRenderer = current.annotationListRenderer
            annotationQualifierRenderer = current.annotationQualifierRenderer
            annotationArgumentsRenderer = current.annotationArgumentsRenderer
            action()
        }.build()
    }

    /**
     * 注解 renderer 构建器。
     */
    class Builder {
        lateinit var annotationListRenderer: CaAnnotationListRenderer
        lateinit var annotationQualifierRenderer: CaAnnotationQualifierRenderer
        lateinit var annotationArgumentsRenderer: CaAnnotationArgumentsRenderer

        fun build(): CaAnnotationRenderer = CaAnnotationRenderer(
            annotationListRenderer = annotationListRenderer,
            annotationQualifierRenderer = annotationQualifierRenderer,
            annotationArgumentsRenderer = annotationArgumentsRenderer,
        )
    }

    companion object {
        operator fun invoke(action: Builder.() -> Unit): CaAnnotationRenderer =
            Builder().apply(action).build()
    }
}
