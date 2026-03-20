package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextDependentRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

object CfirDiagnosticRenderers {

    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangjieType, _ ->
        type.toString()
    }

    val DECLARATION_NAME = Renderer { symbol: CfirSymbol<*> ->
        when (symbol) {
            is CfirCallableSymbol<*> -> symbol.name.asString()
            is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
            is CfirTypeParameterSymbol -> symbol.name.asString()
            else -> return@Renderer "???"
        }
    }
}
