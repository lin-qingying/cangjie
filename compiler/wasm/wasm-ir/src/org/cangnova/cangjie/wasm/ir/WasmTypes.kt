package org.cangnova.cangjie.wasm.ir

/** 仓颉 wasm32 ABI 当前支持的 Wasm Core 值类型。 */
enum class WasmValueType(
    /** Binary-format value type byte. */
    val binaryCode: Byte,
    /** S-expression spelling used in diagnostics and name metadata. */
    val displayName: String,
) {
    I32(0x7f, "i32"),
    I64(0x7e, "i64"),
    F32(0x7d, "f32"),
    F64(0x7c, "f64"),
}

/** Wasm Core 函数签名；当前仓颉 ABI 每个函数最多返回一个值。 */
data class WasmFunctionType(
    val parameters: List<WasmValueType>,
    val results: List<WasmValueType>,
) {
    init {
        require(results.size <= 1) { "the current Cangjie Wasm ABI supports at most one function result" }
    }
}

/** 具名函数参数或局部变量。 */
data class WasmLocal(
    val name: String,
    val type: WasmValueType,
)

/** Core global section 支持的常量初始化值。 */
sealed interface WasmConstant {
    val type: WasmValueType

    data class I32(val value: Int) : WasmConstant {
        override val type: WasmValueType = WasmValueType.I32
    }

    data class I64(val value: Long) : WasmConstant {
        override val type: WasmValueType = WasmValueType.I64
    }

    data class F32(val value: Float) : WasmConstant {
        override val type: WasmValueType = WasmValueType.F32
    }

    data class F64(val value: Double) : WasmConstant {
        override val type: WasmValueType = WasmValueType.F64
    }
}

/** 以 64 KiB page 为单位的 WebAssembly memory limits。 */
data class WasmMemoryLimits(
    val minimumPages: UInt,
    val maximumPages: UInt? = null,
) {
    init {
        require(minimumPages <= 65_536u) { "memory32 cannot exceed 65,536 pages" }
        require(maximumPages == null || maximumPages in minimumPages..65_536u) {
            "maximum memory pages must be within the memory32 limit and not below the minimum"
        }
    }
}
