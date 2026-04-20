package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint

object CaSuperTypeListRendererForSource {
    val AS_LIST: CaSuperTypeListRenderer = CaSuperTypeListRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val superTypes = symbol.superTypes.filter { superType ->
            declarationRenderer.superTypesFilter.shouldRenderSuperType(analysisSession, symbol, superType)
        }
        if (superTypes.isEmpty()) return@CaSuperTypeListRenderer
        printer.append(" <: ")
        printer.append(
            superTypes.joinToString(" & ") { superType ->
                prettyPrint {
                    declarationRenderer.superTypeRenderer.renderSuperType(
                        analysisSession,
                        superType,
                        declarationRenderer,
                        this,
                    )
                }
            },
        )
    }
}
