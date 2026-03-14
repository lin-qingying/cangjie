package org.cangnova.cangjie.codegen.backend

interface LlvmBackendApi {
    val id: String

    fun initialize()

    fun emitBitcode(moduleName: String, llvmIr: String): ByteArray
}

class InMemoryLlvmBackendApi : LlvmBackend {
    override val id: String = "in-memory"
    override val capabilities: LlvmBackendCapabilities = LlvmBackendCapabilities(
        supportsInProcessIR = false,
        supportsOptimization = false,
        supportsTargetCodegen = false,
        llvmVersion = null,
    )

    override fun initialize() = Unit

    override fun emitBitcode(moduleName: String, llvmIr: String): ByteArray = llvmIr.toByteArray()
}
