package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaUnionType

/**
 * [CaUnionType] 的渲染策略。
 *
 * 用于把仓颉联合类型按 `A | B | C` 的形式展示到 [PrettyPrinter]，
 * 对齐 Kotlin Analysis API 中对类似复合类型的渲染思路。
 */
fun interface CaUnionTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文。
     * @param type 待渲染的联合类型。
     * @param typeRenderer 父级类型渲染器，用于递归渲染每个候选类型。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaUnionType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 用 `" | "` 连接 [CaUnionType.alternatives] 中的每个候选类型。
         */
        val AS_UNION: CaUnionTypeRenderer = CaUnionTypeRenderer { analysisSession, type, typeRenderer, printer ->
            printer {
                printCollection(
                    type.alternatives,
                    separator = " | ",
                ) { alternative ->
                    typeRenderer.renderType(analysisSession, alternative, this)
                }
            }
        }
    }
}
