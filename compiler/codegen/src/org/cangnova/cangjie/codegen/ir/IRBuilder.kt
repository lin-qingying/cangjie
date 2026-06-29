package org.cangnova.cangjie.codegen.ir

/**
 * LLVM IR 文本行构造器。
 */
class IRBuilder {
    /**
     * 已按顺序收集的 LLVM IR 行。
     */
    private val lines = mutableListOf<String>()

    /**
     * 追加 LLVM 注释行。
     */
    fun comment(text: String) {
        lines += "; $text"
    }

    /**
     * 追加原始 LLVM IR 行。
     */
    fun emit(text: String) {
        lines += text
    }

    /**
     * 追加基本块 label 行。
     */
    fun label(name: String) {
        lines += "$name:"
    }

    /**
     * 返回当前构造出的 LLVM IR 行列表快照。
     */
    fun build(): List<String> = lines.toList()
}
