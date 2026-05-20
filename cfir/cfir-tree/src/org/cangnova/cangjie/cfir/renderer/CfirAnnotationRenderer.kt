package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall

open class CfirAnnotationRenderer {

    internal lateinit var components: CfirRendererComponents
    protected val visitor: CfirRenderer.Visitor get() = components.visitor
    protected val printer: CfirPrinter get() = components.printer
    protected val callArgumentsRenderer: CfirCallArgumentsRenderer? get() = components.callArgumentsRenderer

    fun render(annotationContainer: CfirAnnotationContainer) {
        renderAnnotations(annotationContainer.annotations)
    }

    internal fun renderAnnotations(annotations: List<CfirAnnotation>) {
        for (annotation in annotations) {
            renderAnnotation(annotation)
        }
    }

    internal fun renderAnnotation(annotation: CfirAnnotation) {
        printer.print("@")
        annotation.typeRef.accept(visitor)
        when (annotation) {
            is CfirAnnotationCall -> callArgumentsRenderer?.renderArguments(annotation.argumentList)
            else -> if (annotation.arguments.isNotEmpty()) {
                callArgumentsRenderer?.renderArgumentElements(annotation.arguments)
            }
        }
        printer.print(" ")
    }


}
