package org.cangnova.cangjie.codegen.backend

interface LlvmBackendApi {
    val id: String

    fun initialize()

    fun emitBitcode(moduleName: String, llvmIr: String): ByteArray
}

class InMemoryLlvmBackendApi : LlvmBackendApi {
    override val id: String = "in-memory"

    override fun initialize() = Unit

    override fun emitBitcode(moduleName: String, llvmIr: String): ByteArray = llvmIr.toByteArray()
}

