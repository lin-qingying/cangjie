package org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationValueRenderer
import org.cangnova.cangjie.render
import kotlin.text.append

/**
 * 注解参数渲染器。
 *
 * 仓颉当前公开注解模型只暴露 `arguments: List<String>`，
 * 因此这里不发明命名参数对象模型，而是只负责拼装外层括号。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationArgumentsRenderer`。
 */
fun interface CaAnnotationArgumentsRenderer {
    /** 将注解 [annotation] 的参数列表写入 [printer]。 */
    fun renderAnnotationArguments(
        analysisSession: CaSession,
        annotation: CaAnnotation,
        owner: CaAnnotated,
        annotationRenderer: CaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 不渲染任何参数, 仅保留注解名。 */
        val NONE: CaAnnotationArgumentsRenderer = CaAnnotationArgumentsRenderer { _, _, _, _, _ -> }

        /**
         * 预设: 当存在参数时按 `name = value` 形式渲染, 用括号包裹;
         * 无参数则完全省略括号。
         */
        val IF_ANY: CaAnnotationArgumentsRenderer = CaAnnotationArgumentsRenderer { _, annotation, _, _, printer ->
            if (annotation.arguments.isEmpty()) return@CaAnnotationArgumentsRenderer

            printer {
                printCollection(
                    annotation.arguments,
                    prefix = "(",
                    postfix = ")",
                ) { argument ->
                    append(argument.name.render())
                    append(" = ")
                    append(CaAnnotationValueRenderer.render(argument.expression))
                }
            }
        }
    }
}
