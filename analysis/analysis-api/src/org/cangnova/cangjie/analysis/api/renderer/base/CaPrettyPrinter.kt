package org.cangnova.cangjie.analysis.api.renderer.base

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.cangnova.cangjie.utils.ifTrue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * renderer 体系使用的轻量 pretty printer。
 *
 * 公开 renderer 不依赖 CFIR 内部打印器，
 * 以免把底层树渲染约束泄漏到 Analysis API 公共层。
 *
 * ## 使用场景
 * - 将类型、声明、表达式等 AST 节点格式化为人类可读字符串
 * - 在 IDE 悬停提示、代码补全、错误信息等场景中渲染结构化文本
 * - 需要精确控制缩进、前缀、分隔符的输出场景
 *
 * ## 基本使用示例
 * ```kotlin
 * val result = prettyPrint {
 *     append("fun foo(")
 *     withIndent {
 *         append("a: Int,")
 *         appendLine()
 *         append("b: String")
 *     }
 *     append(")")
 * }
 * // 输出：
 * // fun foo(
 * //   a: Int,
 * //   b: String
 * // )
 * ```
 *
 * @param indentSize 每级缩进的空格数，默认为 2
 */
class PrettyPrinter(val indentSize: Int = 2) : Appendable {

    /** 内部字符串构建器，所有输出最终写入此处 */
    @PublishedApi
    internal val builder: StringBuilder = StringBuilder()

    /**
     * 待打印的前缀队列（不可变持久化列表）。
     *
     * 使用 [PersistentList] 而非普通 List，是为了在 [withPrefix] 中
     * 能安全地保存/恢复快照，避免副作用。
     */
    @PublishedApi
    internal var prefixesToPrint: PersistentList<String> = persistentListOf()

    /** 当前缩进层级，每调用一次 [withIndent] 加 1，退出后减 1 */
    @PublishedApi
    internal var indent: Int = 0

    /**
     * 追加字符序列，自动处理换行和缩进。
     *
     * - `null` 会被渲染为字符串 `"null"`
     * - 内容按 `\n` 分割后逐行写入，每行开头自动补充当前缩进
     * - 空行（仅含换行）不追加缩进，避免产生尾随空白
     *
     * ## 示例
     * ```kotlin
     * prettyPrint {
     *     append("line1\nline2")
     * }
     * // 输出：
     * // line1
     * // line2
     * ```
     */
    override fun append(nullableSeq: CharSequence?): Appendable = apply {
        val seq = nullableSeq ?: "null"
        if (seq.isEmpty()) return@apply
        printPrefixes()
        seq.split('\n').forEachIndexed { index, line ->
            if (index > 0) {
                builder.append('\n')
            }
            if (line.isNotEmpty()) {
                appendIndentIfNeeded()
                builder.append(line)
            }
        }
    }

    /** 追加字符序列的子序列 [start, end)，内部委托给 [append] */
    override fun append(nullableSeq: CharSequence?, start: Int, end: Int): Appendable = apply {
        append((nullableSeq ?: "null").subSequence(start, end))
    }

    /**
     * 追加单个字符，换行符不追加缩进，其他字符按需补充缩进。
     *
     * ## 示例
     * ```kotlin
     * prettyPrint {
     *     append('A')
     *     append('\n')
     *     append('B') // 'B' 前会自动补缩进
     * }
     * ```
     */
    override fun append(c: Char): Appendable = apply {
        printPrefixes()
        if (c != '\n') {
            appendIndentIfNeeded()
        }
        builder.append(c)
    }

    /**
     * 将 [prefixesToPrint] 中所有待打印前缀一次性刷出，随后清空队列。
     * 在实际写入内容前调用，确保前缀紧贴后续内容。
     */
    private fun printPrefixes() {
        if (prefixesToPrint.isNotEmpty()) {
            appendIndentIfNeeded()
            prefixesToPrint.forEach { builder.append(it) }
            prefixesToPrint = persistentListOf()
        }
    }

    /**
     * 在 [block] 执行期间增加一级缩进（+1），退出后恢复。
     *
     * ## 示例
     * ```kotlin
     * prettyPrint {
     *     append("class Foo {")
     *     appendLine()
     *     withIndent {
     *         append("val x: Int")  // 输出前有 2 个空格缩进
     *     }
     *     appendLine()
     *     append("}")
     * }
     * ```
     */
    inline fun withIndent(block: PrettyPrinter.() -> Unit) {
        indent += 1
        block(this)
        indent -= 1
    }

    /**
     * 在 [block] 执行期间增加 [indentCount] 级缩进，退出后恢复。
     *
     * 适用于需要一次性嵌套多层缩进的场景，如对齐多级嵌套结构。
     *
     * @param indentCount 要增加的缩进层数，必须 >= 0
     * @throws IllegalArgumentException 若 [indentCount] 为负数
     */
    inline fun withIndents(indentCount: Int, block: PrettyPrinter.() -> Unit) {
        require(indentCount >= 0) { "Number of indents should be non-negative" }
        indent += indentCount
        block(this)
        indent -= indentCount
    }

