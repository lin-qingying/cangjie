package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextDependentRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

object CfirDiagnosticRenderers {

    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangJieType, _ ->
        type.toString()
    }

    val RENDER_TYPE_LIST = ContextDependentRenderer { types: Collection<ConeCangJieType>, context ->
        types.joinToString(", ") { RENDER_TYPE.render(it, context) }
    }

    val DECLARATION_NAME = Renderer { symbol: CfirBasedSymbol<*> ->
        when (symbol) {
            is CfirCallableSymbol<*> -> symbol.name.asString()
            is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
            is CfirTypeParameterSymbol -> symbol.name.asString()
            else -> return@Renderer "???"
        }
    }

    val DECLARATION_NAME_LIST = ContextDependentRenderer { symbols: Collection<CfirBasedSymbol<*>>, context ->
        symbols.joinToString(", ") { DECLARATION_NAME.render(it, context) }
    }
}
