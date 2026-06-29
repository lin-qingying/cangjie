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
    /**
     * 渲染普通注解列表。
     *
     * 输出只包含当前 `CaAnnotationList` 的直接注解，不递归展开注解类上的 meta-annotations。
     */
    fun renderAnnotations(analysisSession: CaSession, annotations: CaAnnotationList): String = buildString {
        renderAnnotationsRecursive(analysisSession, annotations, currentMetaAnnotations = null, indent = 0)
    }

    /**
     * 渲染单个注解为标准注解列表片段。
     *
     * 该方法服务按 ClassId 访问的测试，使直接访问和解析访问得到完全相同的文本比较面。
     */
    fun renderSingleAnnotation(annotation: CaAnnotation): String = buildString {
        appendLine("annotations: [")
        appendLine(indent(renderAnnotation(annotation), 2))
        appendLine("]")
    }

    /**
     * 渲染注解列表并递归展开 meta-annotations。
     *
     * 渲染过程中会记录当前递归链上的 `ClassId`，避免自递归或环形 meta-annotation 造成无限展开。
     */
    fun renderAnnotationsWithMeta(analysisSession: CaSession, annotations: CaAnnotationList): String = buildString {
        renderAnnotationsRecursive(analysisSession, annotations, currentMetaAnnotations = linkedSetOf(), indent = 0)
    }

    /**
     * 递归渲染注解列表及其可选 meta-annotations。
     *
     * `currentMetaAnnotations == null` 表示只渲染直接注解；非空集合表示启用递归并携带当前访问栈。
     */
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

    /**
     * 渲染单个 `CaAnnotation` 的 class id、短名和实参。
     *
     * 未解析注解会显式输出占位文本，保证解析缺失在 golden 中可见。
     */
    private fun renderAnnotation(annotation: CaAnnotation): String = buildString {
        append("@")
        append(annotation.classId?.asFqNameString() ?: annotation.shortName?.asString() ?: "<unresolved>")
        if (annotation.arguments.isNotEmpty()) {
            append("[")
            append(annotation.arguments.joinToString(", ") { renderNamedArgument(it) })
            append("]")
        }
    }

    /**
     * 渲染具名注解实参。
     *
     * 输出格式固定为 `name = value`，由上层 annotation renderer 负责拼接多个参数。
     */
    private fun renderNamedArgument(argument: CaNamedAnnotationValue): String {
        return "${argument.name.asString()} = ${renderValue(argument.expression)}"
    }

    /**
     * 渲染公开注解值模型。
     *
     * 覆盖常量、枚举、元组、class instance 和 struct instance，确保所有公开 `CaAnnotationValue`
     * 子类型都有稳定 golden 表达。
     */
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

    /**
     * 为单行文本添加指定数量的前导空格。
     *
     * 注解 renderer 用它保持嵌套 meta-annotation 输出的层级缩进稳定。
     */
    private fun indent(text: String, indent: Int): String = " ".repeat(indent) + text
}
