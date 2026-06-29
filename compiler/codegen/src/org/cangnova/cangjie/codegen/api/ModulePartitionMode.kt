package org.cangnova.cangjie.codegen.api

/**
 * CHIR package 降低为 LLVM module 时使用的模块切分策略。
 */
enum class ModulePartitionMode {
    SINGLE_MODULE,
    PER_CHIR_MODULE,
}
