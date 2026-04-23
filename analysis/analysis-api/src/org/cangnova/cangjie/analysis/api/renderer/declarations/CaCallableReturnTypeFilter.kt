package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * callable 返回类型过滤协议。
 *
 * 公共 renderer 不应该把“是否省略某些返回类型”硬编码在具体声明渲染器里，
 * 而应通过稳定策略位集中描述。
 */
fun interface CaCallableReturnTypeFilter {
    public fun shouldRenderReturnType(analysisSession: CaSession, type: CaType, symbol: CaCallableSymbol): Boolean

    companion object {
        val ALWAYS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { _,_, _ -> true }

        val NO_UNIT_FOR_FUNCTIONS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { analysisSession,type, symbol ->
            val returnType = with(analysisSession) { symbol.returnType }
            return@CaCallableReturnTypeFilter when (returnType) {
                is CaPrimitiveType -> returnType.kind != PrimitiveTypeKind.UNIT
                else -> true
            }
        }
    }
}
