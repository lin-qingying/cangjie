package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenCodeModel
import org.cangnova.cangjie.codegen.api.CodegenOptimizationLevel
import org.cangnova.cangjie.codegen.api.CodegenRelocationMode

/**
 * 单个 LLVM 模块交给原生后端时使用的发射参数。
 */
data class LlvmBackendEmissionOptions(
    val targetTriple: String? = null,
    val targetDataLayout: String? = null,
    val targetCpu: String = "generic",
    val targetFeatures: String = "",
    val optimizeModule: Boolean = false,
    val optimizationLevel: CodegenOptimizationLevel = CodegenOptimizationLevel.DEFAULT,
    val relocationMode: CodegenRelocationMode = CodegenRelocationMode.DEFAULT,
    val codeModel: CodegenCodeModel = CodegenCodeModel.DEFAULT,
)

interface LlvmBackendApi {
    val id: String

    fun initialize()

    fun emitBitcode(
        moduleName: String,
        llvmIr: String,
        options: LlvmBackendEmissionOptions = LlvmBackendEmissionOptions(),
    ): ByteArray

    fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray

    fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String)
}
