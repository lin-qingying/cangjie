package org.cangnova.cangjie.codegen.backend

data class LlvmBackendCapabilities(
    val supportsInProcessIR: Boolean,
    val supportsOptimization: Boolean,
    val supportsTargetCodegen: Boolean,
    val llvmVersion: String?,
)

interface LlvmBackend : LlvmBackendApi {
    val capabilities: LlvmBackendCapabilities
}
