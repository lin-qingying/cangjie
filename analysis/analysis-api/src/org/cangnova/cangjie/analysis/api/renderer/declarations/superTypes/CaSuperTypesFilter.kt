package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 超类型过滤策略。
 *
 * 决定列表中的某个超类型是否参与渲染, 例如可用于隐藏隐式 `Object`。
 *
 * 对齐 Kotlin Analysis API 的 `KaSuperTypesFilter`。
 */
fun interface CaSuperTypesFilter {
    /** 返回 true 表示该超类型应当渲染。 */
    fun shouldRenderSuperType(
        analysisSession: CaSession,
        owner: CaClassSymbol,
        superType: CaType,
    ): Boolean

    companion object {
        /** 预设: 放行全部超类型。 */
        val ALL: CaSuperTypesFilter = CaSuperTypesFilter { _, _, _ -> true }

        /** 预设: 隐藏全部超类型, 输出"裸"声明头。 */
        val NONE: CaSuperTypesFilter = CaSuperTypesFilter { _, _, _ -> false }
    }
}
