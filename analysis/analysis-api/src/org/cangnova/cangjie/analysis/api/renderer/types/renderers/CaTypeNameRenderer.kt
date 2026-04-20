package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.render

/**
 * class-like 类型短名渲染协议。
 */
fun interface CaTypeNameRenderer {
    fun renderName(
        analysisSession: CaSession,
        name: Name,
        owner: CaType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    object QUOTED : CaTypeNameRenderer {
        override fun renderName(
            analysisSession: CaSession,
            name: Name,
            owner: CaType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append(name.render())
        }
    }
}
