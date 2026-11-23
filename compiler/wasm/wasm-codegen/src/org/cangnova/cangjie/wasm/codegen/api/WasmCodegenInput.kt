package org.cangnova.cangjie.wasm.codegen.api

import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.wasm.common.WasmTarget

/** Wasm codegen 的不可变输入；CHIR package 是后端唯一语言模型输入。 */
data class WasmCodegenInput(
    /** 已完成语义处理的 CHIR package。 */
    val chirPackage: ChirPackage,
    /** Wasm 宿主目标与产物选项。 */
    val options: WasmCodegenOptions,
)

/** WebAssembly Core module 的 codegen 选项。 */
data class WasmCodegenOptions(
    /** 选择 WASI Preview 1 或 wasm32 unknown-unknown 宿主约定。 */
    val target: WasmTarget,
    /** 显式导出的 CHIR 函数名；null 表示采用目标 profile 默认导出策略。 */
    val exportedFunctionNames: Set<String>? = null,
    /** WASI `_start` wrapper 要调用的零参数程序入口；设为 null 时按库模块输出。 */
    val entryFunctionName: String? = "main",
    /** 初始 linear memory 页数；每页固定为 64 KiB。 */
    val initialMemoryPages: UInt = 1u,
    /** 最大 linear memory 页数；null 表示由 Core memory32 上限约束。 */
    val maximumMemoryPages: UInt? = null,
    /** 是否生成 WebAssembly name custom section。 */
    val emitNameSection: Boolean = true,
) {
    init {
        require(initialMemoryPages > 0u) { "initial WebAssembly memory must contain at least one page" }
        require(maximumMemoryPages == null || maximumMemoryPages >= initialMemoryPages) {
            "maximum WebAssembly memory pages must be greater than or equal to the initial page count"
        }
        require(maximumMemoryPages == null || maximumMemoryPages <= 65_536u) {
            "memory32 cannot exceed 65,536 WebAssembly pages"
        }
    }
}
