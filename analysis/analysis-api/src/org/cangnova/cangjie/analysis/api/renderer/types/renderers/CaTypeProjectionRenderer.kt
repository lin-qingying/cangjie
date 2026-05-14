package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection

/**
 * [CaTypeProjection] 的渲染策略。
 *
 * 用于在渲染泛型类型实参时控制具体投影（普通类型实参、上下界变体等）的展示，
 * 与 Kotlin `KaTypeProjectionRenderer` 对齐。
 */
fun interface CaTypeProjectionRenderer {
    /**
     * 把 [projection] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文。
     * @param projection 待渲染的类型投影。
     * @param typeRenderer 父级类型渲染器，用于渲染投影内部的实际类型。
     * @param printer 输出目标。
     */
    fun renderTypeProjection(
        analysisSession: CaSession,
        projection: CaTypeProjection,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 输出投影对应的实参类型；如果投影没有具体类型（例如 `*`/`_`）则不输出任何内容。
         */
        val WITH_TYPE_ARGUMENTS: CaTypeProjectionRenderer = CaTypeProjectionRenderer { analysisSession, projection, typeRenderer, printer ->
            projection.type?.let { type ->
                typeRenderer.renderType(analysisSession, type, printer)
            }
        }

        /**
         * 始终不输出投影内容，常用于上层希望自行处理实参列表的场景。
         */
        val NONE: CaTypeProjectionRenderer = CaTypeProjectionRenderer { _, _, _, _ -> }
    }
}
