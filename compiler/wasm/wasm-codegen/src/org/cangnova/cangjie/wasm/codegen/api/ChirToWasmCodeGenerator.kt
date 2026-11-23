package org.cangnova.cangjie.wasm.codegen.api

/**
 * CHIR 到 WebAssembly Core 的后端入口。
 *
 * 此接口位于前端与宿主工具链之外；实现直接消费 CHIR，不启动 LLVM 或修改前端产物。
 */
fun interface ChirToWasmCodeGenerator {
    /** 验证 CHIR、lowering 为 Wasm IR、验证 module 并生成标准 `.wasm` bytes。 */
    fun generate(input: WasmCodegenInput): WasmCodegenOutput
}
