package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType

/**
 * [CaTypeParameterType] 的渲染策略。
 *
 * 对齐 Kotlin `KaTypeParameterTypeRenderer`，负责把泛型类型参数（如 `T`、`out T` 等）
 * 在签名、提示文本中渲染为合适的形式。
 */
interface CaTypeParameterTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与名字渲染依赖。
     * @param type 待渲染的类型参数类型。
     * @param typeRenderer 父级类型渲染器，用于复用注解、名字等子渲染器。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaTypeParameterType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    /**
     * 按源码风格渲染：依次输出注解与类型参数名。
     */
    object AS_SOURCE : CaTypeParameterTypeRenderer {
        /**
         * 按源码风格输出类型参数类型。
         */
        override fun renderType(
            analysisSession: CaSession,
            type: CaTypeParameterType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                    {
                        typeRenderer.typeNameRenderer.renderName(analysisSession, type.name, type, typeRenderer, printer)
                        with(analysisSession) {
                        }
                    },
                )
            }
        }
    }
}
