package org.cangnova.cangjie.codegen.ir

/**
 * 单个 LLVM 函数的文本产物。
 */
data class LlvmFunctionArtifact(
    /**
     * LLVM 函数名。
     */
    val name: String,
    /**
     * 函数级 LLVM IR 文本。
     */
    val ir: String,
)

/**
 * 单个 LLVM module 的 codegen 产物。
 */
data class LlvmModuleArtifact(
    /**
     * LLVM module 名称。
     */
    val name: String,
    /**
     * 完整 LLVM IR 文本。
     */
    val ir: String,
    /**
     * module 中已降低的函数产物。
     */
    val functions: List<LlvmFunctionArtifact>,
    /**
     * 可选 LLVM bitcode 字节。
     */
    val bitcode: ByteArray? = null,
    /**
     * 可选目标 object code 字节。
     */
    val objectCode: ByteArray? = null,
)

/**
 * LLVM SSA value 引用。
 */
data class LlvmValueRef(
    /**
     * LLVM value 名称。
     */
    val name: String,
    /**
     * LLVM value 类型文本。
     */
    val llvmType: String,
)

/**
 * LLVM basic block 引用。
 */
data class LlvmBasicBlockRef(
    /**
     * LLVM basic block 名称。
     */
    val name: String,
)

