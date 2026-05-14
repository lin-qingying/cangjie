package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType

/**
 * [CaPrimitiveType] 的渲染策略。
 *
 * 对齐 Kotlin Analysis API 中的 `KaBuiltinTypeRenderer`，用于把仓颉内置基础类型
 * （如 `Int64`、`Bool`、`String` 等）按统一风格输出。
 */
fun interface CaPrimitiveTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与注解读取。
     * @param type 待渲染的基础类型。
     * @param typeRenderer 父级类型渲染器，用于复用注解渲染器等子组件。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaPrimitiveType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 按源码可见形式渲染：先输出注解，再输出类型名（[CaPrimitiveType.kind] 对应的 `typeName`）。
         */
        val AS_SOURCE: CaPrimitiveTypeRenderer =
            CaPrimitiveTypeRenderer { analysisSession, type, typeRenderer, printer ->
                printer {
                    " ".separated(
                        { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this) },
                        {
                            append(type.kind.typeName)
                        },
                    )
                }
            }
    }
}
