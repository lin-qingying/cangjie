package org.cangnova.cangjie.cli

import org.cangnova.cangjie.cfir.session.CfirCjmpOptimizationLevel
import org.cangnova.cangjie.codegen.api.CodegenOptimizationLevel

/**
 * 把前端已经接受的 CJMP `-O` 级别交给 LLVM codegen，并保持 O0 不运行 LLVM 优化管线。
 */
internal data class CjmpCodegenOptimization(
    val optimizeLlvmModule: Boolean,
    val level: CodegenOptimizationLevel,
)

/** 将命令行级别从 CLI 字符串边界转换为有穷的 codegen 选项。 */
internal fun cjmpCodegenOptimization(value: String?): CjmpCodegenOptimization {
    val level = value?.let(CfirCjmpOptimizationLevel::fromCommandLineValue)
        ?: if (value == null) CfirCjmpOptimizationLevel.O0 else error("Frontend accepted invalid CJMP optimization level: $value")
    return when (level) {
        CfirCjmpOptimizationLevel.O0 -> CjmpCodegenOptimization(false, CodegenOptimizationLevel.NONE)
        CfirCjmpOptimizationLevel.O1 -> CjmpCodegenOptimization(true, CodegenOptimizationLevel.LESS)
        CfirCjmpOptimizationLevel.O2 -> CjmpCodegenOptimization(true, CodegenOptimizationLevel.DEFAULT)
        CfirCjmpOptimizationLevel.O3 -> CjmpCodegenOptimization(true, CodegenOptimizationLevel.AGGRESSIVE)
        CfirCjmpOptimizationLevel.O_S -> CjmpCodegenOptimization(true, CodegenOptimizationLevel.SIZE)
        CfirCjmpOptimizationLevel.O_Z -> CjmpCodegenOptimization(true, CodegenOptimizationLevel.SIZE_AGGRESSIVE)
    }
}
