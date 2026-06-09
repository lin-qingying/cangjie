package org.cangnova.cangjie.llvm.api

/**
 * LLVM 代码生成优化级别。
 */
enum class LlvmCodeGenOptimizationLevel {
    NONE,
    LESS,
    DEFAULT,
    AGGRESSIVE,
}

/**
 * LLVM 重定位模式。
 */
enum class LlvmRelocationMode {
    DEFAULT,
    STATIC,
    PIC,
    DYNAMIC_NO_PIC,
    ROPI,
    RWPI,
    ROPI_RWPI,
}

/**
 * LLVM code model。
 */
enum class LlvmCodeModel {
    DEFAULT,
    JIT_DEFAULT,
    TINY,
    SMALL,
    KERNEL,
    MEDIUM,
    LARGE,
}

/**
 * LLVM 目标机器创建参数。
 */
data class LlvmTargetMachineOptions(
    val targetTriple: String,
    val cpu: String = "generic",
    val features: String = "",
    val optimizationLevel: LlvmCodeGenOptimizationLevel = LlvmCodeGenOptimizationLevel.DEFAULT,
    val relocationMode: LlvmRelocationMode = LlvmRelocationMode.DEFAULT,
    val codeModel: LlvmCodeModel = LlvmCodeModel.DEFAULT,
)

/**
 * LLVM 目标机器抽象。
 */
class LlvmTargetMachine internal constructor(
    val ref: LlvmTargetMachineRef,
    private val bindings: LlvmBindings,
) : AutoCloseable {
    /** 将模块生成目标文件。 */
    fun emitObjectFile(module: LlvmModule, outputPath: String) {
        bindings.targetMachineEmitObjectFile(ref, module.ref, outputPath)
    }

    /** 将模块生成目标文件字节。 */
    fun emitObjectBytes(module: LlvmModule): ByteArray =
        bindings.targetMachineEmitObjectBytes(ref, module.ref)

    override fun close() {
        bindings.targetDisposeMachine(ref)
    }
}

/**
 * LLVM 目标机器工厂。
 */
object LlvmTargetMachines {
    /** 初始化 LLVM 后端目标注册表。 */
    fun initializeAll() {
        LlvmBindingRegistry.bindings.targetInitializeAll()
    }

    /** 返回 LLVM 为当前机器探测到的默认目标三元组。 */
    fun defaultTriple(): String = LlvmBindingRegistry.bindings.targetDefaultTriple()

    /** 基于目标三元组创建目标机器。 */
    fun create(options: LlvmTargetMachineOptions): LlvmTargetMachine {
        val bindings = LlvmBindingRegistry.bindings
        val ref = bindings.targetCreateMachine(options)
        return LlvmTargetMachine(ref, bindings)
    }
}
