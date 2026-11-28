package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall

/**
 * CFIR 注解渲染器。
 */
open class CfirAnnotationRenderer {

    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前渲染 visitor。
     */
    protected val visitor: CfirRenderer.Visitor get() = components.visitor

    /**
     * 当前输出 printer。
     */
    protected val printer: CfirPrinter get() = components.printer

    /**
     * 调用实参渲染器。
     */
    protected val callArgumentsRenderer: CfirCallArgumentsRenderer? get() = components.callArgumentsRenderer

    /**
     * 渲染注解容器；行内声明需要末尾分隔空格，文件/features 独立行由宿主换行。
     */
    fun render(annotationContainer: CfirAnnotationContainer, trailingSpace: Boolean = true) {
        renderAnnotations(annotationContainer.annotations, trailingSpace)
    }

    /**
     * 渲染注解列表。
     */
    internal fun renderAnnotations(annotations: List<CfirAnnotation>, trailingSpace: Boolean = true) {
        for ((index, annotation) in annotations.withIndex()) {
            renderAnnotation(annotation, trailingSpace = index < annotations.lastIndex || trailingSpace)
        }
    }

    /**
     * 渲染单个注解。
     */
    internal fun renderAnnotation(annotation: CfirAnnotation, trailingSpace: Boolean = true) {
        printer.print("@")
        annotation.typeRef.accept(visitor)
        when (annotation) {
            is CfirAnnotationCall -> callArgumentsRenderer?.renderArguments(annotation.argumentList)
            else -> if (annotation.arguments.isNotEmpty()) {
                callArgumentsRenderer?.renderArgumentElements(annotation.arguments)
            }
        }
        if (trailingSpace) printer.print(" ")
    }


}
