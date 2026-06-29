

package org.cangnova.cangjie.utils

/**
 * 包装 [Printer] 的智能打印器。
 *
 * 首次输出保留普通缩进行为，连续输出时避免重复输出缩进，适合构造紧凑的声明文本。
 */
class SmartPrinter private constructor(
    /**
     * 承担实际输出和缩进管理的底层打印器。
     */
    private val printer: Printer,
) : IndentingPrinter by printer {
    constructor(appendable: Appendable, indent: String = DEFAULT_INDENT) : this(Printer(appendable, indent))

    /**
     * 默认缩进常量。
     */
    companion object {
        /**
         * 默认四空格缩进。
         */
        private const val DEFAULT_INDENT = "    "
    }

    /**
     * 当前行是否已经输出过内容。
     */
    private var notFirstPrint: Boolean = false

    /**
     * 输出对象；同一行后续输出不再重复缩进。
     */
    override fun print(vararg objects: Any?): SmartPrinter {
        if (notFirstPrint) {
            printer.printWithNoIndent(*objects)
        } else {
            printer.print(*objects)
        }
        notFirstPrint = true
        return this
    }

    /**
     * 输出对象并换行，同时重置首段输出状态。
     */
    override fun println(vararg objects: Any?): SmartPrinter {
        if (notFirstPrint) {
            printer.printlnWithNoIndent(*objects)
        } else {
            printer.println(*objects)
        }
        notFirstPrint = false
        return this
    }

    /**
     * 输出多行文本并重置首段输出状态。
     */
    override fun printlnMultiLine(s: String): SmartPrinter {
        printer.printlnMultiLine(s)
        notFirstPrint = false
        return this
    }
}
