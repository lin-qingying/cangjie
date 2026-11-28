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
     * 渲染注解容器上的全部注解。
     *
     * @param annotationContainer 注解宿主。
     * @param terminatesLine 该宿主是否让注解独占一行。独占时换行由本渲染器负责，
     *   调用方不得再补 [CfirPrinter.newLine]，否则会多出一个空行。
     */
    fun render(annotationContainer: CfirAnnotationContainer, terminatesLine: Boolean = false) {
        renderAnnotations(annotationContainer.annotations, terminatesLine)
    }

    /**
     * 渲染注解列表。
     *
     * @param annotations 待渲染注解。
     * @param terminatesLine 每个注解是否各自结束一行，见 [render]。
     */
    internal fun renderAnnotations(annotations: List<CfirAnnotation>, terminatesLine: Boolean = false) {
        for (annotation in annotations) {
            renderAnnotation(annotation, terminatesLine)
        }
    }

    /**
     * 渲染单个注解。
     *
     * 注解后默认补一个空格，作为同一行后续文本（修饰符、声明关键字、类型引用）的分隔；
     * 注解独占一行时改发换行，否则那个空格会变成行尾空白。
     *
     * 对齐 Kotlin `FirAnnotationRenderer`（`useSiteTarget == FILE` 时 `println()`，
     * 否则 `print(" ")`）。仓颉没有 `@file:` 注解语法，文件级 metadata 由
     * features directive 这个独立节点承载，注解自身不携带 use-site target，
     * 因此判据只能由调用方按宿主传入。
     *
     * @param annotation 待渲染注解。
     * @param terminatesLine 该注解是否结束一行，见 [render]。
     */
    internal fun renderAnnotation(annotation: CfirAnnotation, terminatesLine: Boolean = false) {
        printer.print("@")
        annotation.typeRef.accept(visitor)
        when (annotation) {
            is CfirAnnotationCall -> callArgumentsRenderer?.renderArguments(annotation.argumentList)
            else -> if (annotation.arguments.isNotEmpty()) {
                callArgumentsRenderer?.renderArgumentElements(annotation.arguments)
            }
        }
        if (terminatesLine) printer.println() else printer.print(" ")
    }


}
