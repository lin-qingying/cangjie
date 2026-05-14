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
     *
     * 实际写出顺序与排版交由 [annotationListRenderer] 决定。
     */
    fun renderAnnotations(analysisSession: CaSession, owner: CaAnnotated, printer: PrettyPrinter) {
        annotationListRenderer.renderAnnotations(analysisSession, owner, this, printer)
    }

    /**
     * 基于当前 renderer 派生一个新配置。
     *
     * 未在 [action] 中显式覆盖的字段沿用当前实例的设置。
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
     * 注解 renderer 构建器, 用于以 DSL 方式装配 [CaAnnotationRenderer]。
     */
    class Builder {
        /** 注解列表组织策略(顺序、分隔、换行)。 */
        lateinit var annotationListRenderer: CaAnnotationListRenderer

        /** 注解名(短名/全限定名)渲染策略。 */
        lateinit var annotationQualifierRenderer: CaAnnotationQualifierRenderer

        /** 注解参数渲染策略。 */
        lateinit var annotationArgumentsRenderer: CaAnnotationArgumentsRenderer

        /** 构建最终的注解渲染器。 */
        fun build(): CaAnnotationRenderer = CaAnnotationRenderer(
            annotationListRenderer = annotationListRenderer,
            annotationQualifierRenderer = annotationQualifierRenderer,
            annotationArgumentsRenderer = annotationArgumentsRenderer,
        )
    }

    companion object {
        /** DSL 入口, 等价于 `Builder().apply(action).build()`。 */
        operator fun invoke(action: Builder.() -> Unit): CaAnnotationRenderer =
            Builder().apply(action).build()
    }
}
