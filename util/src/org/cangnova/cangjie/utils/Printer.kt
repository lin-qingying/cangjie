package org.cangnova.cangjie.utils

import java.io.IOException

/**
 * 带缩进能力的通用打印器。
 *
 * 基于 Kotlin `core/util.runtime` 的 `Printer` 语义实现，供编译器一方模块复用。
 */
open class Printer private constructor(
    /**
     * 接收打印文本的底层输出目标。
     */
    private val out: Appendable,
    /**
     * 允许连续输出的最大空行数。
     */
    private val maxBlankLines: Int,
    /**
     * 单个缩进层级使用的字符串。
     */
    private val indentUnit: String,
    indent: String,
) : IndentingPrinter {
    /**
     * 当前输出行使用的缩进字符串。
     */
    final override var currentIndent: String = indent
        private set

    /**
     * 包含当前行在内的连续空行计数。
     */
    private var blankLineCountIncludingCurrent = 0
    /**
     * 下一次输出是否临时跳过自动缩进。
     */
    private var withholdIndentOnce = false
    /**
     * 已写出的字符数量。
     */
    private var length = 0

    constructor(out: Appendable, indentUnit: String) : this(out, Int.MAX_VALUE, indentUnit)

    @JvmOverloads
    constructor(
        out: Appendable,
        maxBlankLines: Int = Int.MAX_VALUE,
        indentUnit: String = DEFAULT_INDENTATION_UNIT,
    ) : this(out, maxBlankLines, indentUnit, indent = "")

    constructor(out: Appendable, parent: Printer) : this(out, parent.maxBlankLines, parent.indentUnit, parent.currentIndent)

    /**
     * 向底层 [out] 追加单个对象的字符串形式。
     */
    private fun append(o: Any?) {
        try {
            val string = o.toString()
            out.append(string)
            length += string.length
        } catch (e: IOException) {
            throw IllegalStateException("Failed to append printer output", e)
        }
    }

    /**
     * 输出对象并追加行分隔符。
     */
    override fun println(vararg objects: Any?): Printer {
        print(*objects)
        printLineSeparator()
        return this
    }

    /**
     * 在空行限制内输出一个行分隔符。
     */
    private fun printLineSeparator() {
        if (blankLineCountIncludingCurrent <= maxBlankLines) {
            blankLineCountIncludingCurrent++
            append(LINE_SEPARATOR)
        }
    }

    /**
     * 按当前缩进输出对象。
     */
    override fun print(vararg objects: Any?): Printer {
        if (withholdIndentOnce) {
            withholdIndentOnce = false
        } else if (objects.isNotEmpty()) {
            printIndent()
        }
        printWithNoIndent(*objects)
        return this
    }

    /**
     * 输出当前缩进字符串。
     */
    fun printIndent() {
        append(currentIndent)
    }

    /**
     * 不添加缩进地输出对象。
     */
    fun printWithNoIndent(vararg objects: Any?): Printer {
        for (`object` in objects) {
            blankLineCountIncludingCurrent = 0
            append(`object`)
        }
        return this
    }

    /**
     * 使下一次 [print] 调用不输出缩进。
     */
    fun withholdIndentOnce(): Printer {
        withholdIndentOnce = true
        return this
    }

    /**
     * 不添加缩进地输出对象并换行。
     */
    fun printlnWithNoIndent(vararg objects: Any?): Printer {
        printWithNoIndent(*objects)
        printLineSeparator()
        return this
    }

    /**
     * 按当前缩进输出多行字符串。
     */
    override fun printlnMultiLine(s: String): IndentingPrinter {
        printlnWithNoIndent(
            s.replaceIndent(currentIndent)
                .lines()
                .joinToString(separator = "\n") { if (it.isBlank()) "" else it }
        )
        return this
    }

    /**
     * 增加一个缩进层级。
     */
    override fun pushIndent(): Printer {
        currentIndent += indentUnit
        return this
    }

    /**
     * 减少一个缩进层级。
     */
    override fun popIndent(): Printer {
        check(currentIndent.length >= indentUnit.length) { "No indentation to pop" }
        currentIndent = currentIndent.substring(indentUnit.length)
        return this
    }

    /**
     * 使用 [separator] 分隔输出可变参数中的每个条目。
     */
    fun separated(separator: Any, vararg items: Any?): Printer {
        for (index in items.indices) {
            if (index > 0) {
                printlnWithNoIndent(separator)
            }
            printlnWithNoIndent(items[index])
        }
        return this
    }

    /**
     * 使用 [separator] 分隔输出集合中的每个条目。
     */
    fun separated(separator: Any, items: Collection<*>): Printer {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            printlnWithNoIndent(iterator.next())
            if (iterator.hasNext()) {
                printlnWithNoIndent(separator)
            }
        }
        return this
    }

    /**
     * 当前打印器是否尚未输出任何字符。
     */
    val isEmpty: Boolean
        get() = length == 0

    /**
     * 返回底层输出目标的字符串形式。
     */
    override fun toString(): String = out.toString()

    /**
     * 当前缩进层级数。
     */
    override val currentIndentLengthInUnits: Int
        get() = currentIndent.length / indentUnit.length

    /**
     * 单个缩进单元的字符长度。
     */
    override val indentUnitLength: Int
        get() = indentUnit.length

    /**
     * 打印器默认常量。
     */
    companion object {
        /**
         * 默认四空格缩进。
         */
        private const val DEFAULT_INDENTATION_UNIT = "    "

        /**
         * 两空格缩进。
         */
        const val TWO_SPACE_INDENT = "  "

        /**
         * 当前运行平台的行分隔符。
         */
        @JvmField
        val LINE_SEPARATOR: String = System.lineSeparator()
    }
}
