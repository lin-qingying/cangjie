package org.cangnova.cangjie.llvm.api

interface LlvmTargetMachine : AutoCloseable {
    val ref: LlvmTargetMachineRef

    fun emitObjectFile(module: LlvmModule, outputPath: String)
}
