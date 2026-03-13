package org.cangnova.cangjie.cli.common.arguments

data class CommonCompilerArguments(
    var languageVersion: String? = null,
    var languageFeatures: List<String> = emptyList(),
    var enableChirToLlvmCodegen: Boolean = false,
    var codegenPartitionMode: String? = null,
    var verifyLlvmModule: Boolean = true,
    var emitLlvmBitcode: Boolean = true,
)
