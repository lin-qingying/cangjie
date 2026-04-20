package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation

open class CfirAnnotationRenderer {

    internal lateinit var components: CfirRendererComponents
    protected val visitor: CfirRenderer.Visitor get() = components.visitor
    protected val printer: CfirPrinter get() = components.printer

    fun render(annotationContainer: CfirAnnotationContainer) {
    }

    internal fun renderAnnotations(annotations: List<CfirAnnotation>) {

    }

    internal fun renderAnnotation(annotation: CfirAnnotation) {

    }


}

