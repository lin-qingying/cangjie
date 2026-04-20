package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall

class CfirResolvePhaseRenderer {
    internal lateinit var components: CfirRendererComponents
    private val printer get() = components.printer

    fun render(element: CfirElementWithResolveState) {
        @OptIn(ResolveStateAccess::class)
        val text = when (element) {
            else -> "[${element.resolveState}] "
        }

        printer.print(text)
    }

//    fun render(element: CfirAnnotationCall) {
//        printer.print("[${element.annotationResolvePhase}]")
//    }
}
