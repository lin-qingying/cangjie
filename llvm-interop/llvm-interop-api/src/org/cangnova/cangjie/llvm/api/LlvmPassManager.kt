package org.cangnova.cangjie.llvm.api

interface LlvmPassManager : AutoCloseable {
    fun run(module: LlvmModule)
}
