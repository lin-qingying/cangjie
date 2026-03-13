package org.cangnova.cangjie.codegen.runtime

data class RuntimeSymbol(
    val name: String,
    val llvmSignature: String,
)

class RuntimeSymbolTable {
    private val symbols = linkedMapOf<String, RuntimeSymbol>()

    init {
        register(RuntimeSymbol("cangjie.throw", "void(ptr)"))
        register(RuntimeSymbol("cangjie.alloc", "ptr(i64)"))
        register(RuntimeSymbol("cangjie.gc.barrier", "void(ptr, ptr)"))
    }

    fun register(symbol: RuntimeSymbol) {
        symbols[symbol.name] = symbol
    }

    fun resolve(name: String): RuntimeSymbol? = symbols[name]

    fun allSymbols(): List<RuntimeSymbol> = symbols.values.toList()
}

