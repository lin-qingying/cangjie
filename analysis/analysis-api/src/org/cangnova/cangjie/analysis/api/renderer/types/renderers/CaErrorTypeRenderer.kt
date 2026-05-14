package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaErrorType

/**
 * [CaErrorType] 的渲染策略。
 *
 * 与 Kotlin Analysis API 中的 `KaErrorTypeRenderer` 对齐：用于在 IDE 提示、调试输出等场景下
 * 选择如何展示一个无法解析或解析失败的类型。
 */
interface CaErrorTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer] 中。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文信息。
     * @param type 待渲染的错误类型。
     * @param typeRenderer 父级类型渲染器，可用于复用注解、限定符等子渲染器。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaErrorType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    /**
     * 优先输出错误类型保留的源码片段；若没有源码信息则退化为 `ERROR` 字面量。
     */
    object AS_CODE_IF_POSSIBLE : CaErrorTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaErrorType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.presentableText?.let {
                printer.append(it)
                return
            }
            printer.append("ERROR")
        }
    }

    /**
     * 统一输出 `ERROR` 字面量，不暴露任何内部错误细节。
     */
    object AS_ERROR_WORD : CaErrorTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaErrorType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR")
        }
    }

    /**
     * 输出 `ERROR(<errorMessage>)`，便于调试或日志展示具体的错误原因。
     */
    object WITH_ERROR_MESSAGE : CaErrorTypeRenderer {
        override fun renderType(
            analysisSession: CaSession,
            type: CaErrorType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("ERROR(${type.errorMessage})")
        }
    }
}
