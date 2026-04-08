package org.cangnova.cangjie.analysis.api.renderer.base

/**
 * renderer 体系使用的轻量 pretty printer。
 *
 * 公开 renderer 不依赖 CFIR 内部打印器，
 * 以免把底层树渲染约束泄漏到 Analysis API 公共层。
 */
class CaPrettyPrinter(
    private val indentUnit: String = "    ",
) {
    private val builder = StringBuilder()
    private var currentIndentLevel: Int = 0
    private var lineStart: Boolean = true

    fun append(text: String) {
        if (text.isEmpty()) return
        ensureIndent()
        builder.append(text)
    }

    fun appendLine(text: String = "") {
        if (text.isNotEmpty()) {
            append(text)
        }
        builder.appendLine()
        lineStart = true
    }

    fun withIndent(block: CaPrettyPrinter.() -> Unit) {
        currentIndentLevel++
        try {
            block()
        } finally {
            currentIndentLevel--
        }
    }

    private fun ensureIndent() {
        if (!lineStart) return
        repeat(currentIndentLevel) { builder.append(indentUnit) }
        lineStart = false
    }

    override fun toString(): String = builder.toString()
}

inline fun prettyPrint(block: CaPrettyPrinter.() -> Unit): String {
    val printer = CaPrettyPrinter()
    printer.block()
    return printer.toString().trimEnd()
}
