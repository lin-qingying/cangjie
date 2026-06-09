package org.cangnova.cangjie.llvm.api

/** LLVMContext 原生句柄。 */
@JvmInline
value class LlvmContextRef(val address: Long) {
    companion object {
        val NULL = LlvmContextRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVMModule 原生句柄。 */
@JvmInline
value class LlvmModuleRef(val address: Long) {
    companion object {
        val NULL = LlvmModuleRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVMType 原生句柄。 */
@JvmInline
value class LlvmTypeRef(val address: Long) {
    companion object {
        val NULL = LlvmTypeRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVMValue 原生句柄。 */
@JvmInline
value class LlvmValueRef(val address: Long) {
    companion object {
        val NULL = LlvmValueRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVMBasicBlock 原生句柄。 */
@JvmInline
value class LlvmBasicBlockRef(val address: Long) {
    companion object {
        val NULL = LlvmBasicBlockRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVMBuilder 原生句柄。 */
@JvmInline
value class LlvmBuilderRef(val address: Long) {
    companion object {
        val NULL = LlvmBuilderRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVMTargetMachine 原生句柄。 */
@JvmInline
value class LlvmTargetMachineRef(val address: Long) {
    companion object {
        val NULL = LlvmTargetMachineRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

/** LLVM 模块 Pass 管线原生执行上下文句柄。 */
@JvmInline
value class LlvmPassManagerRef(val address: Long) {
    companion object {
        val NULL = LlvmPassManagerRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}
