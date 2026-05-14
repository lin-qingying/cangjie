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
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableReturnTypeFilter`。
 */
fun interface CaCallableReturnTypeFilter {
    /** 返回 true 表示当前 callable 的返回类型应当写入输出。 */
    fun shouldRenderReturnType(analysisSession: CaSession, type: CaType, symbol: CaCallableSymbol): Boolean

    companion object {
        /** 预设: 永远输出返回类型, 即便是 `Unit`。 */
        val ALWAYS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { _,_, _ -> true }

        /**
         * 预设: 对函数省略 `Unit` 返回类型, 贴近仓颉源码常用风格。
         */
        val NO_UNIT_FOR_FUNCTIONS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { analysisSession,type, symbol ->
            val returnType = with(analysisSession) { symbol.returnType }
            return@CaCallableReturnTypeFilter when (returnType) {
                is CaPrimitiveType -> returnType.kind != PrimitiveTypeKind.UNIT
                else -> true
            }
        }
    }
}
