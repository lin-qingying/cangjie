package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenCodeModel
import org.cangnova.cangjie.codegen.api.CodegenOptimizationLevel
import org.cangnova.cangjie.codegen.api.CodegenRelocationMode

/**
 * 单个 LLVM 模块交给原生后端时使用的发射参数。
 */
data class LlvmBackendEmissionOptions(
    /**
     * 目标平台 triple；为空时由后端使用默认目标。
     */
    val targetTriple: String? = null,
    /**
     * 目标 data layout；为空时由后端使用默认布局。
     */
    val targetDataLayout: String? = null,
    /**
     * 目标 CPU。
     */
    val targetCpu: String = "generic",
    /**
     * 目标 CPU feature 字符串。
     */
    val targetFeatures: String = "",
    /**
     * 是否在发射前优化 LLVM module。
     */
    val optimizeModule: Boolean = false,
    /**
     * LLVM 优化级别。
     */
    val optimizationLevel: CodegenOptimizationLevel = CodegenOptimizationLevel.DEFAULT,
    /**
     * LLVM 重定位模式。
     */
    val relocationMode: CodegenRelocationMode = CodegenRelocationMode.DEFAULT,
    /**
     * LLVM code model。
     */
    val codeModel: CodegenCodeModel = CodegenCodeModel.DEFAULT,
)

/**
 * LLVM 后端的最小发射 API。
 */
interface LlvmBackendApi {
    /**
     * 后端实现标识。
     */
    val id: String

    /**
     * 初始化后端资源。
     */
    fun initialize()

    /**
     * 将 LLVM IR 发射为 bitcode 字节。
     */
    fun emitBitcode(
        moduleName: String,
        llvmIr: String,
        options: LlvmBackendEmissionOptions = LlvmBackendEmissionOptions(),
    ): ByteArray

    /**
     * 将 LLVM IR 发射为目标 object code 字节。
     */
    fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray

    /**
     * 将 LLVM IR 发射为目标 object file。
     */
    fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String)
}
