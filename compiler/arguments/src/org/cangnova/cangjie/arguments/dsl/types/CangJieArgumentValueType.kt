package org.cangnova.cangjie.arguments.dsl.types

import org.cangnova.cangjie.arguments.dsl.base.ExperimentalArgumentApi
import org.cangnova.cangjie.arguments.dsl.base.ReleaseDependent

/**
 * 编译器参数值类型的基类，负责描述默认值、可空性和生成源码中的字面量表示。
 */
@ExperimentalArgumentApi
sealed class CangJieArgumentValueType<T> {
    /**
     * 该参数类型对应的版本化默认值。
     */
    abstract val defaultValue: ReleaseDependent<T?>

    /**
     * 该参数类型在各版本中是否允许为空。
     */
    abstract val isNullable: ReleaseDependent<Boolean>

    /**
     * 将运行时默认值渲染为生成源码可使用的 Kotlin 表达式文本。
     */
    abstract fun stringRepresentation(value: T?): String?
}

/**
 * 布尔命令行参数值类型。
 */
@ExperimentalArgumentApi
data class BooleanType(
    /**
     * 布尔参数的版本化默认值。
     */
    override val defaultValue: ReleaseDependent<Boolean?>,
    /**
     * 布尔参数默认不可为空。
     */
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<Boolean>() {
    /**
     * 将布尔值渲染为 `true` 或 `false` 字面量。
     */
    override fun stringRepresentation(value: Boolean?): String? = value?.toString()
}

/**
 * 普通字符串命令行参数值类型。
 */
@ExperimentalArgumentApi
data class StringType(
    /**
     * 字符串参数的版本化默认值。
     */
    override val defaultValue: ReleaseDependent<String?>,
    /**
     * 字符串参数默认允许为空。
     */
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)
) : CangJieArgumentValueType<String>() {
    /**
     * 将字符串默认值渲染为带双引号的源码字面量。
     */
    override fun stringRepresentation(value: String?): String? = value?.let { "\"$it\"" }
}

/**
 * 字符串数组命令行参数值类型。
 */
@ExperimentalArgumentApi
data class StringArrayType(
    /**
     * 字符串数组参数的版本化默认值。
     */
    override val defaultValue: ReleaseDependent<Array<String>?> = ReleaseDependent(emptyArray()),
    /**
     * 字符串数组参数默认不可为空。
     */
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<Array<String>>() {
    /**
     * 将数组默认值渲染为当前生成器使用的空数组表达式。
     */
    override fun stringRepresentation(value: Array<String>?): String = "emptyArray()"
}

/**
 * 字符串列表命令行参数值类型。
 */
@ExperimentalArgumentApi
data class StringListType(
    /**
     * 字符串列表参数的版本化默认值。
     */
    override val defaultValue: ReleaseDependent<List<String>?> = ReleaseDependent(emptyList()),
    /**
     * 字符串列表参数默认不可为空。
     */
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<List<String>>() {
    /**
     * 将字符串列表默认值渲染为逗号分隔的带引号文本。
     */
    override fun stringRepresentation(value: List<String>?): String? =
        value?.joinToString(", ") { "\"$it\"" }
}

/**
 * 系统路径命令行参数值类型。
 */
@ExperimentalArgumentApi
data class SystemPathType(
    /**
     * 系统路径参数的版本化默认值。
     */
    override val defaultValue: ReleaseDependent<String?> = ReleaseDependent(null),
    /**
     * 系统路径参数默认允许为空。
     */
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)
) : CangJieArgumentValueType<String>() {
    /**
     * 将系统路径默认值渲染为字符串字面量。
     */
    override fun stringRepresentation(value: String?): String? = value?.let { "\"$it\"" }
}

/**
 * 字面路径数组命令行参数值类型。
 */
@ExperimentalArgumentApi
data class LiteralPathType(
    /**
     * 字面路径数组参数的版本化默认值。
     */
    override val defaultValue: ReleaseDependent<Array<String>?> = ReleaseDependent(emptyArray()),
    /**
     * 字面路径数组参数默认不可为空。
     */
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<Array<String>>() {
    /**
     * 将路径数组默认值渲染为当前生成器使用的空数组表达式。
     */
    override fun stringRepresentation(value: Array<String>?): String = "emptyArray()"
}
