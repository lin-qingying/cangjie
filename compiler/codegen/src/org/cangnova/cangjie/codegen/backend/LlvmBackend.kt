package org.cangnova.cangjie.codegen.backend

/**
 * LLVM 后端能力描述。
 */
data class LlvmBackendCapabilities(
    /**
     * 是否支持在当前进程内接收 LLVM IR 并生成产物。
     */
    val supportsInProcessIR: Boolean,
    /**
     * 是否支持 LLVM 模块优化。
     */
    val supportsOptimization: Boolean,
    /**
     * 是否支持目标平台 codegen 产物。
     */
    val supportsTargetCodegen: Boolean,
    /**
     * 当前后端报告的 LLVM 版本。
     */
    val llvmVersion: String?,
)

/**
 * 可初始化并具备能力描述的 LLVM 后端。
 */
interface LlvmBackend : LlvmBackendApi {
    /**
     * 后端当前可用能力。
     */
    val capabilities: LlvmBackendCapabilities
}
