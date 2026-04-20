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
 */
fun interface CaAnnotationListRenderer {
    fun renderAnnotations(
        analysisSession: CaSession,
        owner: CaAnnotated,
        annotationRenderer: CaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
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
