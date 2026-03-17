package org.cangnova.cangjie.llvm.api

/**
 * LLVM 目标机器抽象。
 */
interface LlvmTargetMachine : AutoCloseable {
    /** 原生目标机器句柄。 */
    val ref: LlvmTargetMachineRef

    /** 将模块生成目标文件。 */
    fun emitObjectFile(module: LlvmModule, outputPath: String)
}
