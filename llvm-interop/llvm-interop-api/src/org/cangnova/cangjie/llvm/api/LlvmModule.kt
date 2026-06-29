package org.cangnova.cangjie.llvm.api

/**
 * LLVM 模块封装。
 */
class LlvmModule internal constructor(
    /**
     * LLVMModule 原生句柄。
     */
    val ref: LlvmModuleRef,
    /**
     * 创建并拥有该模块生命周期的 LLVM 上下文。
     */
    private val owner: LlvmContext,
    /**
     * 实际执行模块操作的 LLVM 绑定实现。
     */
    private val bindings: LlvmBindings,
) : AutoCloseable {
    /**
     * 当前模块是否已经关闭。
     */
    private var closed = false

    /**
     * 设置模块目标三元组。
     */
    fun setTargetTriple(targetTriple: String) {
        ensureOpen()
        bindings.moduleSetTargetTriple(ref, targetTriple)
    }

    /**
     * 设置模块数据布局字符串。
     */
    fun setDataLayout(dataLayout: String) {
        ensureOpen()
        bindings.moduleSetDataLayout(ref, dataLayout)
    }

    /**
     * 向模块添加函数声明。
     */
    fun addFunction(name: String, functionType: LlvmTypeRef): LlvmValueRef {
        ensureOpen()
        return bindings.moduleAddFunction(ref, name, functionType)
    }

    /**
     * 向模块添加全局变量。
     */
    fun addGlobal(name: String, type: LlvmTypeRef): LlvmValueRef {
        ensureOpen()
        return bindings.moduleAddGlobal(ref, type, name)
    }

    /**
     * 向函数追加基本块。
     */
    fun appendBasicBlock(function: LlvmValueRef, name: String): LlvmBasicBlockRef {
        ensureOpen()
        return bindings.functionAppendBasicBlock(function, name)
    }

    /**
     * 获取 LLVM value 的名称。
     */
    fun valueName(value: LlvmValueRef): String {
        ensureOpen()
        return bindings.valueGetName(value)
    }

    /**
     * 获取 LLVM value 的类型。
     */
    fun valueType(value: LlvmValueRef): LlvmTypeRef {
        ensureOpen()
        return bindings.valueGetType(value)
    }

    /**
     * 将模块打印为 LLVM IR 文本。
     */
    fun irText(): String {
        ensureOpen()
        return bindings.moduleToString(ref)
    }

    /**
     * 校验整个模块，不通过时抛出 [LlvmVerificationException]。
     */
    fun verify() {
        ensureOpen()
        val verification = bindings.moduleVerify(ref)
        if (!verification.ok) {
            throw LlvmVerificationException(verification.message ?: "module verification failed")
        }
    }

    /**
     * 校验单个函数，不通过时抛出 [LlvmVerificationException]。
     */
    fun verifyFunction(function: LlvmValueRef) {
        ensureOpen()
        val verification = bindings.functionVerify(function)
        if (!verification.ok) {
            throw LlvmVerificationException(verification.message ?: "function verification failed")
        }
    }

    /**
     * 将模块序列化为 bitcode 字节数组。
     */
    fun bitcodeBytes(): ByteArray {
        ensureOpen()
        return bindings.moduleWriteBitcodeToMemoryBuffer(ref)
    }

    /**
     * 将模块 bitcode 写入指定文件。
     */
    fun writeBitcodeToFile(outputPath: String): Int {
        ensureOpen()
        return bindings.moduleWriteBitcodeToFile(ref, outputPath)
    }

    /**
     * 释放模块原生资源。
     */
    override fun close() {
        if (closed) return
        closed = true
        bindings.moduleDispose(ref)
    }

    /**
     * 确认 owner 上下文和当前模块均未关闭。
     */
    private fun ensureOpen() {
        owner.ensureOpen()
        check(!closed) { "LLVM module is already closed" }
    }
}
