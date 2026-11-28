package org.cangnova.cangjie.wasm.codegen.lowering

/** 仓颉 Wasm runtime import ABI 使用的稳定 32-bit type id。 */
internal object WasmRuntimeTypeIds {
    fun of(name: String): Int {
        var hash = 0x811c9dc5.toInt()
        name.encodeToByteArray().forEach { byte ->
            hash = hash xor (byte.toInt() and 0xff)
            hash *= 0x01000193
        }
        return hash
    }
}