    /**
     * 用花括号 `{ }` 包裹内容，内部自动增加一级缩进。
     *
     * ## 示例
     * ```kotlin
     * prettyPrint {
     *     append("fun foo()")
     *     withIndentInBraces {
     *         append("return 42")
     *     }
     * }
     * // 输出：
     * // fun foo()
     * // {
     * //   return 42
     * // }
     * ```
     */
    inline fun withIndentInBraces(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "{", after = "}", block)
    }

    /**
     * 用方括号 `[ ]` 包裹内容，内部自动增加一级缩进。
     *
     * 适用于渲染注解参数列表、数组字面量等场景。
     *
     * ## 示例
     * ```kotlin
     * prettyPrint {
     *     append("@Ann")
     *     withIndentInSquareBrackets {
     *         append("value = 1")
     *     }
     * }
     * // 输出：
     * // @Ann
     * // [
     * //   value = 1
     * // ]
     * ```
     */
    inline fun withIndentInSquareBrackets(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "[", after = "]", block)
    }

    /**
     * 用自定义的 [before] 和 [after] 字符串包裹内容，内部自动增加一级缩进。
     * [withIndentInBraces] 和 [withIndentInSquareBrackets] 均委托此方法实现。
     *
     * @param before 包裹开始符号，如 `"{"`、`"("`
     * @param after  包裹结束符号，如 `"}"`、`")"`
     */
    inline fun withIndentWrapped(before: String, after: String, block: PrettyPrinter.() -> Unit) {
        append(before)
        appendLine()
        withIndent(block)
        appendLine()
        append(after)
    }

    /**
     * 遍历集合并逐项渲染，支持自定义分隔符、前缀、后缀。
     *
     * ## 示例
     * ```kotlin
     * prettyPrint {
     *     printCollection(
     *         listOf("Int", "String", "Boolean"),
     *         separator = ", ",
     *         prefix = "<",
     *         postfix = ">"
     *     ) { append(it) }
     * }
     * // 输出：<Int, String, Boolean>
     * ```
     *
     * @param collection 要渲染的集合
     * @param separator  元素间的分隔符，默认 `", "`
     * @param prefix     整体前缀，默认空字符串
     * @param postfix    整体后缀，默认空字符串
     * @param renderItem 单个元素的渲染逻辑
     */
    inline fun <T> printCollection(
        collection: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        renderItem: PrettyPrinter.(T) -> Unit
    ) {
        append(prefix)
        val iterator = collection.iterator()
        while (iterator.hasNext()) {
            renderItem(iterator.next())
            if (iterator.hasNext()) {
                append(separator)
            }
        }
        append(postfix)
    }

    /**
     * 与 [printCollection] 相同，但集合为空时**直接跳过**，不输出任何内容（包括前缀后缀）。
     *
     * ## 使用场景
     * 渲染泛型参数列表时，若无类型参数则完全省略 `<>`：
     * ```kotlin
     * prettyPrint {
     *     append("List")
     *     printCollectionIfNotEmpty(typeParams, prefix = "<", postfix = ">") {
     *         append(it)
     *     }
     * }
     * // 有参数时输出：List<Int>
     * // 无参数时输出：List
     * ```
     */
    inline fun <T> printCollectionIfNotEmpty(
        collection: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        renderItem: PrettyPrinter.(T) -> Unit
    ) {
        if (!collection.iterator().hasNext()) return
        printCollection(collection, separator, prefix, postfix, renderItem)
    }

    /**
     * 若末尾字符不是 [char]，则追加该字符。用于避免重复写入同一字符。
     *
     * ## 使用场景
     * 确保语句结尾恰好有一个分号或换行，即使多处代码都尝试写入：
     * ```kotlin
     * prettyPrint {
     *     append("val x = 1")
     *     printCharIfNotThere('\n')
     *     printCharIfNotThere('\n') // 不会重复写入
     * }
     * ```
     */
    fun printCharIfNotThere(char: Char) {
        if (builder.lastOrNull() != char) {
            append(char)
        }
    }

    /**
     * 若当前位置需要缩进（builder 为空或上一个字符是 `\n`），则补充缩进空格。
     * 保证每一行开头恰好有正确数量的空格，且不会重复追加。
     */
    private fun appendIndentIfNeeded() {
        if (builder.isEmpty() || builder[builder.lastIndex] == '\n') {
            builder.append(" ".repeat(indentSize * indent))
        }
    }

    /** 返回当前已构建的完整字符串 */
    override fun toString(): String {
        return builder.toString()
    }

    /**
     * 执行 [render] 并返回它是否产生了任何输出（即 builder 长度是否增加）。
     *
     * 常与条件渲染配合使用，判断某部分是否被实际打印，从而决定是否追加分隔符等。
     *
     * ## 示例
     * ```kotlin
     * val printed = checkIfPrinted {
     *     renderOptionalAnnotations()  // 可能不输出任何内容
     * }
     * if (printed) append(" ")  // 只在有注解时补一个空格
     * ```
     *
     * @return `true` 表示 [render] 有输出；`false` 表示无任何输出
     */
    @OptIn(ExperimentalContracts::class)
    inline fun checkIfPrinted(render: () -> Unit): Boolean {
        contract { callsInPlace(render, InvocationKind.EXACTLY_ONCE) }
        val initialSize = builder.length
        render()
        return initialSize != builder.length
    }

    /**
     * 让 PrettyPrinter 实例可以像函数一样被调用，等价于直接在其上执行 [print]。
     *
     * ## 示例
     * ```kotlin
     * val printer = PrettyPrinter()
     * printer {
     *     append("hello")
     * }
     * ```
     */
    inline operator fun invoke(print: PrettyPrinter.() -> Unit) {
        this.print()
    }

    /**
     * 以当前字符串（`this`）为分隔符，按顺序渲染 [p1] 和 [p2]：
     * 若 [p1] 有输出，则在 [p2] 前自动插入该分隔符；若 [p1] 无输出，则直接渲染 [p2]。
     *
     * ## 使用场景
     * 渲染 `modifiers returnType name` 这类由空格分隔、各部分可选的结构：
     * ```kotlin
     * prettyPrint {
     *     " ".separated(
     *         { renderModifiers() },   // 可能为空
     *         { append("String") }     // 返回类型
     *     )
     * }
     * // 有修饰符时：public String
     * // 无修饰符时：String
     * ```
     */
    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
        }
        val firstRendered = checkIfPrinted { p1() }
        if (firstRendered) {
            withPrefix(this, p2)
        } else {
            p2()
        }
    }

    /** [String.separated] 的三元素版本，依次渲染 [p1]、[p2]、[p3]，各部分间按需插入分隔符 */
    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2) }, p3)
    }

    /** [String.separated] 的四元素版本 */
    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p4, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3) }, p4)
    }

    /** [String.separated] 的五元素版本 */
    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit, p5: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p5, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3, p4) }, p5)
    }

    /**
     * 为 [print] 的输出添加前缀 [prefix]：若 [print] 有输出，前缀紧贴内容打印；
     * 若 [print] 无输出，前缀也不会打印（利用 [prefixesToPrint] 延迟机制实现）。
     *
     * ## 使用场景
     * 在分隔符场景中，只有后一部分确实有内容时才插入分隔符：
     * ```kotlin
     * prettyPrint {
     *     append("public")
     *     withPrefix(" ") {
     *         renderReturnType()  // 有内容时输出 "public String"，无内容时输出 "public"
     *     }
     * }
     * ```
     *
     * @param prefix 要添加的前缀字符串
     * @param print  实际打印内容的 lambda
     */
    @OptIn(ExperimentalContracts::class)
    inline fun withPrefix(prefix: String, print: () -> Unit) {
        contract {
            callsInPlace(print, InvocationKind.EXACTLY_ONCE)
        }
        val currentPrefixes = prefixesToPrint
        prefixesToPrint = prefixesToPrint.add(prefix)
        try {
            print()
        } finally {
            // 若 print() 没有实际输出，prefixesToPrint 未被消费，恢复快照丢弃该前缀
            if (prefixesToPrint.isNotEmpty()) {
                prefixesToPrint = currentPrefixes
            }
        }
    }

    /**
     * 若 [p1] 有任何输出，则在其后追加 [suffix]；若无输出则什么也不做。
     *
     * ## 使用场景
     * 渲染可选的尾随逗号、分号等：
     * ```kotlin
     * prettyPrint {
     *     withSuffix(",") {
     *         renderOptionalParam()  // 有内容时输出 "param,"，无内容时输出空
     *     }
     * }
     * ```
     */
    inline fun withSuffix(suffix: String, p1: () -> Unit) {
        checkIfPrinted { p1() }.ifTrue { append(suffix) }
    }
}

