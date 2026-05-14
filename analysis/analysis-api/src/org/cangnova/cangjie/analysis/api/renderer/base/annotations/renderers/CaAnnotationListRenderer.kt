package org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer

/**
 * 注解列表渲染器。
 *
 * 该层只负责：
 * - 从 owner 读取注解列表
 * - 组织注解间分隔符
 * - 保证渲染完成后为后续声明/类型输出补上一个尾随空格
 *
 * 它不决定注解名长短，也不解释参数含义。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationListRenderer`。
 */
fun interface CaAnnotationListRenderer {
    /** 写出 [owner] 上的所有注解, 并在末尾补足分隔空格。 */
    fun renderAnnotations(
        analysisSession: CaSession,
        owner: CaAnnotated,
        annotationRenderer: CaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 面向源码风格输出。
         *
         * 多个注解之间用空格分隔, 每条注解以 `@` 开头, 注解列表末尾再补一个空格,
         * 便于后续直接拼接声明/类型。
         */
        val FOR_SOURCE: CaAnnotationListRenderer = CaAnnotationListRenderer { analysisSession, owner, annotationRenderer, printer ->
            val annotations = owner.annotations
            if (annotations.isEmpty()) return@CaAnnotationListRenderer

            printer {
                printCollection(annotations, separator = " ") { annotation ->
                    append("@")
                    annotationRenderer.annotationQualifierRenderer.renderQualifier(
                        analysisSession,
                        annotation,
                        owner,
                        annotationRenderer,
                        this,
                    )
                    annotationRenderer.annotationArgumentsRenderer.renderAnnotationArguments(
                        analysisSession,
                        annotation,
                        owner,
                        annotationRenderer,
                        this,
                    )
                }
                append(" ")
            }
        }
    }
}
