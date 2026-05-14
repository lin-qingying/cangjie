package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

/**
 * 类型形参过滤策略。
 *
 * 决定 owner 的某个类型形参是否参与渲染, 用于 IDE 隐藏内部生成的合成形参等。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererTypeParametersFilter`。
 */
fun interface CaTypeParametersFilter {
    /** 返回 true 表示当前类型形参应当渲染。 */
    fun shouldRenderTypeParameter(
        analysisSession: CaSession,
        owner: CaTypeParameterOwnerSymbol,
        typeParameter: CaTypeParameterSymbol,
    ): Boolean

    companion object {
        /** 预设: 放行全部类型形参。 */
        val ALL: CaTypeParametersFilter = CaTypeParametersFilter { _, _, _ -> true }

        /** 预设: 隐藏全部类型形参, 适合简化签名展示。 */
        val NONE: CaTypeParametersFilter = CaTypeParametersFilter { _, _, _ -> false }
    }
}
