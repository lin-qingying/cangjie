package org.cangnova.cangjie.codegen.ir

class IRBuilder {
    private val lines = mutableListOf<String>()

    fun comment(text: String) {
        lines += "; $text"
    }

    fun emit(text: String) {
        lines += text
    }

    fun label(name: String) {
        lines += "$name:"
    }

    fun build(): List<String> = lines.toList()
}

