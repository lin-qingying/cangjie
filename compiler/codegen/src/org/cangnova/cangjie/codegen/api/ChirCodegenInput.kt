package org.cangnova.cangjie.codegen.api

import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * CHIR 到 LLVM codegen 的输入模型。
 *
 * 输入以完整 CHIR package 为粒度，后端根据 [options] 决定模块切分、LLVM 后端、目标平台和产物类型。
 */
data class ChirCodegenInput(
    /**
     * 待降低为 LLVM IR 的 CHIR package。
     */
    val chirPackage: ChirPackage,
    /**
     * 本次 codegen 使用的生成选项。
     */
    val options: CodegenOptions = CodegenOptions(),
)
