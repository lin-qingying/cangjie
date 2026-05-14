package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType

/**
 * [CaClassErrorType] 类未解析错误类型的渲染策略。
 *
 * 与 Kotlin Analysis API 类似，把"明确知道它是个 class 引用，但未能解析到目标声明"的错误类型
 * 通过限定符（qualifier）链按可读形式输出，便于在 IDE 上展示未解析的源码 token。
 */
interface CaUnresolvedClassErrorTypeRenderer {
    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与上下文。
     * @param type 待渲染的错误类型。
     * @param typeRenderer 父级类型渲染器，可用于复用注解、错误占位、类型实参等子渲染器。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaClassErrorType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 以未解析限定名形式渲染 class-like 错误类型。
         */
        val UNRESOLVED_QUALIFIER: CaUnresolvedClassErrorTypeRenderer = object : CaUnresolvedClassErrorTypeRenderer {
            override fun renderType(
                analysisSession: CaSession,
                type: CaClassErrorType,
                typeRenderer: CaTypeRenderer,
                printer: PrettyPrinter,
            ) {
                val qualifiers = type.qualifiers
                if (qualifiers.isEmpty()) {
                    typeRenderer.errorTypeRenderer.renderType(analysisSession, type, typeRenderer, printer)
                    return
                }

                printer.printCollection(qualifiers, separator = ".") { qualifier ->
                    append(qualifier.name.asString())
                    printCollectionIfNotEmpty(
                        qualifier.typeArguments,
                        prefix = "<",
                        postfix = ">",
                    ) { argument ->
                        typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, argument, typeRenderer, this)
                    }
                }
            }
        }
    }
}
