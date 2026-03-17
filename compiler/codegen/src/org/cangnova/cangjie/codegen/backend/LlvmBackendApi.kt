package org.cangnova.cangjie.codegen.backend

interface LlvmBackendApi {
    val id: String

    fun initialize()

    fun emitBitcode(moduleName: String, llvmIr: String): ByteArray
}
