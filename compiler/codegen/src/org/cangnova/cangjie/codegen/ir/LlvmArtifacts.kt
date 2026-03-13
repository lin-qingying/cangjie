package org.cangnova.cangjie.codegen.ir

data class LlvmFunctionArtifact(
    val name: String,
    val ir: String,
)

data class LlvmModuleArtifact(
    val name: String,
    val ir: String,
    val functions: List<LlvmFunctionArtifact>,
    val bitcode: ByteArray? = null,
)

data class LlvmValueRef(
    val name: String,
    val llvmType: String,
)

data class LlvmBasicBlockRef(
    val name: String,
)

