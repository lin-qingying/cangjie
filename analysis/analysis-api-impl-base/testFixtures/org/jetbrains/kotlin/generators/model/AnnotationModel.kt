package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.util.isDefaultImportedClass
import org.jetbrains.kotlin.utils.Printer

/**
 * Java 注解的源码生成模型。
 *
 * @property annotation 需要写入生成源码的注解类型。
 * @property arguments 注解实参列表，单个默认 `value` 参数会省略显式名称。
 */
class AnnotationModel(
    /**
     * 需要写入生成源码的注解类型。
     */
    val annotation: Class<out Annotation>,
    /**
     * 注解实参列表，单个默认 `value` 参数会省略显式名称。
     */
    val arguments: List<AnnotationArgumentModel>,
) {
    /**
     * 将该注解模型渲染到 Java 源码打印器。
     */
    fun generate(p: Printer) {
        val needExplicitNames = arguments.singleOrNull()?.name != AnnotationArgumentModel.DEFAULT_NAME
        val argumentsString = arguments.joinToString(separator = ", ") { argument ->
            val valueString = when (val value = argument.value) {
                is Enum<*> -> "${value.javaClass.simpleName}.${value.name}"
                is Array<*> -> value.toJavaString()
                is Class<*> -> "${value.simpleName}.class"
                else -> "\"$value\""
            }
            if (needExplicitNames) "${argument.name} = $valueString" else valueString
        }
        p.print("@${annotation.simpleName}($argumentsString)")
    }

    /**
     * 将数组注解实参渲染成 Java 注解可接受的数组字面量。
     */
    private fun Array<*>.toJavaString(): String =
        buildString {
            append("{ ")
            append(this@toJavaString.joinToString(separator = ", ") { "\"$it\"" })
            append(" }")
        }

    /**
     * 返回渲染该注解及其实参时需要导入的非默认包类型。
     */
    fun imports(): List<Class<*>> {
        return buildList {
            add(annotation)
            arguments.mapNotNullTo(this) { argument ->
                when (val value = argument.value) {
                    is Enum<*> -> value.javaClass
                    is Class<*> -> value
                    else -> null
                }
            }
        }.filterNot { it.isDefaultImportedClass() }
    }
}

/**
 * 创建只有默认 `value` 实参的注解模型。
 */
fun annotation(annotation: Class<out Annotation>, singleArgumentValue: Any): AnnotationModel {
    return AnnotationModel(annotation, listOf(AnnotationArgumentModel(value = singleArgumentValue)))
}

/**
 * 创建包含显式命名实参的注解模型。
 */
fun annotation(annotation: Class<out Annotation>, vararg arguments: Pair<String, Any>): AnnotationModel {
    return AnnotationModel(annotation, arguments.map { AnnotationArgumentModel(it.first, it.second) })
}
