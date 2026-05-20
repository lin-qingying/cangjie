package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 注解测试专用 renderer。
 *
 * 当前仓颉公开注解 renderer 仍在建设中，测试侧需要一份只依赖稳定 public API 字段的输出器：
 * - classId / shortName
 * - arguments
 * - 嵌套 meta-annotations
 */
object TestAnnotationRenderer {
    fun renderAnnotations(analysisSession: CaSession, annotations: CaAnnotationList): String = buildString {
        renderAnnotationsRecursive(analysisSession, annotations, currentMetaAnnotations = null, indent = 0)
    }

    fun renderSingleAnnotation(annotation: CaAnnotation): String = buildString {
        appendLine("annotations: [")
        appendLine(indent(renderAnnotation(annotation), 2))
        appendLine("]")
    }

    fun renderAnnotationsWithMeta(analysisSession: CaSession, annotations: CaAnnotationList): String = buildString {
        renderAnnotationsRecursive(analysisSession, annotations, currentMetaAnnotations = linkedSetOf(), indent = 0)
    }

    private fun StringBuilder.renderAnnotationsRecursive(
        analysisSession: CaSession,
        annotations: CaAnnotationList,
        currentMetaAnnotations: Set<ClassId>?,
        indent: Int,
    ) {
        appendLine(indent("annotations: [", indent))
        for (annotation in annotations) {
            appendLine(indent(renderAnnotation(annotation), indent + 2))
            if (currentMetaAnnotations == null) continue

            val classId = annotation.classId
            if (classId == null) {
                appendLine(indent("<unknown meta-annotation>", indent + 4))
                continue
            }

            if (classId in currentMetaAnnotations) {
                appendLine(indent("<recursive meta-annotation ${classId.asString()}>", indent + 4))
                continue
            }

            val metaAnnotations = with(analysisSession) {
                (getClassLikeSymbol(classId) as? CaDeclarationSymbol)?.annotations
            }

            if (metaAnnotations != null) {
                renderAnnotationsRecursive(
                    analysisSession = analysisSession,
                    annotations = metaAnnotations,
                    currentMetaAnnotations = currentMetaAnnotations + classId,
                    indent = indent + 4,
                )
            } else {
                appendLine(indent("<unknown meta-annotation ${classId.asString()}>", indent + 4))
            }
        }
        appendLine(indent("]", indent))
    }

    private fun renderAnnotation(annotation: CaAnnotation): String = buildString {
        append("@")
        append(annotation.classId?.asFqNameString() ?: annotation.shortName?.asString() ?: "<unresolved>")
        if (annotation.arguments.isNotEmpty()) {
            append("[")
            append(annotation.arguments.joinToString(", ") { renderNamedArgument(it) })
            append("]")
        }
    }

    private fun renderNamedArgument(argument: CaNamedAnnotationValue): String {
        return "${argument.name.asString()} = ${renderValue(argument.expression)}"
    }

    private fun renderValue(value: CaAnnotationValue): String {
        return when (value) {
            is CaAnnotationValue.ConstantValue -> value.value.render()
            is CaAnnotationValue.EnumValue -> buildString {
                append(value.callableId?.toString() ?: "<unresolved-enum>")
                if (value.arguments.isNotEmpty()) {
                    append("[")
                    append(value.arguments.joinToString(", ") { renderValue(it) })
                    append("]")
                }
            }

            is CaAnnotationValue.TupleValue ->
                value.values.joinToString(prefix = "(", postfix = ")") { element -> renderValue(element) }

            is CaAnnotationValue.ClassInstanceValue -> buildString {
                append(value.classId?.asFqNameString() ?: "<unresolved-class-instance>")
                append("{")
                append(value.arguments.joinToString(", ") { argument -> renderNamedArgument(argument) })
                append("}")
            }

            is CaAnnotationValue.StructInstanceValue -> buildString {
                append(value.classId?.asFqNameString() ?: "<unresolved-struct-instance>")
                append("{")
                append(value.arguments.joinToString(", ") { argument -> renderNamedArgument(argument) })
                append("}")
            }
        }
    }

    private fun indent(text: String, indent: Int): String = " ".repeat(indent) + text
}
