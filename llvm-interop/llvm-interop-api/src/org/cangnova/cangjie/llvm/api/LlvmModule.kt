package org.cangnova.cangjie.llvm.api

/**
 * LLVM 模块封装。
 */
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

    fun appendBasicBlock(function: LlvmValueRef, name: String): LlvmBasicBlockRef {
        ensureOpen()
        return bindings.functionAppendBasicBlock(function, name)
    }

    fun valueName(value: LlvmValueRef): String {
        ensureOpen()
        return bindings.valueGetName(value)
    }

    fun valueType(value: LlvmValueRef): LlvmTypeRef {
        ensureOpen()
        return bindings.valueGetType(value)
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

    fun verifyFunction(function: LlvmValueRef) {
        ensureOpen()
        val verification = bindings.functionVerify(function)
        if (!verification.ok) {
            throw LlvmVerificationException(verification.message ?: "function verification failed")
        }
    }

    fun bitcodeBytes(): ByteArray {
        ensureOpen()
        return bindings.moduleWriteBitcodeToMemoryBuffer(ref)
    }

    fun writeBitcodeToFile(outputPath: String): Int {
        ensureOpen()
        return bindings.moduleWriteBitcodeToFile(ref, outputPath)
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
