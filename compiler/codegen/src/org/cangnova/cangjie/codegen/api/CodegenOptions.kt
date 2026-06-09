package org.cangnova.cangjie.codegen.api

/**
 * LLVM 模块级优化级别。
 */
enum class CodegenOptimizationLevel {
    NONE,
    LESS,
    DEFAULT,
    AGGRESSIVE,
}

/**
 * LLVM 目标重定位模式。
 */
enum class CodegenRelocationMode {
    DEFAULT,
    STATIC,
    PIC,
    DYNAMIC_NO_PIC,
    ROPI,
    RWPI,
    ROPI_RWPI,
}

/**
 * LLVM 目标 code model。
 */
enum class CodegenCodeModel {
    DEFAULT,
    JIT_DEFAULT,
    TINY,
    SMALL,
    KERNEL,
    MEDIUM,
    LARGE,
}

data class CodegenOptions(
    val enabled: Boolean = true,
    val partitionMode: ModulePartitionMode = ModulePartitionMode.SINGLE_MODULE,
    val llvmBackendKind: LlvmBackendKind = LlvmBackendKind.JNI,
    val failOnUnavailable: Boolean = false,
    val requiredLlvmMajorVersion: Int = 18,
    val verifyBeforeWrite: Boolean = true,
    val validateChirBeforeLowering: Boolean = true,
    val emitLoweringTrace: Boolean = false,
    val emitBitcode: Boolean = true,
    val emitObjectCode: Boolean = false,
    val emitComments: Boolean = true,
    val emitModuleHeader: Boolean = true,
    val emitRuntimeDeclarations: Boolean = true,
    val optimizeLlvmModule: Boolean = false,
    val optimizationLevel: CodegenOptimizationLevel = CodegenOptimizationLevel.DEFAULT,
    val targetTriple: String = "unknown-cangjie",
    val targetDataLayout: String = "",
    val targetCpu: String = "generic",
    val targetFeatures: String = "",
    val relocationMode: CodegenRelocationMode = CodegenRelocationMode.DEFAULT,
    val codeModel: CodegenCodeModel = CodegenCodeModel.DEFAULT,
)
