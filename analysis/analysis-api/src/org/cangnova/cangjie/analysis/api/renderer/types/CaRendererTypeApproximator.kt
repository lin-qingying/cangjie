package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.types.CaType

fun interface CaRendererTypeApproximator {
    fun approximateType(analysisSession: CaSession, type: CaType): CaType

    object NO_APPROXIMATION : CaRendererTypeApproximator {
        override fun approximateType(analysisSession: CaSession, type: CaType): CaType {
            return type
        }
    }
}
