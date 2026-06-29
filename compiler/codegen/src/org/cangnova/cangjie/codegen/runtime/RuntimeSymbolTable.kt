package org.cangnova.cangjie.codegen.runtime

/**
 * codegen 可引用的运行时符号。
 */
data class RuntimeSymbol(
    /**
     * 运行时符号名。
     */
    val name: String,
    /**
     * LLVM 函数签名文本。
     */
    val llvmSignature: String,
)

/**
 * LLVM codegen 使用的运行时符号表。
 */
class RuntimeSymbolTable {
    /**
     * 按符号名索引的运行时符号。
     */
    private val symbols = linkedMapOf<String, RuntimeSymbol>()

    init {
        register(RuntimeSymbol("cangjie.throw", "void(ptr)"))
        register(RuntimeSymbol("cangjie.alloc", "ptr(i64)"))
        register(RuntimeSymbol("cangjie.gc.barrier", "void(ptr, ptr)"))
    }

    /**
     * 注册或覆盖运行时符号。
     */
    fun register(symbol: RuntimeSymbol) {
        symbols[symbol.name] = symbol
    }

    /**
     * 按名称解析运行时符号。
     */
    fun resolve(name: String): RuntimeSymbol? = symbols[name]

    /**
     * 返回当前符号表中的所有运行时符号。
     */
    fun allSymbols(): List<RuntimeSymbol> = symbols.values.toList()
}
