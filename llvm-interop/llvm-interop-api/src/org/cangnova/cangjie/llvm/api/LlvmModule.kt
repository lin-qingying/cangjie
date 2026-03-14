package org.cangnova.cangjie.llvm.api

class LlvmModule internal constructor(
    val ref: LlvmModuleRef,
    private val owner: LlvmContext,
    private val bindings: LlvmBindings,
) : AutoCloseable {
    private var closed = false

    fun setTargetTriple(targetTriple: String) {
        ensureOpen()
        bindings.moduleSetTargetTriple(ref, targetTriple)
    }

    fun setDataLayout(dataLayout: String) {
        ensureOpen()
        bindings.moduleSetDataLayout(ref, dataLayout)
    }

    fun addFunction(name: String, functionType: LlvmTypeRef): LlvmValueRef {
        ensureOpen()
        return bindings.moduleAddFunction(ref, name, functionType)
    }

    fun addGlobal(name: String, type: LlvmTypeRef): LlvmValueRef {
        ensureOpen()
        return bindings.moduleAddGlobal(ref, type, name)
    }

    fun irText(): String {
        ensureOpen()
        return bindings.moduleToString(ref)
    }

    fun verify() {
        ensureOpen()
        val verification = bindings.moduleVerify(ref)
        if (!verification.ok) {
            throw LlvmVerificationException(verification.message ?: "module verification failed")
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        bindings.moduleDispose(ref)
    }

    private fun ensureOpen() {
        owner.ensureOpen()
        check(!closed) { "LLVM module is already closed" }
    }
}
