package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaTupleType

/**
 * [CaTupleType] 的渲染策略。
 *
 * 负责把元组类型按 `(T1, T2, T3)` 的形式输出到 [PrettyPrinter]，
 * 对齐 Kotlin Analysis API 中 tuple/记录类的渲染思路。
 */
fun interface CaTupleTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文。
     * @param type 待渲染的元组类型。
     * @param typeRenderer 父级类型渲染器，用于递归渲染每个元素类型。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaTupleType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 按源码可见形式渲染，使用括号包裹元素类型，元素之间以默认分隔符隔开。
         */
        val AS_SOURCE: CaTupleTypeRenderer = CaTupleTypeRenderer { analysisSession, type, typeRenderer, printer ->
            printer {
                printCollection(
                    type.elementTypes,
                    prefix = "(",
                    postfix = ")",
                ) { elementType ->
                    typeRenderer.renderType(analysisSession, elementType, this)
                }
            }
        }
    }
}
