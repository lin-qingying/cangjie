package org.cangnova.cangjie.llvm.api

/**
 * LLVM Pass 管理器抽象。
 */
interface LlvmPassManager : AutoCloseable {
    /** 在给定模块上运行优化/分析 pass。 */
    fun run(module: LlvmModule)
}
