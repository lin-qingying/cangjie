package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId

/**
 * callable 返回类型过滤协议。
 *
 * 公共 renderer 不应该把“是否省略某些返回类型”硬编码在具体声明渲染器里，
 * 而应通过稳定策略位集中描述。
 */
fun interface CaCallableReturnTypeFilter {
    fun shouldRenderReturnType(analysisSession: CaSession, symbol: CaCallableSymbol): Boolean

    companion object {
        private val UNIT_CLASS_ID: ClassId = ClassId(StandardNames.STD_CORE_PACKAGE_FQ_NAME, StandardNames.UNIT)

        val ALWAYS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { _, _ -> true }

        val NO_UNIT_FOR_FUNCTIONS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { analysisSession, symbol ->
            val returnType = with(analysisSession) { symbol.returnType }
            return@CaCallableReturnTypeFilter when (returnType) {
                is CaClassLikeType -> returnType.classId != UNIT_CLASS_ID
                else -> true
            }
        }
    }
}
