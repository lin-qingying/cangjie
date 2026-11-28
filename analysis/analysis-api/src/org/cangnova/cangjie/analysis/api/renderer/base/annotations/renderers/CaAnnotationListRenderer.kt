package org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol

/**
 * 注解列表渲染器。
 *
 * 该层只负责：
 * - 从 owner 读取注解列表
 * - 组织注解间分隔符
 *
 * 它不决定注解名长短，也不解释参数含义。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationListRenderer`。
 */
fun interface CaAnnotationListRenderer {
    /** 写出 [owner] 上的所有注解; 注解与 owner 之间的分隔由调用方按 renderer code style 处理。 */
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
         * 多个声明注解之间换行, 参数和类型注解之间用空格; 每条注解以 `@` 开头。
         * 注解列表与 owner 之间的分隔由声明/类型 renderer 按 code style 决定。
         */
        val FOR_SOURCE: CaAnnotationListRenderer = CaAnnotationListRenderer { analysisSession, owner, annotationRenderer, printer ->
            val annotations = owner.annotations
            if (annotations.isEmpty()) return@CaAnnotationListRenderer

            printer {
                printCollection(
                    annotations,
                    separator = when (owner) {
                        is CaValueParameterSymbol -> " "
                        is CaDeclarationSymbol -> "\n"
                        else -> " "
                    },
                ) { annotation ->
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
            }
        }
    }
}
