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
 */
fun interface CaAnnotationArgumentsRenderer {
    fun renderAnnotationArguments(
        analysisSession: CaSession,
        annotation: CaAnnotation,
        owner: CaAnnotated,
        annotationRenderer: CaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val NONE: CaAnnotationArgumentsRenderer = CaAnnotationArgumentsRenderer { _, _, _, _, _ -> }

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
