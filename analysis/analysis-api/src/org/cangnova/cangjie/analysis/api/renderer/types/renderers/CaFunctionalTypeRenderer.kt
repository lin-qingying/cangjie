package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.impl.CaFunctionalTypeRendererForSource
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/**
 * [CaFunctionType] 的渲染策略。
 *
 * 与 Kotlin `KaFunctionalTypeRenderer` 对齐，负责把仓颉函数类型（包含可能的 `func`/lambda 关键字、
 * 参数列表、返回类型与注解）按目标输出风格写到 [PrettyPrinter]。
 */
fun interface CaFunctionalTypeRenderer {
    /**
     * 将 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文。
     * @param type 待渲染的函数类型。
     * @param typeRenderer 父级类型渲染器，用于复用参数类型、注解等子渲染器。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaFunctionType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 按源码可见形式渲染（带 lambda/func 等关键字），用于贴近用户书写的展示场景。
         */
        val AS_SOURCE: CaFunctionalTypeRenderer = CaFunctionalTypeRendererForSource.WITH_KIND_KEYWORDS
    }
}
