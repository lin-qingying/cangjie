package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextDependentRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * CFIR 诊断参数渲染器集合。
 */
object CfirDiagnosticRenderers {

    /**
     * 渲染单个 cone 类型。
     */
    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangJieType, _ ->
        type.toString()
    }

    /**
     * 渲染 cone 类型集合。
     */
    val RENDER_TYPE_LIST = ContextDependentRenderer { types: Collection<ConeCangJieType>, context ->
        types.joinToString(", ") { RENDER_TYPE.render(it, context) }
    }

    /**
     * 渲染 CFIR 符号的声明名。
     */
    val DECLARATION_NAME = Renderer { symbol: CfirBasedSymbol<*> ->
        when (symbol) {
            is CfirCallableSymbol<*> -> symbol.name.asString()
            is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
            is CfirTypeParameterSymbol -> symbol.name.asString()
            else -> return@Renderer "???"
        }
    }

    /**
     * 渲染 CFIR 符号声明名集合。
     */
    val DECLARATION_NAME_LIST = ContextDependentRenderer { symbols: Collection<CfirBasedSymbol<*>>, context ->
        symbols.joinToString(", ") { DECLARATION_NAME.render(it, context) }
    }
}
