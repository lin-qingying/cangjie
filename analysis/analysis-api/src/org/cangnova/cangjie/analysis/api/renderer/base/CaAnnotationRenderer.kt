package org.cangnova.cangjie.analysis.api.renderer.base

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation

/**
 * 注解渲染器。
 *
 * 当前公共注解模型已经稳定暴露：
 * - shortName
 * - classId
 * - arguments
 * - renderedText
 *
 * 因此 renderer 在公共层只基于这些稳定字段工作。
 */
fun interface CaAnnotationRenderer {
    fun renderAnnotations(annotations: List<CaAnnotation>, printer: CaPrettyPrinter)
}

object CaAnnotationRendererForSource {
    val WITH_QUALIFIED_NAMES: CaAnnotationRenderer = CaAnnotationRenderer { annotations, printer ->
        annotations.forEachIndexed { index, annotation ->
            if (index > 0) printer.append(" ")
            printer.append(renderAnnotation(annotation, useQualifiedName = true))
        }
        if (annotations.isNotEmpty()) {
            printer.append(" ")
        }
    }

    val WITH_SHORT_NAMES: CaAnnotationRenderer = CaAnnotationRenderer { annotations, printer ->
        annotations.forEachIndexed { index, annotation ->
            if (index > 0) printer.append(" ")
            printer.append(renderAnnotation(annotation, useQualifiedName = false))
        }
        if (annotations.isNotEmpty()) {
            printer.append(" ")
        }
    }

    private fun renderAnnotation(
        annotation: CaAnnotation,
        useQualifiedName: Boolean,
    ): String {
        val classId = annotation.classId
        val shortName = annotation.shortName
        val renderedName = when {
            useQualifiedName && classId != null -> classId.asFqNameString()
            shortName != null -> shortName.asString()
            else -> return annotation.renderedText
        }

        return buildString {
            append("@")
            append(renderedName)
            if (annotation.arguments.isNotEmpty()) {
                append(annotation.arguments.joinToString(prefix = "(", postfix = ")"))
            }
        }
    }
}