/**
 * 创建一个新的 [PrettyPrinter] 并执行 [body]，返回最终字符串。
 *
 * ## 示例
 * ```kotlin
 * val code = prettyPrint {
 *     append("fun hello() {")
 *     appendLine()
 *     withIndent {
 *         append("println(\"Hello\")")
 *     }
 *     appendLine()
 *     append("}")
 * }
 * ```
 */
inline fun prettyPrint(body: PrettyPrinter.() -> Unit): String =
    PrettyPrinter().apply(body).toString()

/**
 * 以 [other] 的配置（目前仅 [PrettyPrinter.indentSize]）创建新的 [PrettyPrinter] 并执行 [body]。
 *
 * ## 使用场景
 * 在嵌套 renderer 中，子 renderer 需要与父 renderer 保持相同的缩进风格：
 * ```kotlin
 * fun renderInner(outer: PrettyPrinter) {
 *     val inner = prettyPrintWithSettingsFrom(outer) {
 *         append("inner content")
 *     }
 *     outer.append(inner)
 * }
 * ```
 */
@OptIn(ExperimentalContracts::class)
inline fun prettyPrintWithSettingsFrom(other: PrettyPrinter, body: PrettyPrinter.() -> Unit): String {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return PrettyPrinter(other.indentSize).apply(body).toString()
}