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

/**
 * CHIR 到 LLVM codegen 的配置集合。
 *
 * 该模型同时控制 lowering 行为、LLVM 原生后端选择、目标机器参数与最终产物类型。
 */
data class CodegenOptions(
    /**
     * 是否启用 codegen 阶段。
     */
    val enabled: Boolean = true,
    /**
     * CHIR module 到 LLVM module 的切分策略。
     */
    val partitionMode: ModulePartitionMode = ModulePartitionMode.SINGLE_MODULE,
    /**
     * 使用的 LLVM 后端实现种类。
     */
    val llvmBackendKind: LlvmBackendKind = LlvmBackendKind.JNI,
    /**
     * LLVM 后端不可用时是否立即失败。
     */
    val failOnUnavailable: Boolean = false,
    /**
     * 需要的 LLVM 主版本号。
     */
    val requiredLlvmMajorVersion: Int = 18,
    /**
     * 写出产物前是否执行 verifier。
     */
    val verifyBeforeWrite: Boolean = true,
    /**
     * lowering 前是否校验 CHIR 控制流和类型契约。
     */
    val validateChirBeforeLowering: Boolean = true,
    /**
     * 是否输出 lowering pass 追踪信息。
     */
    val emitLoweringTrace: Boolean = false,
    /**
     * 是否请求 LLVM bitcode 产物。
     */
    val emitBitcode: Boolean = true,
    /**
     * 是否请求目标 object code 产物。
     */
    val emitObjectCode: Boolean = false,
    /**
     * 是否在 LLVM IR 中保留注释行。
     */
    val emitComments: Boolean = true,
    /**
     * 是否在 LLVM IR 中生成模块头部信息。
     */
    val emitModuleHeader: Boolean = true,
    /**
     * 是否生成运行时符号声明。
     */
    val emitRuntimeDeclarations: Boolean = true,
    /**
     * 是否让 LLVM 对模块执行优化。
     */
    val optimizeLlvmModule: Boolean = false,
    /**
     * LLVM 模块优化级别。
     */
    val optimizationLevel: CodegenOptimizationLevel = CodegenOptimizationLevel.DEFAULT,
    /**
     * LLVM target triple。
     */
    val targetTriple: String = "unknown-cangjie",
    /**
     * LLVM target data layout 字符串。
     */
    val targetDataLayout: String = "",
    /**
     * LLVM 目标 CPU 名称。
     */
    val targetCpu: String = "generic",
    /**
     * LLVM 目标 feature 字符串。
     */
    val targetFeatures: String = "",
    /**
     * LLVM 重定位模式。
     */
    val relocationMode: CodegenRelocationMode = CodegenRelocationMode.DEFAULT,
    /**
     * LLVM code model。
     */
    val codeModel: CodegenCodeModel = CodegenCodeModel.DEFAULT,
)
