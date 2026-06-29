package org.cangnova.cangjie.utils

/**
 * 支持缩进输出的打印器接口。
 */
interface IndentingPrinter {
    /**
     * 当前输出行使用的缩进字符串。
     */
    val currentIndent: String

    /**
     * 当前缩进层级数。
     */
    val currentIndentLengthInUnits: Int

    /**
     * 单个缩进单元的字符长度。
     */
    val indentUnitLength: Int

    /**
     * 打印对象并追加换行符。
     */
    fun println(vararg objects: Any?): IndentingPrinter

    /**
     * 打印对象但不主动换行。
     */
    fun print(vararg objects: Any?): IndentingPrinter

    /**
     * 按当前缩进打印多行字符串。
     */
    fun printlnMultiLine(s: String): IndentingPrinter

    /**
     * 增加一个缩进层级。
     */
    fun pushIndent(): IndentingPrinter

    /**
     * 减少一个缩进层级。
     */
    fun popIndent(): IndentingPrinter

    /**
     * 返回当前打印器已写出的文本。
     */
    override fun toString(): String
}

/**
 * 在临时增加一个缩进层级的上下文中执行 [block]。
 */
inline fun IndentingPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}
