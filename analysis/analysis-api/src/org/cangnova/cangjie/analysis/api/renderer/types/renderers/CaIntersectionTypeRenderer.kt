package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType

/**
 * [CaIntersectionType] 的渲染策略。
 *
 * 用于在 IDE 提示、签名显示等场景按统一规则输出交集类型 `A & B & C`，
 * 对齐 Kotlin Analysis API 中的 `KaIntersectionTypeRenderer`。
 */
fun interface CaIntersectionTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文。
     * @param type 待渲染的交集类型。
     * @param typeRenderer 父级类型渲染器，用于递归渲染每个 `conjunct`。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaIntersectionType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 用 `" & "` 连接 [CaIntersectionType.conjuncts] 中的每个分量类型。
         */
        val AS_INTERSECTION: CaIntersectionTypeRenderer = CaIntersectionTypeRenderer { analysisSession, type, typeRenderer, printer ->
            printer {
                printCollection(
                    type.conjuncts,
                    separator = " & ",
                ) { conjunct ->
                    typeRenderer.renderType(analysisSession, conjunct, printer)
                }
            }
        }
    }
}
