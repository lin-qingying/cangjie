package org.cangnova.cangjie.wasm.ir

/** 从 Wasm 宿主或链接 runtime module 导入的函数。 */
data class WasmImport(
    val moduleName: String,
    /** Field name requested from the host. */
    val importName: String,
    /** Unique internal function symbol used by calls and indices. */
    val functionName: String,
    val type: WasmFunctionType,
)

/** 从宿主导入的数值 global。 */
data class WasmGlobalImport(
    val moduleName: String,
    val importName: String,
    val globalName: String,
    val type: WasmValueType,
    val mutable: Boolean,
)

/** 一个由当前 module 定义的函数体。 */
data class WasmFunction(
    val name: String,
    val type: WasmFunctionType,
    val parameters: List<WasmLocal>,
    val locals: List<WasmLocal>,
    val body: List<WasmInstruction>,
)

/** 由 Core 常量表达式初始化的数值 global。 */
data class WasmGlobal(
    val name: String,
    val type: WasmValueType,
    val mutable: Boolean,
    val initializer: WasmConstant,
)

/** 单个 wasm32 linear memory 定义。 */
data class WasmMemory(
    val name: String,
    val limits: WasmMemoryLimits,
)

/** 以 function reference 数量计的 table limits。 */
data class WasmTableLimits(
    val minimumElements: UInt,
    val maximumElements: UInt? = null,
) {
    init {
        require(maximumElements == null || maximumElements >= minimumElements) {
            "maximum table size must be greater than or equal to the minimum"
        }
    }
}

/** `funcref` 函数表定义。 */
data class WasmTable(
    val name: String,
    val limits: WasmTableLimits,
)

/** 初始化函数表 slot 的 active element segment。 */
data class WasmElementSegment(
    val tableName: String,
    val offset: UInt,
    val functionNames: List<String>,
)

/** Active data segment placed at a fixed linear-memory address. */
data class WasmDataSegment(
    val offset: UInt,
    val bytes: ByteArray,
)

/** 导出给宿主的函数、memory 或 global。 */
sealed interface WasmExport {
    val externalName: String

    data class Function(override val externalName: String, val functionName: String) : WasmExport
    data class Memory(override val externalName: String, val memoryName: String) : WasmExport
    data class Global(override val externalName: String, val globalName: String) : WasmExport
}

/** 分配 binary index 前的完整符号化 Wasm Core module。 */
data class WasmModule(
    val name: String,
    val imports: List<WasmImport> = emptyList(),
    val globalImports: List<WasmGlobalImport> = emptyList(),
    val functions: List<WasmFunction> = emptyList(),
    val globals: List<WasmGlobal> = emptyList(),
    val table: WasmTable? = null,
    val elementSegments: List<WasmElementSegment> = emptyList(),
    val memory: WasmMemory? = null,
    val exports: List<WasmExport> = emptyList(),
    val startFunctionName: String? = null,
    val dataSegments: List<WasmDataSegment> = emptyList(),
)
