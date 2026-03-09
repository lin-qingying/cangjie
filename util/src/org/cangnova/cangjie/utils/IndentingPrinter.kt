package org.cangnova.cangjie.utils

/**
 * 支持缩进输出的打印器接口。
 */
interface IndentingPrinter {
    val currentIndent: String

    val currentIndentLengthInUnits: Int

    val indentUnitLength: Int

    fun println(vararg objects: Any?): IndentingPrinter

    fun print(vararg objects: Any?): IndentingPrinter

    fun printlnMultiLine(s: String): IndentingPrinter

    fun pushIndent(): IndentingPrinter

    fun popIndent(): IndentingPrinter

    override fun toString(): String
}

inline fun IndentingPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}
