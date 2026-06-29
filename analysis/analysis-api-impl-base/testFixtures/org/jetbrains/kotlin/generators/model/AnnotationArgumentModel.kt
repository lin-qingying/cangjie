package org.jetbrains.kotlin.generators.model

/**
 * Java 注解实参的生成模型。
 *
 * @property name 注解参数名，默认使用 Java 注解的 `value` 参数。
 * @property value 注解参数值，生成时会根据枚举、数组、Class 和普通值分别渲染。
 */
class AnnotationArgumentModel(
    /**
     * 注解参数名，默认使用 Java 注解的 `value` 参数。
     */
    val name: String = DEFAULT_NAME,
    /**
     * 注解参数值，生成时会根据类型渲染为 Java 注解字面量。
     */
    val value: Any,
) {
    /**
     * 注解单值参数的默认名称。
     */
    companion object {
        /**
         * Java 注解约定的默认参数名。
         */
        const val DEFAULT_NAME = "value"
    }
}
