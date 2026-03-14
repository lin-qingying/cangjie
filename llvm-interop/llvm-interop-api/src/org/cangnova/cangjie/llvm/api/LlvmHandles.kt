package org.cangnova.cangjie.llvm.api

@JvmInline
value class LlvmContextRef(val address: Long) {
    companion object {
        val NULL = LlvmContextRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmModuleRef(val address: Long) {
    companion object {
        val NULL = LlvmModuleRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmTypeRef(val address: Long) {
    companion object {
        val NULL = LlvmTypeRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmValueRef(val address: Long) {
    companion object {
        val NULL = LlvmValueRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmBasicBlockRef(val address: Long) {
    companion object {
        val NULL = LlvmBasicBlockRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmBuilderRef(val address: Long) {
    companion object {
        val NULL = LlvmBuilderRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmTargetMachineRef(val address: Long) {
    companion object {
        val NULL = LlvmTargetMachineRef(0L)
    }

    val isNull: Boolean get() = address == 0L
}
