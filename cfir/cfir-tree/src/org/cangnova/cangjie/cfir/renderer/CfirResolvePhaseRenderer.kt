package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall

/**
 * resolve phase 调试渲染器。
 */
class CfirResolvePhaseRenderer {
    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前输出 printer。
     */
    private val printer get() = components.printer

    /**
     * 渲染元素当前 resolve state。
     */
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
