package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextDependentRenderer
import org.cangnova.cangjie.cfir.types.ConeCangjieType

object CfirDiagnosticRenderers {

    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangjieType, _ ->
        type.toString()
    }
}